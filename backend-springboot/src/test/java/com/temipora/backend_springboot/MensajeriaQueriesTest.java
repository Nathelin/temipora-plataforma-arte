package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import com.temipora.backend_springboot.modelo.ConfiguracionMensajeria;
import com.temipora.backend_springboot.modelo.EstadoConversacion;
import com.temipora.backend_springboot.modelo.EstadoMensaje;
import com.temipora.backend_springboot.modelo.TipoAccionConversacion;
import com.temipora.backend_springboot.repositorios.ConfiguracionMensajeriaRepository;
import com.temipora.backend_springboot.repositorios.ConversacionAccionRepository;
import com.temipora.backend_springboot.repositorios.ConversacionParticipanteRepository;
import com.temipora.backend_springboot.repositorios.ConversacionRepository;
import com.temipora.backend_springboot.repositorios.MensajeRepository;
import com.temipora.backend_springboot.repositorios.TipoNotificacionRepository;

@SpringBootTest
class MensajeriaQueriesTest {

    @Autowired
    private ConversacionRepository conversacionRepository;

    @Autowired
    private ConversacionParticipanteRepository participanteRepository;

    @Autowired
    private MensajeRepository mensajeRepository;

    @Autowired
    private ConversacionAccionRepository accionRepository;

    @Autowired
    private ConfiguracionMensajeriaRepository configuracionRepository;

    @Autowired
    private TipoNotificacionRepository tipoNotificacionRepository;

    @Test
    void configuracion_mensajeriaTieneSuFilaUnica() {
        assertThat(configuracionRepository.findById(ConfiguracionMensajeria.ID_UNICO))
            .isPresent();
    }

    @Test
    void seeds_existenLosTiposDeNotificacionDeMensajeria() {
        assertThat(tipoNotificacionRepository.findByCodigo(
            "SOLICITUD_MENSAJE_RECIBIDA")).isPresent();
        assertThat(tipoNotificacionRepository.findByCodigo(
            "SOLICITUD_MENSAJE_ACEPTADA")).isPresent();
        assertThat(tipoNotificacionRepository.findByCodigo(
            "SOLICITUD_MENSAJE_RECHAZADA")).isPresent();
        assertThat(tipoNotificacionRepository.findByCodigo(
            "SOLICITUD_MENSAJE_CANCELADA")).isPresent();
        assertThat(tipoNotificacionRepository.findByCodigo(
            "MENSAJE_RECIBIDO")).isPresent();
    }

    @Test
    void conversacion_busquedaPorParInexistente_esVacia() {
        assertThat(conversacionRepository.buscarPorPar(-1L, -2L)).isEmpty();
        assertThat(conversacionRepository
            .countByIniciadorIdUsuarioAndEstado(
                -1L, EstadoConversacion.PENDIENTE)).isZero();
        assertThat(conversacionRepository
            .countByDestinatarioIdUsuarioAndEstado(
                -1L, EstadoConversacion.PENDIENTE)).isZero();
        assertThat(conversacionRepository
            .findByEstadoAndFechaSolicitudBefore(
                EstadoConversacion.PENDIENTE,
                LocalDateTime.of(1900, 1, 1, 0, 0)))
            .isEmpty();
    }

    @Test
    void bandeja_deUsuarioInexistente_seEjecutaVacia() {
        assertThat(participanteRepository.listarBandeja(
            -1L,
            false,
            List.of(EstadoConversacion.values()),
            false,
            false,
            PageRequest.of(0, 10)).getContent()).isEmpty();
        assertThat(participanteRepository
            .findByConversacionIdConversacionAndUsuarioIdUsuario(-1L, -1L))
            .isEmpty();
    }

    @Test
    void historial_yAgregados_deConversacionInexistente_seEjecutanVacios() {
        assertThat(mensajeRepository.listarHistorial(
            -1L, null, EstadoMensaje.ENVIADO, PageRequest.of(0, 10))).isEmpty();
        assertThat(mensajeRepository.ultimosMensajes(
            List.of(-1L), EstadoMensaje.ENVIADO)).isEmpty();
        assertThat(mensajeRepository.contarNoLeidos(
            -1L, EstadoMensaje.ENVIADO, EstadoConversacion.ACEPTADA)).isZero();
        assertThat(mensajeRepository.contarNoLeidosPorConversacion(
            -1L, List.of(-1L), EstadoMensaje.ENVIADO)).isEmpty();
        assertThat(mensajeRepository
            .countByRemitenteIdUsuarioAndFechaEnvioAfter(
                -1L, LocalDateTime.now().minusDays(1))).isZero();
    }

    @Test
    void acciones_deReferenciaInexistente_seEjecutanVacias() {
        assertThat(accionRepository
            .countByUsuarioIdUsuarioAndTipoAccionInAndFechaAfter(
                -1L,
                List.of(TipoAccionConversacion.SOLICITUD,
                    TipoAccionConversacion.REENVIO),
                LocalDateTime.now().minusDays(1))).isZero();
        assertThat(accionRepository
            .findFirstByConversacionIdConversacionAndTipoAccionOrderByFechaDesc(
                -1L, TipoAccionConversacion.RECHAZO))
            .isEmpty();
    }
}
