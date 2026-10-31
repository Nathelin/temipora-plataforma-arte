package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionMensajeria;
import com.temipora.backend_springboot.modelo.Conversacion;
import com.temipora.backend_springboot.modelo.ConversacionParticipante;
import com.temipora.backend_springboot.modelo.EstadoConversacion;
import com.temipora.backend_springboot.modelo.EstadoMensaje;
import com.temipora.backend_springboot.modelo.HistorialMensajesDTO;
import com.temipora.backend_springboot.modelo.Mensaje;
import com.temipora.backend_springboot.modelo.MensajeDTO;
import com.temipora.backend_springboot.modelo.MensajeRequest;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ConversacionParticipanteRepository;
import com.temipora.backend_springboot.repositorios.ConversacionRepository;
import com.temipora.backend_springboot.repositorios.MensajeRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class MensajeService {

    private static final int TAMANIO_HISTORIAL_POR_DEFECTO = 30;
    private static final int TAMANIO_MAXIMO_HISTORIAL = 50;

    private final MensajeRepository mensajeRepository;
    private final ConversacionRepository conversacionRepository;
    private final ConversacionParticipanteRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionMensajeriaService configuracionService;
    private final IndicadorEscritura indicadorEscritura;

    public MensajeService(
            MensajeRepository mensajeRepository,
            ConversacionRepository conversacionRepository,
            ConversacionParticipanteRepository participanteRepository,
            UsuarioRepository usuarioRepository,
            ConfiguracionMensajeriaService configuracionService,
            IndicadorEscritura indicadorEscritura) {
        this.mensajeRepository = mensajeRepository;
        this.conversacionRepository = conversacionRepository;
        this.participanteRepository = participanteRepository;
        this.usuarioRepository = usuarioRepository;
        this.configuracionService = configuracionService;
        this.indicadorEscritura = indicadorEscritura;
    }

    @Transactional
    public MensajeDTO enviar(
            Long idConversacion,
            Long idRemitente,
            MensajeRequest request) {
        Conversacion conversacion = obtenerConversacion(idConversacion);
        if (!conversacion.involucra(idRemitente)) {
            throw new AccesoDenegadoException(
                "No podés enviar mensajes en esta conversación.");
        }
        Usuario remitente = usuarioRepository.findByIdUsuario(idRemitente)
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + idRemitente));
        if (!remitente.isEstadoCuenta()) {
            throw new AccesoDenegadoException(
                "Tu cuenta está deshabilitada para enviar mensajes.");
        }
        if (conversacion.getEstado() != EstadoConversacion.ACEPTADA) {
            throw new ConflictoException(motivoEstado(
                conversacion.getEstado()));
        }
        Usuario contraparte = conversacion.contraparteDe(idRemitente);
        if (!contraparte.isEstadoCuenta()) {
            throw new ConflictoException(
                "El usuario no está disponible para recibir mensajes.");
        }

        ConfiguracionMensajeria config = configuracionService.obtener();
        String contenido = TextoMensajeria.obligatorio(
            request != null ? request.getContenido() : null,
            config.getLongitudMaximaMensaje(),
            "El mensaje");

        long enviados = mensajeRepository
            .countByRemitenteIdUsuarioAndFechaEnvioAfter(
                idRemitente, LocalDateTime.now().minusDays(1));
        if (enviados >= config.getMaxMensajesPorDia()) {
            throw new ConflictoException(
                "Alcanzaste el límite de " + config.getMaxMensajesPorDia()
                    + " mensajes por día. Probá más tarde.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conversacion);
        mensaje.setRemitente(remitente);
        mensaje.setContenido(contenido);
        mensaje.setFechaEnvio(ahora);
        mensajeRepository.save(mensaje);

        conversacion.setFechaUltimoMensaje(ahora);
        conversacion.setFechaActualizacion(ahora);

        participanteRepository
            .findByConversacionIdConversacionAndUsuarioIdUsuario(
                idConversacion, remitente.getIdUsuario())
            .ifPresent(participante -> participante.setArchivada(false));
        participanteRepository
            .findByConversacionIdConversacionAndUsuarioIdUsuario(
                idConversacion, contraparte.getIdUsuario())
            .ifPresent(participante -> participante.setArchivada(false));
        indicadorEscritura.limpiar(idConversacion, idRemitente);

        return MensajeDTO.desdeMensaje(mensaje, idRemitente, null);
    }

    @Transactional(readOnly = true)
    public HistorialMensajesDTO historial(
            Long idConversacion,
            Long idUsuario,
            Long antesDe,
            Integer tamanio) {
        Conversacion conversacion = obtenerConversacion(idConversacion);
        if (!conversacion.involucra(idUsuario)) {
            throw new AccesoDenegadoException(
                "No podés ver esta conversación.");
        }

        int limite = tamanio == null
            ? TAMANIO_HISTORIAL_POR_DEFECTO
            : Math.min(Math.max(1, tamanio), TAMANIO_MAXIMO_HISTORIAL);
        List<Mensaje> filas = mensajeRepository.listarHistorial(
            idConversacion,
            antesDe,
            EstadoMensaje.ENVIADO,
            PageRequest.of(0, limite + 1));
        boolean hayMas = filas.size() > limite;
        List<Mensaje> pagina = hayMas ? filas.subList(0, limite) : filas;
        Long idCursorAnterior = pagina.isEmpty()
            ? null
            : pagina.get(pagina.size() - 1).getIdMensaje();

        LocalDateTime ultimoLeidoContraparte = participanteRepository
            .findByConversacionIdConversacionAndUsuarioIdUsuarioNot(
                idConversacion, idUsuario)
            .map(ConversacionParticipante::getUltimoLeidoEn)
            .orElse(null);

        List<MensajeDTO> mensajes = new ArrayList<>(pagina.stream()
            .map(mensaje -> MensajeDTO.desdeMensaje(
                mensaje, idUsuario, ultimoLeidoContraparte))
            .toList());
        Collections.reverse(mensajes);

        return HistorialMensajesDTO.builder()
            .mensajes(mensajes)
            .idCursorAnterior(idCursorAnterior)
            .hayMas(hayMas)
            .contraparteUltimoLeidoEn(ultimoLeidoContraparte)
            .build();
    }

    @Transactional
    public void marcarLeido(Long idConversacion, Long idUsuario) {
        Conversacion conversacion = obtenerConversacion(idConversacion);
        if (!conversacion.involucra(idUsuario)) {
            throw new AccesoDenegadoException(
                "No podés marcar esta conversación como leída.");
        }
        if (conversacion.getEstado() != EstadoConversacion.ACEPTADA) {
            return;
        }
        ConversacionParticipante participante = participanteRepository
            .findByConversacionIdConversacionAndUsuarioIdUsuario(
                idConversacion, idUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "Conversación no encontrada: " + idConversacion));

        LocalDateTime ahora = LocalDateTime.now();
        if (participante.getUltimoLeidoEn() == null
                || ahora.isAfter(participante.getUltimoLeidoEn())) {
            participante.setUltimoLeidoEn(ahora);
        }
    }

    private String motivoEstado(EstadoConversacion estado) {
        return switch (estado) {
            case PENDIENTE -> "La solicitud todavía no fue aceptada.";
            case RECHAZADA -> "La conversación fue rechazada.";
            case CANCELADA -> "La solicitud fue cancelada.";
            case ACEPTADA -> "La conversación no admite mensajes.";
        };
    }

    private Conversacion obtenerConversacion(Long idConversacion) {
        return conversacionRepository.findByIdConversacion(idConversacion)
            .orElseThrow(() -> new NoSuchElementException(
                "Conversación no encontrada: " + idConversacion));
    }
}
