package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.temipora.backend_springboot.modelo.IntentoCobro;
import com.temipora.backend_springboot.modelo.Pago;
import com.temipora.backend_springboot.repositorios.IntentoCobroRepository;
import com.temipora.backend_springboot.repositorios.PagoRepository;
import com.temipora.backend_springboot.servicios.pagos.DatosPagoProveedor;

class ConciliacionPersistenciaTest {

    private final PagoRepository pagoRepository = mock(PagoRepository.class);
    private final IntentoCobroRepository intentoCobroRepository =
        mock(IntentoCobroRepository.class);
    private final AuditoriaService auditoria = mock(AuditoriaService.class);

    private final ConciliacionPersistencia persistencia =
        new ConciliacionPersistencia(
            pagoRepository, intentoCobroRepository, auditoria);

    @Test
    void completaSoloLosImportesQueFaltanYNoSobrescribe() {
        Pago pago = new Pago();
        pago.setIdPago(1L);
        pago.setArancelMp(new BigDecimal("5.00"));
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago));

        boolean completo = persistencia.completarPago(1L, datos());

        assertThat(completo).isTrue();
        assertThat(pago.getArancelMp()).isEqualByComparingTo("5.00");
        assertThat(pago.getNetoRecibidoMp()).isEqualByComparingTo("128.25");
        verify(pagoRepository).save(pago);
        verify(auditoria).registrar(eq(AccionesAdmin.PAGO_CONCILIAR),
            eq("Pago"), eq(1L), anyString(), anyString());
    }

    @Test
    void noGuardaNiAuditaSiNoHayNadaQueCompletar() {
        Pago pago = new Pago();
        pago.setIdPago(1L);
        pago.setArancelMp(new BigDecimal("5.00"));
        pago.setNetoRecibidoMp(new BigDecimal("128.25"));
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago));

        boolean completo = persistencia.completarPago(1L, datos());

        assertThat(completo).isFalse();
        verify(pagoRepository, never()).save(any());
        verify(auditoria, never()).registrar(anyString(), anyString(),
            any(), anyString(), anyString());
    }

    @Test
    void completaLosIntentosDeCobroDeProductos() {
        IntentoCobro intento = new IntentoCobro();
        intento.setIdIntentoCobro(9L);
        when(intentoCobroRepository.findById(9L))
            .thenReturn(Optional.of(intento));

        boolean completo = persistencia.completarIntento(9L, datos());

        assertThat(completo).isTrue();
        assertThat(intento.getNetoRecibidoMp())
            .isEqualByComparingTo("128.25");
        assertThat(intento.getArancelMp()).isEqualByComparingTo("15.75");
        verify(auditoria).registrar(eq(AccionesAdmin.PAGO_CONCILIAR),
            eq("Intento de cobro"), eq(9L), anyString(), anyString());
    }

    private DatosPagoProveedor datos() {
        return new DatosPagoProveedor(new BigDecimal("15.75"),
            new BigDecimal("128.25"), LocalDateTime.now(), null,
            "visa", "credit_card", 1, "4242");
    }
}
