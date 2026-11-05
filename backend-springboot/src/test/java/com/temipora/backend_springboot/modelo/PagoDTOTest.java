package com.temipora.backend_springboot.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class PagoDTOTest {

    @Test
    void desdePago_incluyeLosDatosDeMercadoPago() {
        Pago pago = new Pago();
        pago.setIdPago(1L);
        pago.setEstado(EstadoPago.APROBADO);
        pago.setMonto(new BigDecimal("100.00"));
        pago.setMontoCobro(new BigDecimal("100.00"));
        pago.setArancelMp(new BigDecimal("4.50"));
        pago.setNetoRecibidoMp(new BigDecimal("45.50"));
        pago.setMedioPago("visa");
        pago.setTipoMedioPago("credit_card");
        pago.setCuotas(3);
        pago.setUltimosCuatro("1234");
        pago.setFechaAprobacion(LocalDateTime.of(2026, 11, 2, 10, 0));
        pago.setFechaLiberacion(LocalDateTime.of(2026, 11, 5, 10, 0));

        PagoDTO dto = PagoDTO.desdePago(pago, false);

        assertThat(dto.getArancelMp()).isEqualByComparingTo("4.50");
        assertThat(dto.getNetoRecibidoMp()).isEqualByComparingTo("45.50");
        assertThat(dto.getMedioPago()).isEqualTo("visa");
        assertThat(dto.getTipoMedioPago()).isEqualTo("credit_card");
        assertThat(dto.getCuotas()).isEqualTo(3);
        assertThat(dto.getUltimosCuatro()).isEqualTo("1234");
        assertThat(dto.getFechaAprobacion()).isNotNull();
        assertThat(dto.getFechaLiberacion()).isNotNull();
    }
}
