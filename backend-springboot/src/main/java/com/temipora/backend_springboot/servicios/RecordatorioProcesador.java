package com.temipora.backend_springboot.servicios;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.EmailRecordatorioPendiente;
import com.temipora.backend_springboot.modelo.EnvioEmail;
import com.temipora.backend_springboot.modelo.EstadoEmailRecordatorio;
import com.temipora.backend_springboot.modelo.EstadoEnvioEmail;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.MotivoRecordatorio;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.PropositoToken;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EmailRecordatorioPendienteRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

/**
 * Procesa los recordatorios pendientes de un usuario. Pagos y entregas se
 * mandan en correos separados: un usuario puede recibir dos mails distintos.
 */
@Service
public class RecordatorioProcesador {

    private static final Logger log =
        LoggerFactory.getLogger(RecordatorioProcesador.class);
    private static final int MAX_INTENTOS = 3;
    private static final int MAX_LOTE = 50;
    private static final int VIGENCIA_ENLACE_HORAS = 72;
    private static final DateTimeFormatter FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmailRecordatorioPendienteRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final SancionService sancionService;
    private final ConfiguracionPedidoService configuracionPedidoService;
    private final EmailPlantillaService emailPlantillaService;
    private final EnvioEmailService envioEmailService;
    private final AccesoEnlaceService accesoEnlaceService;

    public RecordatorioProcesador(
            EmailRecordatorioPendienteRepository repository,
            UsuarioRepository usuarioRepository,
            SancionService sancionService,
            ConfiguracionPedidoService configuracionPedidoService,
            EmailPlantillaService emailPlantillaService,
            EnvioEmailService envioEmailService,
            AccesoEnlaceService accesoEnlaceService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.sancionService = sancionService;
        this.configuracionPedidoService = configuracionPedidoService;
        this.emailPlantillaService = emailPlantillaService;
        this.envioEmailService = envioEmailService;
        this.accesoEnlaceService = accesoEnlaceService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean procesar(Long idUsuario) {
        Usuario usuario = usuarioRepository.findByIdUsuario(idUsuario)
            .orElse(null);
        if (usuario == null || !usuario.isEstadoCuenta()
                || usuario.getCorreo() == null
                || usuario.getCorreo().isBlank()) {
            omitirPendientes(idUsuario);
            return false;
        }
        if (sancionService.obtenerVigente(idUsuario).isPresent()) {
            return false;
        }

        List<EmailRecordatorioPendiente> pendientes = repository
            .findByUsuarioIdUsuarioAndEstadoOrderByCreadoEnAsc(
                idUsuario, EstadoEmailRecordatorio.PENDIENTE,
                PageRequest.of(0, MAX_LOTE));
        if (pendientes.isEmpty()) {
            return false;
        }

        List<EmailPlantillaService.RecordatorioEmail> pagos = new ArrayList<>();
        List<EmailPlantillaService.RecordatorioEmail> entregas =
            new ArrayList<>();
        List<EmailRecordatorioPendiente> filasPago = new ArrayList<>();
        List<EmailRecordatorioPendiente> filasEntrega = new ArrayList<>();
        ConfiguracionPedido config = configuracionPedidoService.obtener();
        int gracia = config.getDiasGraciaEntrega() != null
            ? config.getDiasGraciaEntrega()
            : ConfiguracionPedido.DIAS_GRACIA_ENTREGA_POR_DEFECTO;

        for (EmailRecordatorioPendiente fila : pendientes) {
            EmailPlantillaService.RecordatorioEmail item = item(fila, gracia);
            if (item == null) {
                fila.setEstado(EstadoEmailRecordatorio.OMITIDO);
                continue;
            }
            if (item.esPago()) {
                pagos.add(item);
                filasPago.add(fila);
            } else {
                entregas.add(item);
                filasEntrega.add(fila);
            }
        }
        repository.saveAll(pendientes);

        boolean enviado = false;
        if (!pagos.isEmpty()) {
            enviado |= enviarGrupo(
                usuario, pagos, filasPago, true, false);
        }
        if (!entregas.isEmpty()) {
            enviado |= enviarGrupo(
                usuario, entregas, filasEntrega, false, true);
        }
        return enviado;
    }

    private boolean enviarGrupo(
            Usuario usuario,
            List<EmailPlantillaService.RecordatorioEmail> items,
            List<EmailRecordatorioPendiente> filas,
            boolean hayPagos,
            boolean hayEntregas) {
        String lote = UUID.randomUUID().toString();
        for (EmailRecordatorioPendiente fila : filas) {
            fila.setIdLote(lote);
        }
        repository.saveAll(filas);

        String asunto = emailPlantillaService.asuntoRecordatorio(
            hayPagos, hayEntregas, hayUrgente(items));
        try {
            String token = accesoEnlaceService.generar(
                usuario, PropositoToken.RECORDATORIO, null,
                VIGENCIA_ENLACE_HORAS);
            String html = hayPagos
                ? emailPlantillaService.recordatorio(
                    nombre(usuario), items, List.of(), token)
                : emailPlantillaService.recordatorio(
                    nombre(usuario), List.of(), items, token);
            EnvioEmail envio = envioEmailService.enviar(
                usuario,
                "RECORDATORIO",
                null,
                null,
                asunto,
                html,
                "recordatorio:" + lote);
            if (envio != null
                    && envio.getEstado() == EstadoEnvioEmail.ENVIADO) {
                LocalDateTime ahora = LocalDateTime.now();
                for (EmailRecordatorioPendiente fila : filas) {
                    fila.setEstado(EstadoEmailRecordatorio.ENVIADO);
                    fila.setEnviadoEn(ahora);
                }
                repository.saveAll(filas);
                return true;
            }
            if (envio != null
                    && envio.getEstado() == EstadoEnvioEmail.PENDIENTE) {
                return false;
            }
            incrementarIntentos(filas);
            return false;
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el recordatorio del usuario {}: {}",
                usuario.getIdUsuario(), e.getMessage());
            incrementarIntentos(filas);
            return false;
        }
    }

    private boolean hayUrgente(
            List<EmailPlantillaService.RecordatorioEmail> items) {
        for (EmailPlantillaService.RecordatorioEmail item : items) {
            if (item.urgente()) {
                return true;
            }
        }
        return false;
    }

    private EmailPlantillaService.RecordatorioEmail item(
            EmailRecordatorioPendiente fila, int gracia) {
        Pedido pedido = fila.getPedido();
        if (pedido == null || fila.getMotivo() == null) {
            return null;
        }
        String servicio = nombreServicio(pedido);
        String imagen = pedido.getServicio() != null
            ? pedido.getServicio().getImagenPortada()
            : null;
        String moneda = pedido.getPresupuesto() != null
                && pedido.getPresupuesto().getMoneda() != null
            ? pedido.getPresupuesto().getMoneda().getCodigo()
            : null;

        if (fila.getMotivo().esPago()) {
            HitoPedido hito = fila.getHitoPedido();
            if (hito == null || hito.getHitoPresupuesto() == null
                    || hito.getEstadoPago() != EstadoPagoHito.PENDIENTE
                    || hito.getFechaLimitePago() == null
                    || !hito.getFechaLimitePago().isAfter(LocalDateTime.now())) {
                return null;
            }
            String artista = pedido.getArtista() != null
                ? texto(pedido.getArtista().getNombreUsuario())
                : "";
            return new EmailPlantillaService.RecordatorioEmail(
                "PAGO",
                pedido.getIdPedido(),
                servicio,
                imagen,
                artista,
                hito.getHitoPresupuesto().getNombre(),
                fecha(hito.getFechaLimitePago()),
                hito.getHitoPresupuesto().getMonto(),
                moneda,
                fila.getMotivo().esUrgente(),
                fila.getMotivo().esAlerta(),
                null);
        }

        String cliente = pedido.getCliente() != null
            ? texto(pedido.getCliente().getNombreUsuario())
            : "";
        if (pedido.getEstado() != EstadoPedido.EN_CURSO) {
            return null;
        }
        HitoPedido hito = proximoEntregable(pedido);
        if (hito == null) {
            return null;
        }
        String detalle = hito.getHitoPresupuesto() != null
            ? hito.getHitoPresupuesto().getNombre()
            : "Entrega pendiente";
        LocalDate prevista = PedidoFechas.entregaPrevista(pedido);
        return new EmailPlantillaService.RecordatorioEmail(
            "ENTREGA",
            pedido.getIdPedido(),
            servicio,
            imagen,
            cliente,
            detalle,
            prevista != null ? FECHA.format(prevista) : "",
            null,
            moneda,
            fila.getMotivo().esUrgente(),
            fila.getMotivo().esAlerta(),
            consecuenciaEntrega(fila.getMotivo(), gracia));
    }

    private String consecuenciaEntrega(MotivoRecordatorio motivo, int gracia) {
        if (motivo == MotivoRecordatorio.ENTREGA_1D) {
            return "Mañana vence el plazo. Si no entregás, en "
                + dias(gracia) + " se suspenden tus ventas.";
        }
        if (motivo == MotivoRecordatorio.ENTREGA_HOY) {
            return "Vence hoy. Si no entregás, en " + dias(gracia)
                + " se suspenden tus ventas. Si la demora continúa, tu cuenta "
                + "queda limitada solo a entregar los pedidos abiertos: no podés "
                + "usar el resto del sitio hasta que un administrador te reintegre.";
        }
        return null;
    }

    private String dias(int cantidad) {
        return cantidad == 1 ? "1 día" : cantidad + " días";
    }

    private HitoPedido proximoEntregable(Pedido pedido) {
        for (HitoPedido hito : pedido.getHitos()) {
            if (hito.getHitoPresupuesto() == null
                    || !hito.getHitoPresupuesto().isRequiereEntrega()) {
                continue;
            }
            EstadoEntregaHito estado = hito.getEstadoEntrega();
            if (estado == EstadoEntregaHito.ENTREGADO
                    || estado == EstadoEntregaHito.APROBADO
                    || estado == EstadoEntregaHito.APROBADO_AUTOMATICAMENTE) {
                continue;
            }
            if (hito.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA
                    || hito.getEstadoPago() == EstadoPagoHito.PAGADO) {
                return hito;
            }
        }
        return null;
    }

    private void incrementarIntentos(List<EmailRecordatorioPendiente> filas) {
        for (EmailRecordatorioPendiente fila : filas) {
            if (fila.getEstado() == EstadoEmailRecordatorio.ENVIADO
                    || fila.getEstado() == EstadoEmailRecordatorio.OMITIDO) {
                continue;
            }
            fila.setIntentos(fila.getIntentos() + 1);
            if (fila.getIntentos() >= MAX_INTENTOS) {
                fila.setEstado(EstadoEmailRecordatorio.FALLIDO);
            }
        }
        repository.saveAll(filas);
    }

    private void omitirPendientes(Long idUsuario) {
        List<EmailRecordatorioPendiente> pendientes = repository
            .findByUsuarioIdUsuarioAndEstadoOrderByCreadoEnAsc(
                idUsuario, EstadoEmailRecordatorio.PENDIENTE,
                PageRequest.of(0, MAX_LOTE));
        for (EmailRecordatorioPendiente fila : pendientes) {
            fila.setEstado(EstadoEmailRecordatorio.OMITIDO);
        }
        repository.saveAll(pendientes);
    }

    private String fecha(LocalDateTime valor) {
        return valor != null ? FECHA.format(valor) : "";
    }

    private String nombreServicio(Pedido pedido) {
        return pedido.getServicio() != null
                && pedido.getServicio().getNombre() != null
            ? pedido.getServicio().getNombre()
            : "tu pedido";
    }

    private String nombre(Usuario usuario) {
        if (usuario.getNombreDisplay() != null
                && !usuario.getNombreDisplay().isBlank()) {
            return usuario.getNombreDisplay();
        }
        return texto(usuario.getNombreUsuario());
    }

    private String texto(String valor) {
        return valor != null ? valor : "";
    }
}
