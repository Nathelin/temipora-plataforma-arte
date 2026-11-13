package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Conversión del rango de precio a cada moneda del catálogo.
 *
 * Es la parte que decide si un servicio cargado en otra moneda entra o no en
 * el filtro, así que el rango tiene que expresarse en la moneda del servicio.
 */
class ServicioServiceRangoPrecioTest {

    private static final BigDecimal FACTOR_USD_A_ARS = new BigDecimal("1000");

    @Test
    void rango_enLaMismaMoneda_noSeAltera() {
        Map<Long, BigDecimal> factores = Map.of(1L, BigDecimal.ONE);

        Map<Long, ServicioService.RangoPrecio> rangos = ServicioService.rangosPorMoneda(
            new BigDecimal("10000"), new BigDecimal("30000"), factores);

        ServicioService.RangoPrecio ars = rangos.get(1L);
        assertThat(ars.minimo()).isEqualByComparingTo("10000");
        assertThat(ars.maximo()).isEqualByComparingTo("30000");
    }

    @Test
    void rango_seDividePorElFactorDeLaOtraMoneda() {
        Map<Long, BigDecimal> factores = Map.of(2L, FACTOR_USD_A_ARS);

        Map<Long, ServicioService.RangoPrecio> rangos = ServicioService.rangosPorMoneda(
            new BigDecimal("10000"), new BigDecimal("30000"), factores);

        // Un servicio de USD 20 equivale a ARS 20.000, así que debe entrar.
        ServicioService.RangoPrecio usd = rangos.get(2L);
        assertThat(usd.minimo()).isEqualByComparingTo("10");
        assertThat(usd.maximo()).isEqualByComparingTo("30");
        assertThat(new BigDecimal("20")).isBetween(usd.minimo(), usd.maximo());
    }

    @Test
    void servicioEnDolaresFueraDelRango_noEntra() {
        Map<Long, BigDecimal> factores = Map.of(2L, FACTOR_USD_A_ARS);

        Map<Long, ServicioService.RangoPrecio> rangos = ServicioService.rangosPorMoneda(
            new BigDecimal("10000"), new BigDecimal("30000"), factores);

        ServicioService.RangoPrecio usd = rangos.get(2L);
        // USD 50 = ARS 50.000, por encima del máximo de 30.000.
        assertThat(new BigDecimal("50")).isGreaterThan(usd.maximo());
        // USD 5 = ARS 5.000, por debajo del mínimo de 10.000.
        assertThat(new BigDecimal("5")).isLessThan(usd.minimo());
    }

    @Test
    void soloMinimo_dejaElMaximoNulo() {
        Map<Long, BigDecimal> factores = Map.of(1L, BigDecimal.ONE, 2L, FACTOR_USD_A_ARS);

        Map<Long, ServicioService.RangoPrecio> rangos = ServicioService.rangosPorMoneda(
            new BigDecimal("20000"), null, factores);

        assertThat(rangos.get(1L).minimo()).isEqualByComparingTo("20000");
        assertThat(rangos.get(1L).maximo()).isNull();
        assertThat(rangos.get(2L).minimo()).isEqualByComparingTo("20");
        assertThat(rangos.get(2L).maximo()).isNull();
    }

    @Test
    void soloMaximo_dejaElMinimoNulo() {
        Map<Long, BigDecimal> factores = Map.of(2L, FACTOR_USD_A_ARS);

        Map<Long, ServicioService.RangoPrecio> rangos = ServicioService.rangosPorMoneda(
            null, new BigDecimal("50000"), factores);

        assertThat(rangos.get(2L).minimo()).isNull();
        assertThat(rangos.get(2L).maximo()).isEqualByComparingTo("50");
    }

    @Test
    void cadaMonedaPreservaSuPropioOrden() {
        Map<Long, BigDecimal> factores = new LinkedHashMap<>();
        factores.put(1L, BigDecimal.ONE);
        factores.put(2L, FACTOR_USD_A_ARS);

        Map<Long, ServicioService.RangoPrecio> rangos = ServicioService.rangosPorMoneda(
            new BigDecimal("15000"), new BigDecimal("45000"), factores);

        assertThat(rangos).hasSize(2);
        assertThat(rangos.get(1L).minimo()).isEqualByComparingTo("15000");
        assertThat(rangos.get(2L).minimo()).isEqualByComparingTo("15");
        assertThat(rangos.get(2L).maximo()).isEqualByComparingTo("45");
    }
}
