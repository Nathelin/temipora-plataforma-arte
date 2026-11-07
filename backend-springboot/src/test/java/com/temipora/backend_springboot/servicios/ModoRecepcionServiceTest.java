package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.ModoRecepcionSolicitud;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;

@ExtendWith(MockitoExtension.class)
class ModoRecepcionServiceTest {

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private ModoRecepcionService service;

    @Test
    void sinCupos_pasaAListaDeEsperaYGuardaLaPreferencia() {
        Servicio servicio = servicio(1, ModoRecepcionSolicitud.ABIERTA, null);
        when(servicioRepository.findByIdServicioForUpdate(1L))
            .thenReturn(Optional.of(servicio));
        when(solicitudRepository.countByServicioIdServicioAndEstadoIn(
            eq(1L), anyCollection())).thenReturn(1L);

        service.reconciliar(1L);

        assertThat(servicio.getModoRecepcion()).isEqualTo(ModoRecepcionSolicitud.SOLO_ESPERA);
        assertThat(servicio.getModoRecepcionPreferido())
            .isEqualTo(ModoRecepcionSolicitud.ABIERTA);
        assertThat(servicio.isPermiteListaEspera()).isTrue();
        verify(auditoria).registrar(
            eq(AccionesAdmin.SERVICIO_MODO_AUTOMATICO),
            any(), any(), any(), any(), any(), any());
    }

    @Test
    void conCupoLibre_vuelveAAbiertaYLiberaLaPreferencia() {
        Servicio servicio = servicio(
            1, ModoRecepcionSolicitud.SOLO_ESPERA, ModoRecepcionSolicitud.ABIERTA);
        when(servicioRepository.findByIdServicioForUpdate(1L))
            .thenReturn(Optional.of(servicio));
        when(solicitudRepository.countByServicioIdServicioAndEstadoIn(
            eq(1L), anyCollection())).thenReturn(0L);

        service.reconciliar(1L);

        assertThat(servicio.getModoRecepcion()).isEqualTo(ModoRecepcionSolicitud.ABIERTA);
        assertThat(servicio.getModoRecepcionPreferido()).isNull();
        assertThat(servicio.isPermiteListaEspera()).isFalse();
    }

    @Test
    void esperaElegidaPorElArtista_noSeToca() {
        Servicio servicio = servicio(1, ModoRecepcionSolicitud.SOLO_ESPERA, null);
        when(servicioRepository.findByIdServicioForUpdate(1L))
            .thenReturn(Optional.of(servicio));

        service.reconciliar(1L);

        assertThat(servicio.getModoRecepcion()).isEqualTo(ModoRecepcionSolicitud.SOLO_ESPERA);
        verify(servicioRepository, never()).save(any());
        verify(auditoria, never()).registrar(
            any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void servicioPausado_noSeToca() {
        Servicio servicio = servicio(1, ModoRecepcionSolicitud.ABIERTA, null);
        servicio.setEstado(EstadoServicio.PAUSADO);
        when(servicioRepository.findByIdServicioForUpdate(1L))
            .thenReturn(Optional.of(servicio));

        service.reconciliar(1L);

        assertThat(servicio.getModoRecepcion()).isEqualTo(ModoRecepcionSolicitud.ABIERTA);
        verify(servicioRepository, never()).save(any());
    }

    @Test
    void yaEnEsperaPorElSistema_esIdempotente() {
        Servicio servicio = servicio(
            1, ModoRecepcionSolicitud.SOLO_ESPERA, ModoRecepcionSolicitud.ABIERTA);
        when(servicioRepository.findByIdServicioForUpdate(1L))
            .thenReturn(Optional.of(servicio));
        when(solicitudRepository.countByServicioIdServicioAndEstadoIn(
            eq(1L), anyCollection())).thenReturn(1L);

        service.reconciliar(1L);

        assertThat(servicio.getModoRecepcion()).isEqualTo(ModoRecepcionSolicitud.SOLO_ESPERA);
        verify(servicioRepository, never()).save(any());
    }

    private Servicio servicio(
            int cupos,
            ModoRecepcionSolicitud modo,
            ModoRecepcionSolicitud preferido) {
        Servicio servicio = new Servicio();
        servicio.setIdServicio(1L);
        servicio.setEstado(EstadoServicio.PUBLICADO);
        servicio.setCupos(cupos);
        servicio.setModoRecepcion(modo);
        servicio.setModoRecepcionPreferido(preferido);
        servicio.setPermiteListaEspera(modo == ModoRecepcionSolicitud.SOLO_ESPERA);
        return servicio;
    }
}
