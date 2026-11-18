package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

class RankingSoporteTest {

    @Test
    void jaccard_manejaVaciosYSolapamiento() {
        assertThat(RankingSoporte.jaccard(null, Set.of(1L))).isZero();
        assertThat(RankingSoporte.jaccard(Set.of(), Set.of(1L))).isZero();
        assertThat(RankingSoporte.jaccard(Set.of(1L, 2L), Set.of(2L, 3L)))
            .isCloseTo(1.0 / 3.0, within(0.0001));
    }

    @Test
    void normalizar_descartaPopularidadCero() {
        Map<Long, Double> resultado = RankingSoporte.normalizar(
            Map.of(1L, 5L, 2L, 1L),
            Map.of(1L, 10L, 2L, 0L));

        assertThat(resultado).containsEntry(1L, 0.5).doesNotContainKey(2L);
    }

    @Test
    void normalizar_sinCrudoDevuelveVacio() {
        assertThat(RankingSoporte.normalizar(null, Map.of())).isEmpty();
    }

    @Test
    void recencia_decaeConLosDias() {
        assertThat(RankingSoporte.recencia(null, 90)).isZero();
        assertThat(RankingSoporte.recencia(LocalDateTime.now(), 90))
            .isCloseTo(1.0, within(0.001));
        assertThat(RankingSoporte.recencia(
            LocalDateTime.now().minusDays(200), 90)).isZero();
    }

    @Test
    void precioSim_comparaContraLaReferencia() {
        assertThat(RankingSoporte.precioSim(null, BigDecimal.TEN, BigDecimal.TEN))
            .isEqualTo(0.5);
        assertThat(RankingSoporte.precioSim(
            BigDecimal.TEN, BigDecimal.TEN, new BigDecimal("40"))).isEqualTo(1);
        assertThat(RankingSoporte.precioSim(
            BigDecimal.TEN, new BigDecimal("30"), new BigDecimal("40"))).isZero();
    }

    @Test
    void diversificarPorPrecio_conservaTodosLosElementos() {
        List<BigDecimal> precios = List.of(
            new BigDecimal("10"),
            new BigDecimal("20"),
            new BigDecimal("30"),
            new BigDecimal("40"));

        List<BigDecimal> resultado = RankingSoporte.diversificarPorPrecio(
            precios, Function.identity(), 3);

        assertThat(resultado).hasSize(4).containsExactlyInAnyOrderElementsOf(precios);
    }

    @Test
    void diversificarPorPrecio_listaCortaDevuelveCopia() {
        List<BigDecimal> precios = List.of(BigDecimal.TEN);

        List<BigDecimal> resultado = RankingSoporte.diversificarPorPrecio(
            precios, Function.identity(), 3);

        assertThat(resultado).containsExactly(BigDecimal.TEN);
    }
}
