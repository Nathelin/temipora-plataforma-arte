package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;

@ExtendWith(MockitoExtension.class)
class ColaSolicitudesServiceTest {

    private static final Long SERVICIO = 7L;

    @Mock
    private SolicitudRepository solicitudRepository;

    @InjectMocks
    private ColaSolicitudesService colaSolicitudesService;

    private Servicio servicio;

    @BeforeEach
    void setUp() {
        servicio = new Servicio();
        servicio.setIdServicio(SERVICIO);
        servicio.setCupos(3);
        servicio.setNombre("Ilustración");
    }

    @Test
    void cuposDisponibles_descuentaLosOcupados() {
        when(solicitudRepository.countByServicioIdServicioAndEstadoIn(eq(SERVICIO), any()))
            .thenReturn(2L);

        assertThat(colaSolicitudesService.cuposOcupados(SERVICIO)).isEqualTo(2);
        assertThat(colaSolicitudesService.cuposDisponibles(servicio)).isEqualTo(1);
    }

    @Test
    void cuposDisponibles_nuncaEsNegativo() {
        when(solicitudRepository.countByServicioIdServicioAndEstadoIn(eq(SERVICIO), any()))
            .thenReturn(5L);

        assertThat(colaSolicitudesService.cuposDisponibles(servicio)).isZero();
    }

    @Test
    void posicion_devuelveElLugarEnLaCola() {
        Solicitud primera = solicitud(1L, EstadoSolicitud.PENDIENTE);
        Solicitud segunda = solicitud(2L, EstadoSolicitud.EN_LISTA_ESPERA);
        when(solicitudRepository
            .findByServicioIdServicioAndEstadoInOrderByFechaCreacionAscIdSolicitudAsc(
                eq(SERVICIO), any()))
            .thenReturn(List.of(primera, segunda));

        assertThat(colaSolicitudesService.posicion(SERVICIO, 1L)).isEqualTo(1);
        assertThat(colaSolicitudesService.posicion(SERVICIO, 2L)).isEqualTo(2);
        assertThat(colaSolicitudesService.posicion(SERVICIO, 99L)).isNull();
    }

    @Test
    void evaluar_permiteLasPrimerasNSolicitudes() {
        Solicitud primera = solicitud(1L, EstadoSolicitud.PENDIENTE);
        Solicitud segunda = solicitud(2L, EstadoSolicitud.PENDIENTE);
        Solicitud tercera = solicitud(3L, EstadoSolicitud.PENDIENTE);
        when(solicitudRepository.countByServicioIdServicioAndEstadoIn(eq(SERVICIO), any()))
            .thenReturn(1L);
        when(solicitudRepository
            .findByServicioIdServicioAndEstadoInOrderByFechaCreacionAscIdSolicitudAsc(
                eq(SERVICIO), any()))
            .thenReturn(List.of(primera, segunda, tercera));

        assertThat(colaSolicitudesService.evaluar(primera).puede()).isTrue();
        assertThat(colaSolicitudesService.evaluar(segunda).puede()).isTrue();
        assertThat(colaSolicitudesService.evaluar(tercera).puede()).isFalse();
        assertThat(colaSolicitudesService.evaluar(tercera).motivo())
            .contains("solicitudes anteriores");
    }

    @Test
    void evaluar_sinCupos_bloquea() {
        Solicitud primera = solicitud(1L, EstadoSolicitud.PENDIENTE);
        when(solicitudRepository.countByServicioIdServicioAndEstadoIn(eq(SERVICIO), any()))
            .thenReturn(3L);
        when(solicitudRepository
            .findByServicioIdServicioAndEstadoInOrderByFechaCreacionAscIdSolicitudAsc(
                eq(SERVICIO), any()))
            .thenReturn(List.of(primera));

        ColaSolicitudesService.EvaluacionPresupuesto evaluacion =
            colaSolicitudesService.evaluar(primera);

        assertThat(evaluacion.puede()).isFalse();
        assertThat(evaluacion.motivo()).contains("No hay cupos libres");
    }

    @Test
    void evaluar_solicitudCerrada_bloquea() {
        Solicitud rechazada = solicitud(1L, EstadoSolicitud.RECHAZADA);

        ColaSolicitudesService.EvaluacionPresupuesto evaluacion =
            colaSolicitudesService.evaluar(rechazada);

        assertThat(evaluacion.puede()).isFalse();
        assertThat(evaluacion.motivo()).contains("ya no se puede presupuestar");
    }

    @Test
    void evaluar_servicioArchivado_bloquea() {
        servicio.setEstado(EstadoServicio.ARCHIVADO);
        Solicitud pendiente = solicitud(1L, EstadoSolicitud.PENDIENTE);

        ColaSolicitudesService.EvaluacionPresupuesto evaluacion =
            colaSolicitudesService.evaluar(pendiente);

        assertThat(evaluacion.puede()).isFalse();
        assertThat(evaluacion.motivo()).contains("archivado");
    }

    @Test
    void cola_devuelveLasSolicitudesEnOrden() {
        Solicitud primera = solicitud(1L, EstadoSolicitud.PENDIENTE);
        Solicitud segunda = solicitud(2L, EstadoSolicitud.EN_LISTA_ESPERA);
        when(solicitudRepository
            .findByServicioIdServicioAndEstadoInOrderByFechaCreacionAscIdSolicitudAsc(
                eq(SERVICIO), any()))
            .thenReturn(List.of(primera, segunda));

        assertThat(colaSolicitudesService.cola(SERVICIO))
            .containsExactly(primera, segunda);
    }

    private Solicitud solicitud(Long id, EstadoSolicitud estado) {
        Solicitud solicitud = new Solicitud();
        solicitud.setIdSolicitud(id);
        solicitud.setServicio(servicio);
        solicitud.setEstado(estado);
        solicitud.setFechaCreacion(LocalDateTime.now());
        return solicitud;
    }
}
