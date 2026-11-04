package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.AlertaAdminDTO;
import com.temipora.backend_springboot.repositorios.EventoAuditoriaRepository;

@ExtendWith(MockitoExtension.class)
class AlertasAdminServiceTest {

    @Mock
    private EventoAuditoriaRepository eventoRepository;

    @InjectMocks
    private AlertasAdminService alertasService;

    @BeforeEach
    void setUp() {
        lenient().when(eventoRepository.contarPorActor(
            anyString(), any(), anyInt())).thenReturn(List.of());
        lenient().when(eventoRepository.contarPorActorEnAcciones(
            any(), any(), anyInt())).thenReturn(List.of());
        lenient().when(eventoRepository.countByAccionAndOcurridoEnBetween(
            anyString(), any(), any())).thenReturn(0L);
    }

    @Test
    void listar_sinActividad_devuelveVacio() {
        assertThat(alertasService.listar()).isEmpty();
    }

    @Test
    void listar_loginsFallidosPorUsuario_generaAlertaAlta() {
        when(eventoRepository.contarPorActor(
            eq(AccionesAdmin.LOGIN_FALLIDO), any(), eq(5)))
            .thenReturn(List.<Object[]>of(new Object[] { "ana", 7L }));

        List<AlertaAdminDTO> alertas = alertasService.listar();

        assertThat(alertas).hasSize(1);
        AlertaAdminDTO alerta = alertas.get(0);
        assertThat(alerta.codigo()).isEqualTo("LOGIN_FALLIDO_USUARIO");
        assertThat(alerta.severidad()).isEqualTo("ALTA");
        assertThat(alerta.actorNombre()).isEqualTo("ana");
        assertThat(alerta.cantidad()).isEqualTo(7);
        assertThat(alerta.accion()).isEqualTo(AccionesAdmin.LOGIN_FALLIDO);
    }

    @Test
    void listar_pagosRechazados_generaAlertaMedia() {
        when(eventoRepository.countByAccionAndOcurridoEnBetween(
            eq(AccionesAdmin.PAGO_RECHAZAR), any(), any()))
            .thenReturn(4L);

        List<AlertaAdminDTO> alertas = alertasService.listar();

        assertThat(alertas).hasSize(1);
        assertThat(alertas.get(0).codigo()).isEqualTo("PAGOS_RECHAZADOS");
        assertThat(alertas.get(0).severidad()).isEqualTo("MEDIA");
        assertThat(alertas.get(0).cantidad()).isEqualTo(4);
    }

    @Test
    void listar_ordenaLasAlertasAltasPrimero() {
        when(eventoRepository.contarPorActor(
            eq(AccionesAdmin.LOGIN_FALLIDO), any(), eq(5)))
            .thenReturn(List.<Object[]>of(new Object[] { "ana", 7L }));
        when(eventoRepository.countByAccionAndOcurridoEnBetween(
            eq(AccionesAdmin.PAGO_RECHAZAR), any(), any()))
            .thenReturn(4L);

        List<AlertaAdminDTO> alertas = alertasService.listar();

        assertThat(alertas)
            .extracting(AlertaAdminDTO::codigo)
            .containsExactly("LOGIN_FALLIDO_USUARIO", "PAGOS_RECHAZADOS");
    }

    @Test
    void listar_edicionMasivaDeServicios_generaAlerta() {
        when(eventoRepository.contarPorActorEnAcciones(
            org.mockito.ArgumentMatchers.<Collection<String>>argThat(
                acciones -> acciones.contains(AccionesAdmin.SERVICIO_MODIFICAR)),
            any(),
            eq(10)))
            .thenReturn(List.<Object[]>of(new Object[] { "artista1", 12L }));

        List<AlertaAdminDTO> alertas = alertasService.listar();

        assertThat(alertas).hasSize(1);
        assertThat(alertas.get(0).codigo()).isEqualTo("CATALOGO_EDICION_MASIVA");
        assertThat(alertas.get(0).actorNombre()).isEqualTo("artista1");
    }
}
