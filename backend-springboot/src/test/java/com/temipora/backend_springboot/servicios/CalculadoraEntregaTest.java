package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.temipora.backend_springboot.modelo.ETiempoEntregaUnidad;

class CalculadoraEntregaTest {

    @Test
    void sumarPlazo_dias() {
        assertThat(CalculadoraEntrega.sumarPlazo(
            LocalDate.of(2026, 11, 10), 15, ETiempoEntregaUnidad.DIAS))
            .isEqualTo(LocalDate.of(2026, 11, 25));
    }

    @Test
    void sumarPlazo_semanas() {
        assertThat(CalculadoraEntrega.sumarPlazo(
            LocalDate.of(2026, 11, 10), 3, ETiempoEntregaUnidad.SEMANAS))
            .isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    void sumarPlazo_meses_ajustaAlUltimoDiaDelMes() {
        assertThat(CalculadoraEntrega.sumarPlazo(
            LocalDate.of(2026, 1, 31), 1, ETiempoEntregaUnidad.MESES))
            .isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void sumarPlazo_meses_respetaAnioBisiesto() {
        assertThat(CalculadoraEntrega.sumarPlazo(
            LocalDate.of(2024, 1, 31), 1, ETiempoEntregaUnidad.MESES))
            .isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @Test
    void sumarPlazo_meses_calendarioReal() {
        assertThat(CalculadoraEntrega.sumarPlazo(
            LocalDate.of(2026, 11, 10), 3, ETiempoEntregaUnidad.MESES))
            .isEqualTo(LocalDate.of(2027, 2, 10));
    }

    @Test
    void sumarPlazo_valorInvalido_lanzaError() {
        assertThatThrownBy(() -> CalculadoraEntrega.sumarPlazo(
            LocalDate.of(2026, 11, 10), 0, ETiempoEntregaUnidad.DIAS))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("al menos 1");
    }

    @Test
    void inicioReal_sinInicioEstimado_usaFechaDePago() {
        LocalDate pago = LocalDate.of(2026, 10, 25);

        assertThat(CalculadoraEntrega.inicioReal(pago, null)).isEqualTo(pago);
    }

    @Test
    void inicioReal_usaInicioEstimadoSiEsPosteriorAlPago() {
        LocalDate pago = LocalDate.of(2026, 10, 25);
        LocalDate inicio = LocalDate.of(2026, 11, 10);

        assertThat(CalculadoraEntrega.inicioReal(pago, inicio)).isEqualTo(inicio);
    }

    @Test
    void inicioReal_usaPagoSiEsPosteriorAlInicioEstimado() {
        LocalDate pago = LocalDate.of(2026, 11, 15);
        LocalDate inicio = LocalDate.of(2026, 11, 10);

        assertThat(CalculadoraEntrega.inicioReal(pago, inicio)).isEqualTo(pago);
    }

    @Test
    void fechaEntrega_pagoAntesDelInicio_entregaDesdeElInicio() {
        assertThat(CalculadoraEntrega.fechaEntrega(
            LocalDate.of(2026, 10, 25),
            LocalDate.of(2026, 11, 10),
            3,
            ETiempoEntregaUnidad.MESES))
            .isEqualTo(LocalDate.of(2027, 2, 10));
    }

    @Test
    void fechaEntrega_pagoDespuesDelInicio_entregaDesdeElPago() {
        assertThat(CalculadoraEntrega.fechaEntrega(
            LocalDate.of(2026, 11, 15),
            LocalDate.of(2026, 11, 10),
            3,
            ETiempoEntregaUnidad.MESES))
            .isEqualTo(LocalDate.of(2027, 2, 15));
    }

    @Test
    void diasNormalizados_equivalencias() {
        assertThat(CalculadoraEntrega.diasNormalizados(
            12, ETiempoEntregaUnidad.MESES)).isEqualTo(360);
        assertThat(CalculadoraEntrega.diasNormalizados(
            52, ETiempoEntregaUnidad.SEMANAS)).isEqualTo(364);
        assertThat(CalculadoraEntrega.diasNormalizados(
            365, ETiempoEntregaUnidad.DIAS)).isEqualTo(365);
    }

    @Test
    void excedePlazoMaximo_limiteDeUnAnio() {
        assertThat(CalculadoraEntrega.excedePlazoMaximo(
            365, ETiempoEntregaUnidad.DIAS)).isFalse();
        assertThat(CalculadoraEntrega.excedePlazoMaximo(
            366, ETiempoEntregaUnidad.DIAS)).isTrue();
        assertThat(CalculadoraEntrega.excedePlazoMaximo(
            52, ETiempoEntregaUnidad.SEMANAS)).isFalse();
        assertThat(CalculadoraEntrega.excedePlazoMaximo(
            53, ETiempoEntregaUnidad.SEMANAS)).isTrue();
        assertThat(CalculadoraEntrega.excedePlazoMaximo(
            12, ETiempoEntregaUnidad.MESES)).isFalse();
        assertThat(CalculadoraEntrega.excedePlazoMaximo(
            13, ETiempoEntregaUnidad.MESES)).isTrue();
    }
}
