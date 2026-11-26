package com.temipora.backend_springboot.servicios;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoProrroga;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.ProrrogaPedido;
import com.temipora.backend_springboot.modelo.ProrrogaRequest;
import com.temipora.backend_springboot.modelo.RespuestaProrrogaRequest;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.ProrrogaPedidoRepository;

/**
 * Propuestas de prórroga del plazo de entrega.
 *
 * <p>El artista propone cuántos días necesita y el cliente acepta o rechaza.
 * Solo una propuesta puede estar pendiente por pedido. La promesa original
 * ({@code pedido.limiteEntrega}) no se toca: una prórroga aceptada suma días a
 * la entrega prevista, y una rechazada o vencida mantiene los plazos
 * originales.</p>
 */
@Service
public class ProrrogaService {

    private static final String TIPO_REFERENCIA = "PEDIDO";
    private static final String ENTIDAD = "ProrrogaPedido";
    private static final String NOTIF_SOLICITADA = "PRORROGA_SOLICITADA";
    private static final String NOTIF_ACEPTADA = "PRORROGA_ACEPTADA";
    private static final String NOTIF_RECHAZADA = "PRORROGA_RECHAZADA";
    private static final String NOTIF_VENCIDA = "PRORROGA_VENCIDA";
    private static final int LARGO_MOTIVO = 500;
    private static final DateTimeFormatter FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("estado", "Estado"),
        CampoAuditado.de("diasSolicitados", "Días solicitados"),
        CampoAuditado.de("motivo", "Motivo"),
        CampoAuditado.de("motivoRespuesta", "Motivo de la respuesta"),
        CampoAuditado.de("fechaRespuesta", "Fecha de respuesta"),
        CampoAuditado.de("entregaPrevistaAntes", "Entrega prevista antes"),
        CampoAuditado.de("entregaPrevistaDespues", "Entrega prevista después"));

    private final PedidoRepository pedidoRepository;
    private final ProrrogaPedidoRepository prorrogaRepository;
    private final ConfiguracionPedidoService configuracionPedidoService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoria;

    public ProrrogaService(
            PedidoRepository pedidoRepository,
            ProrrogaPedidoRepository prorrogaRepository,
            ConfiguracionPedidoService configuracionPedidoService,
            NotificacionService notificacionService,
            AuditoriaService auditoria) {
        this.pedidoRepository = pedidoRepository;
        this.prorrogaRepository = prorrogaRepository;
        this.configuracionPedidoService = configuracionPedidoService;
        this.notificacionService = notificacionService;
        this.auditoria = auditoria;
    }

    @Transactional
    public void solicitar(Long idPedido, Long idArtista,
            ProrrogaRequest request) {
        Pedido pedido = pedidoRepository.findByIdPedidoForUpdate(idPedido)
            .orElseThrow(() -> new NoSuchElementException(
                "Pedido no encontrado: " + idPedido));
        if (pedido.getArtista() == null
                || !pedido.getArtista().getIdUsuario().equals(idArtista)) {
            throw new AccesoDenegadoException(
                "Solo el artista puede pedir una prórroga de este pedido.");
        }
        if (pedido.getEstado() != EstadoPedido.EN_CURSO) {
            throw new ConflictoException(
                "Solo podés pedir una prórroga cuando el pedido está en curso.");
        }
        if (pedido.getLimiteEntrega() == null) {
            throw new ConflictoException(
                "El pedido todavía no tiene una fecha de entrega comprometida.");
        }
        if (pedido.getMoraDesde() != null) {
            throw new ConflictoException(
                "El pedido ya está en mora: una prórroga no revierte el atraso.");
        }
        if (prorrogaRepository.existsByPedidoIdPedido(idPedido)) {
            throw new ConflictoException(
                "Ya pediste la prórroga de este pedido: solo se puede pedir una vez.");
        }
        Integer dias = request != null ? request.getDias() : null;
        if (dias == null || dias < 1 || dias > 365) {
            throw new IllegalArgumentException(
                "La prórroga debe ser de entre 1 y 365 días.");
        }
        String motivo = limpiarTexto(
            request != null ? request.getMotivo() : null, LARGO_MOTIVO);
        if (motivo == null) {
            throw new IllegalArgumentException(
                "Contale al cliente por qué necesitás más plazo.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        ProrrogaPedido prorroga = new ProrrogaPedido();
        prorroga.setPedido(pedido);
        prorroga.setDiasSolicitados(dias);
        prorroga.setMotivo(motivo);
        prorroga.setEstado(EstadoProrroga.PENDIENTE);
        prorroga.setFechaSolicitud(ahora);
        prorroga.setFechaLimiteRespuesta(
            ahora.plusHours(horasRespuesta()));
        prorrogaRepository.save(prorroga);

        notificarSolicitada(pedido, prorroga);
        auditoria.registrar(
            AccionesAdmin.PRORROGA_SOLICITAR, ENTIDAD,
            prorroga.getIdProrroga(),
            "Solicitó correr la entrega del pedido #" + pedido.getIdPedido()
                + " en " + dias + " día(s)",
            null, prorroga, CAMPOS);
    }

    @Transactional
    public void aceptar(Long idPedido, Long idProrroga, Long idCliente) {
        Pedido pedido = pedidoRepository.findByIdPedidoForUpdate(idPedido)
            .orElseThrow(() -> new NoSuchElementException(
                "Pedido no encontrado: " + idPedido));
        if (pedido.getCliente() == null
                || !pedido.getCliente().getIdUsuario().equals(idCliente)) {
            throw new AccesoDenegadoException(
                "Solo el cliente puede responder esta prórroga.");
        }
        ProrrogaPedido prorroga = prorrogaDelPedido(idPedido, idProrroga);
        validarPendienteYVigente(prorroga);

        LocalDate antes = PedidoFechas.entregaPrevista(pedido);
        if (antes == null) {
            throw new ConflictoException(
                "El pedido todavía no tiene una fecha de entrega comprometida.");
        }
        Snapshot snapshot = Snapshot.de(prorroga, CAMPOS);
        LocalDateTime ahora = LocalDateTime.now();
        prorroga.setEstado(EstadoProrroga.ACEPTADA);
        prorroga.setFechaRespuesta(ahora);
        prorroga.setEntregaPrevistaAntes(antes);
        prorroga.setEntregaPrevistaDespues(
            antes.plusDays(prorroga.getDiasSolicitados()));
        prorrogaRepository.save(prorroga);

        notificarAceptada(pedido, prorroga);
        auditoria.registrar(
            AccionesAdmin.PRORROGA_ACEPTAR, ENTIDAD,
            prorroga.getIdProrroga(),
            "Aceptó correr la entrega del pedido #" + pedido.getIdPedido()
                + " en " + prorroga.getDiasSolicitados() + " día(s)"
                + " (de " + FECHA.format(antes) + " a "
                + FECHA.format(prorroga.getEntregaPrevistaDespues()) + ")",
            snapshot, prorroga, CAMPOS);
    }

    @Transactional
    public void rechazar(Long idPedido, Long idProrroga, Long idCliente,
            RespuestaProrrogaRequest request) {
        Pedido pedido = pedidoRepository.findByIdPedidoForUpdate(idPedido)
            .orElseThrow(() -> new NoSuchElementException(
                "Pedido no encontrado: " + idPedido));
        if (pedido.getCliente() == null
                || !pedido.getCliente().getIdUsuario().equals(idCliente)) {
            throw new AccesoDenegadoException(
                "Solo el cliente puede responder esta prórroga.");
        }
        ProrrogaPedido prorroga = prorrogaDelPedido(idPedido, idProrroga);
        validarPendienteYVigente(prorroga);

        Snapshot snapshot = Snapshot.de(prorroga, CAMPOS);
        prorroga.setEstado(EstadoProrroga.RECHAZADA);
        prorroga.setFechaRespuesta(LocalDateTime.now());
        prorroga.setMotivoRespuesta(limpiarTexto(
            request != null ? request.getMotivo() : null, LARGO_MOTIVO));
        prorrogaRepository.save(prorroga);

        notificarRechazada(pedido, prorroga);
        auditoria.registrar(
            AccionesAdmin.PRORROGA_RECHAZAR, ENTIDAD,
            prorroga.getIdProrroga(),
            "Rechazó la prórroga del pedido #" + pedido.getIdPedido()
                + " (" + prorroga.getDiasSolicitados() + " día(s))"
                + (prorroga.getMotivoRespuesta() != null
                    ? ". Motivo: " + prorroga.getMotivoRespuesta()
                    : ""),
            snapshot, prorroga, CAMPOS);
    }

    /**
     * Vence las propuestas que el cliente no respondió dentro de la ventana.
     * Las mantiene en el historial como evidencia de que el plazo original
     * siguió vigente.
     */
    @Transactional
    public int expirarVencidas() {
        LocalDateTime ahora = LocalDateTime.now();
        List<ProrrogaPedido> candidatas = prorrogaRepository
            .findByEstadoAndFechaLimiteRespuestaBefore(
                EstadoProrroga.PENDIENTE, ahora);
        int total = 0;
        for (ProrrogaPedido candidata : candidatas) {
            ProrrogaPedido prorroga = prorrogaRepository
                .findByIdProrrogaForUpdate(candidata.getIdProrroga())
                .orElse(null);
            if (prorroga == null
                    || prorroga.getEstado() != EstadoProrroga.PENDIENTE
                    || prorroga.getFechaLimiteRespuesta() == null
                    || !prorroga.getFechaLimiteRespuesta().isBefore(ahora)) {
                continue;
            }
            Snapshot snapshot = Snapshot.de(prorroga, CAMPOS);
            prorroga.setEstado(EstadoProrroga.VENCIDA);
            prorrogaRepository.save(prorroga);

            Pedido pedido = prorroga.getPedido();
            notificarVencida(pedido, prorroga);
            auditoria.registrarSistema(
                AccionesAdmin.PRORROGA_VENCER, ENTIDAD,
                prorroga.getIdProrroga(),
                "Venció sin respuesta la prórroga del pedido #"
                    + (pedido != null ? pedido.getIdPedido() : "?")
                    + "; se mantienen los plazos originales");
            total++;
        }
        return total;
    }

    private ProrrogaPedido prorrogaDelPedido(
            Long idPedido, Long idProrroga) {
        ProrrogaPedido prorroga = prorrogaRepository
            .findByIdProrrogaForUpdate(idProrroga)
            .orElseThrow(() -> new NoSuchElementException(
                "Prórroga no encontrada: " + idProrroga));
        if (prorroga.getPedido() == null
                || !prorroga.getPedido().getIdPedido().equals(idPedido)) {
            throw new NoSuchElementException(
                "La prórroga no pertenece a este pedido.");
        }
        return prorroga;
    }

    private void validarPendienteYVigente(ProrrogaPedido prorroga) {
        if (!estaPendienteYVigente(prorroga, LocalDateTime.now())) {
            throw new ConflictoException(
                "La propuesta ya fue respondida o venció.");
        }
    }

    private boolean estaPendienteYVigente(
            ProrrogaPedido prorroga, LocalDateTime ahora) {
        return prorroga != null
            && prorroga.getEstado() == EstadoProrroga.PENDIENTE
            && prorroga.getFechaLimiteRespuesta() != null
            && prorroga.getFechaLimiteRespuesta().isAfter(ahora);
    }

    private int horasRespuesta() {
        ConfiguracionPedido config = configuracionPedidoService.obtener();
        return config.getHorasRespuestaProrroga() != null
            ? config.getHorasRespuestaProrroga()
            : ConfiguracionPedido.HORAS_RESPUESTA_PRORROGA_POR_DEFECTO;
    }

    private void notificarSolicitada(
            Pedido pedido, ProrrogaPedido prorroga) {
        LocalDate nueva = PedidoFechas.entregaPrevista(pedido) != null
            ? PedidoFechas.entregaPrevista(pedido)
                .plusDays(prorroga.getDiasSolicitados())
            : null;
        notificacionService.crearParaUsuario(
            pedido.getCliente().getIdUsuario(),
            NOTIF_SOLICITADA,
            "El artista pidió más plazo",
            "Pidió correr la entrega de '" + nombreServicio(pedido) + "' "
                + prorroga.getDiasSolicitados() + " día(s)"
                + (nueva != null
                    ? " (hasta el " + FECHA.format(nueva) + ")"
                    : "")
                + ". Respondé antes del "
                + FECHA_HORA.format(prorroga.getFechaLimiteRespuesta())
                + " o se mantiene el plazo original.",
            pedido.getIdPedido(),
            TIPO_REFERENCIA);
    }

    private void notificarAceptada(
            Pedido pedido, ProrrogaPedido prorroga) {
        notificacionService.crearParaUsuario(
            pedido.getArtista().getIdUsuario(),
            NOTIF_ACEPTADA,
            "Prórroga aceptada",
            "El cliente aceptó correr la entrega de '"
                + nombreServicio(pedido) + "' "
                + prorroga.getDiasSolicitados() + " día(s). Nueva fecha: "
                + FECHA.format(prorroga.getEntregaPrevistaDespues()) + ".",
            pedido.getIdPedido(),
            TIPO_REFERENCIA);
    }

    private void notificarRechazada(
            Pedido pedido, ProrrogaPedido prorroga) {
        String motivo = prorroga.getMotivoRespuesta() != null
            ? " Motivo: " + prorroga.getMotivoRespuesta()
            : "";
        notificacionService.crearParaUsuario(
            pedido.getArtista().getIdUsuario(),
            NOTIF_RECHAZADA,
            "Prórroga rechazada",
            "El cliente rechazó la prórroga de '"
                + nombreServicio(pedido)
                + "'. Se mantiene el plazo original." + motivo,
            pedido.getIdPedido(),
            TIPO_REFERENCIA);
    }

    private void notificarVencida(
            Pedido pedido, ProrrogaPedido prorroga) {
        if (pedido == null) {
            return;
        }
        String descripcion = "La prórroga de '" + nombreServicio(pedido)
            + "' venció sin respuesta. Se mantienen los plazos originales.";
        notificacionService.crearParaUsuario(
            pedido.getArtista().getIdUsuario(),
            NOTIF_VENCIDA,
            "Prórroga vencida",
            descripcion,
            pedido.getIdPedido(),
            TIPO_REFERENCIA);
        notificacionService.crearParaUsuario(
            pedido.getCliente().getIdUsuario(),
            NOTIF_VENCIDA,
            "Prórroga vencida",
            descripcion,
            pedido.getIdPedido(),
            TIPO_REFERENCIA);
    }

    private String nombreServicio(Pedido pedido) {
        return pedido.getServicio() != null
                && pedido.getServicio().getNombre() != null
            ? pedido.getServicio().getNombre()
            : "tu pedido";
    }

    private String limpiarTexto(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.isEmpty()) {
            return null;
        }
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException(
                "El texto no puede superar los " + maximo + " caracteres.");
        }
        return limpio;
    }
}
