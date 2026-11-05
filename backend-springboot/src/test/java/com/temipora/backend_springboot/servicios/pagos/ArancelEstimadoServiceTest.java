package com.temipora.backend_springboot.servicios.pagos;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.temipora.backend_springboot.modelo.ArancelEstimadoDTO;

class ArancelEstimadoServiceTest {

    @Test
    void calculaElPorcentajeTotalConIva() {
        ArancelEstimadoService service = new ArancelEstimadoService(
            new BigDecimal("3.49"), new BigDecimal("21"));

        ArancelEstimadoDTO dto = service.estimar();

        assertThat(dto.porcentaje()).isEqualByComparingTo("3.49");
        assertThat(dto.ivaPorcentaje()).isEqualByComparingTo("21");
        assertThat(dto.porcentajeTotal()).isEqualByComparingTo("4.2229");
    }

    @Test
    void sinIvaElTotalEsElPorcentaje() {
        ArancelEstimadoService service = new ArancelEstimadoService(
            new BigDecimal("3.49"), BigDecimal.ZERO);

        assertThat(service.estimar().porcentajeTotal())
            .isEqualByComparingTo("3.4900");
    }
}
