package com.temipora.backend_springboot.tareas;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.servicios.ConversacionService;

@ExtendWith(MockitoExtension.class)
class SolicitudMensajeVencimientoJobTest {

    @Mock
    private ConversacionService conversacionService;

    @InjectMocks
    private SolicitudMensajeVencimientoJob job;

    @Test
    void ejecutar_pideVencerLasPendientes() {
        when(conversacionService.vencerPendientes()).thenReturn(2);

        job.ejecutar();

        verify(conversacionService).vencerPendientes();
    }

    @Test
    void ejecutar_siElServicioFalla_noPropagaElError() {
        when(conversacionService.vencerPendientes())
            .thenThrow(new RuntimeException("sin conexión"));

        job.ejecutar();
    }
}
