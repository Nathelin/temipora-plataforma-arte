package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.CierreSustitucionDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionSustitucion;
import com.temipora.backend_springboot.modelo.EmailCierrePendiente;
import com.temipora.backend_springboot.modelo.EnvioEmail;
import com.temipora.backend_springboot.modelo.EstadoEmailCierre;
import com.temipora.backend_springboot.modelo.EstadoEnvioEmail;
import com.temipora.backend_springboot.modelo.PropositoToken;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.SustitucionOpcionDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EmailCierrePendienteRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

/**
 * Procesa los cierres pendientes de un usuario: arma el resumen, calcula las
 * alternativas del cierre más reciente, genera el enlace de acceso y envía un
 * solo correo. Se ejecuta en su propia transacción para aislar fallos.
 */
@Service
public class CierreEmailProcesador {

    private static final Logger log =
        LoggerFactory.getLogger(CierreEmailProcesador.class);
    private static final int MAX_INTENTOS = 3;
    private static final int MAX_ALTERNATIVAS = 3;

    private final EmailCierrePendienteRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionSustitucionService configuracionService;
    private final SustitucionService sustitucionService;
    private final EmailPlantillaService emailPlantillaService;
    private final EnvioEmailService envioEmailService;
    private final AccesoEnlaceService accesoEnlaceService;
    private final SancionService sancionService;

    public CierreEmailProcesador(
            EmailCierrePendienteRepository repository,
            UsuarioRepository usuarioRepository,
            ConfiguracionSustitucionService configuracionService,
            SustitucionService sustitucionService,
            EmailPlantillaService emailPlantillaService,
            EnvioEmailService envioEmailService,
            AccesoEnlaceService accesoEnlaceService,
            SancionService sancionService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.configuracionService = configuracionService;
        this.sustitucionService = sustitucionService;
        this.emailPlantillaService = emailPlantillaService;
        this.envioEmailService = envioEmailService;
        this.accesoEnlaceService = accesoEnlaceService;
        this.sancionService = sancionService;
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

        // Los correos de cierre son automáticos: no se envían mientras la
        // cuenta esté atravesando una sanción vigente.
        if (sancionService.obtenerVigente(idUsuario).isPresent()) {
            return false;
        }

        ConfiguracionSustitucion config = configuracionService.obtener();
        List<EmailCierrePendiente> pendientes = repository
            .findByUsuarioIdUsuarioAndEstadoOrderByCreadoEnAsc(
                idUsuario,
                EstadoEmailCierre.PENDIENTE,
                PageRequest.of(0, Math.max(1, config.getEmailCierreMax())));
        if (pendientes.isEmpty()) {
            return false;
        }

        List<EmailPlantillaService.CierreEmail> cierres = new ArrayList<>();
        List<EmailPlantillaService.AlternativaEmail> alternativas =
            new ArrayList<>();
        List<EmailCierrePendiente> incluidos = new ArrayList<>();
        List<CierreSustitucionDTO> detalles = new ArrayList<>();

        for (EmailCierrePendiente fila : pendientes) {
            CierreSustitucionDTO detalle;
            try {
                detalle = detalleDe(fila, idUsuario);
            } catch (RuntimeException e) {
                fila.setEstado(EstadoEmailCierre.OMITIDO);
                fila.setIntentos(fila.getIntentos() + 1);
                repository.save(fila);
                continue;
            }
            incluidos.add(fila);
            detalles.add(detalle);
            cierres.add(new EmailPlantillaService.CierreEmail(
                detalle.getContexto(),
                detalle.getIdSolicitud(),
                detalle.getIdPedido(),
                detalle.getServicio() != null
                    ? detalle.getServicio().getNombre()
                    : "",
                detalle.getServicio() != null
                    ? detalle.getServicio().getNombreArtista()
                    : "",
                detalle.getServicio() != null
                    ? detalle.getServicio().getImagenPortada()
                    : null,
                detalle.getTitulo(),
                detalle.getDetalle(),
                detalle.getFecha()));
        }

        if (incluidos.isEmpty()) {
            return false;
        }

        // Nunca ofrecemos como alternativa un servicio que este mismo correo
        // informa como cerrado o rechazado.
        Set<Long> serviciosCerrados = new HashSet<>();
        for (CierreSustitucionDTO detalle : detalles) {
            if (detalle.getServicio() != null
                    && detalle.getServicio().getIdServicio() != null) {
                serviciosCerrados.add(detalle.getServicio().getIdServicio());
            }
        }
        for (CierreSustitucionDTO detalle : detalles) {
            if (detalle.getOpciones() == null || detalle.getOpciones().isEmpty()) {
                continue;
            }
            List<EmailPlantillaService.AlternativaEmail> candidatas =
                new ArrayList<>();
            for (SustitucionOpcionDTO opcion : detalle.getOpciones()) {
                if (candidatas.size() >= MAX_ALTERNATIVAS) {
                    break;
                }
                Long idServicio = idServicioDe(opcion);
                if (idServicio == null
                        || serviciosCerrados.contains(idServicio)) {
                    continue;
                }
                candidatas.add(
                    alternativa(opcion, detalle.getIdSustitucion()));
            }
            if (!candidatas.isEmpty()) {
                alternativas.addAll(candidatas);
                break;
            }
        }

        String lote = incluidos.stream()
            .map(EmailCierrePendiente::getIdLote)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseGet(() -> UUID.randomUUID().toString());
        LocalDateTime ahora = LocalDateTime.now();
        for (EmailCierrePendiente fila : incluidos) {
            fila.setIdLote(lote);
        }
        repository.saveAll(incluidos);

        try {
            String token = accesoEnlaceService.generar(
                usuario,
                PropositoToken.CIERRE,
                null,
                Math.max(1, config.getEmailCierreVigenciaHoras()));
            String asunto = emailPlantillaService.asuntoCierre(cierres);
            String html = emailPlantillaService.cierre(
                nombre(usuario), cierres, alternativas, token);
            EnvioEmail envio = envioEmailService.enviar(
                usuario,
                "CIERRE",
                null,
                null,
                asunto,
                html,
                "cierre:" + lote);
            if (envio != null && envio.getEstado() == EstadoEnvioEmail.ENVIADO) {
                for (EmailCierrePendiente fila : incluidos) {
                    fila.setEstado(EstadoEmailCierre.ENVIADO);
                    fila.setEnviadoEn(ahora);
                }
                repository.saveAll(incluidos);
                return true;
            }
            if (envio != null && envio.getEstado() == EstadoEnvioEmail.PENDIENTE) {
                // El envío de mails está deshabilitado: reintentar sin gastar intentos.
                return false;
            }
            incrementarIntentos(incluidos);
            return false;
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el mail de cierre del usuario {}: {}",
                idUsuario, e.getMessage());
            incrementarIntentos(incluidos);
            return false;
        }
    }

    private CierreSustitucionDTO detalleDe(
            EmailCierrePendiente fila, Long idUsuario) {
        if (fila.getPedido() != null) {
            return sustitucionService.detalleCierrePorPedido(
                fila.getPedido().getIdPedido(), idUsuario);
        }
        return sustitucionService.detalleCierrePorSolicitud(
            fila.getSolicitud().getIdSolicitud(), idUsuario);
    }

    private Long idServicioDe(SustitucionOpcionDTO opcion) {
        if (opcion.getIdServicio() != null) {
            return opcion.getIdServicio();
        }
        return opcion.getServicio() != null
            ? opcion.getServicio().getIdServicio()
            : null;
    }

    private EmailPlantillaService.AlternativaEmail alternativa(
            SustitucionOpcionDTO opcion, Long idSustitucion) {
        ServicioDTO servicio = opcion.getServicio();
        return new EmailPlantillaService.AlternativaEmail(
            servicio != null ? servicio.getIdServicio() : opcion.getIdServicio(),
            idSustitucion,
            servicio != null ? servicio.getNombre() : "",
            servicio != null ? servicio.getNombreArtista() : "",
            servicio != null ? servicio.getImagenPortada() : null,
            servicio != null && servicio.getPrecioBaseMostrado() != null
                ? servicio.getPrecioBaseMostrado().toPlainString()
                : "",
            servicio != null ? servicio.getMonedaMostrada() : "",
            opcion.getMotivo(),
            opcion.getCaracteristicas());
    }

    private void incrementarIntentos(List<EmailCierrePendiente> filas) {
        for (EmailCierrePendiente fila : filas) {
            fila.setIntentos(fila.getIntentos() + 1);
            if (fila.getIntentos() >= MAX_INTENTOS) {
                fila.setEstado(EstadoEmailCierre.FALLIDO);
            }
        }
        repository.saveAll(filas);
    }

    private void omitirPendientes(Long idUsuario) {
        List<EmailCierrePendiente> pendientes = repository
            .findByUsuarioIdUsuarioAndEstadoOrderByCreadoEnAsc(
                idUsuario,
                EstadoEmailCierre.PENDIENTE,
                PageRequest.of(0, 20));
        for (EmailCierrePendiente fila : pendientes) {
            fila.setEstado(EstadoEmailCierre.OMITIDO);
        }
        repository.saveAll(pendientes);
    }

    private String nombre(Usuario usuario) {
        if (usuario.getNombreDisplay() != null
                && !usuario.getNombreDisplay().isBlank()) {
            return usuario.getNombreDisplay();
        }
        return usuario.getNombreUsuario() != null
            ? usuario.getNombreUsuario()
            : "";
    }
}
