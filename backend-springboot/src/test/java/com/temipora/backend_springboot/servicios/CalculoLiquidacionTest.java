package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CalculoLiquidacionTest {

    @Test
    void descuentaElFeeDeMercadoPagoDelNetoDelArtista() {
        BigDecimal neto = CalculoLiquidacion.netoRecibido(
            new BigDecimal("50.00"), new BigDecimal("2.50"),
            new BigDecimal("4.55"));

        assertThat(neto).isEqualByComparingTo("45.45");
    }

    @Test
    void coincideConLaLiquidacionDeLaOperacion25() {
        BigDecimal neto = CalculoLiquidacion.netoRecibido(
            new BigDecimal("100.00"), new BigDecimal("5.00"),
            new BigDecimal("9.10"));

        assertThat(neto).isEqualByComparingTo("90.90");
    }

    @Test
    void sinArancelInformadoDevuelveElNetoTeorico() {
        BigDecimal neto = CalculoLiquidacion.netoRecibido(
            new BigDecimal("50.00"), new BigDecimal("2.50"), null);

        assertThat(neto).isEqualByComparingTo("47.50");
    }

    @Test
    void noDescuentaUnFeeNegativoSiLaComisionSuperaAlArancel() {
        BigDecimal neto = CalculoLiquidacion.netoRecibido(
            new BigDecimal("100.00"), new BigDecimal("10.00"),
            new BigDecimal("4.00"));

        assertThat(neto).isEqualByComparingTo("90.00");
    }
}
