package com.temipora.backend_springboot.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class PresupuestoDTOTest {

    @Test
    void cliente_noRecibeComisionNiNeto() {
        PresupuestoDTO dto = PresupuestoDTO.desdePresupuesto(
            presupuesto(), null, null, null, false);

        assertThat(dto.getComisionPorcentaje()).isNull();
        assertThat(dto.getComisionMonto()).isNull();
        assertThat(dto.getComisionMontoCobro()).isNull();
        assertThat(dto.getComisionMontoMostrado()).isNull();
        assertThat(dto.getMontoNetoArtista()).isNull();
        assertThat(dto.getMontoNetoArtistaCobro()).isNull();
        assertThat(dto.getMontoNetoArtistaMostrado()).isNull();

        assertThat(dto.getHitos()).hasSize(1);
        assertThat(dto.getHitos().get(0).getComisionMonto()).isNull();
        assertThat(dto.getHitos().get(0).getMontoNeto()).isNull();
        assertThat(dto.getHitos().get(0).getMonto())
            .isEqualByComparingTo("100.00");
    }

    @Test
    void artista_siRecibeComisionYNeto() {
        PresupuestoDTO dto = PresupuestoDTO.desdePresupuesto(
            presupuesto(), null, null, null, true);

        assertThat(dto.getComisionPorcentaje()).isEqualByComparingTo("10.00");
        assertThat(dto.getComisionMonto()).isEqualByComparingTo("10.00");
        assertThat(dto.getMontoNetoArtista()).isEqualByComparingTo("90.00");

        assertThat(dto.getHitos()).hasSize(1);
        assertThat(dto.getHitos().get(0).getComisionMonto())
            .isEqualByComparingTo("10.00");
        assertThat(dto.getHitos().get(0).getMontoNeto())
            .isEqualByComparingTo("90.00");
    }

    @Test
    void jsonDelCliente_noIncluyeCamposDeComision() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String jsonCliente = mapper.writeValueAsString(
            PresupuestoDTO.desdePresupuesto(presupuesto(), null, null, null, false));
        assertThat(jsonCliente)
            .doesNotContain("comisionPorcentaje")
            .doesNotContain("comisionMonto")
            .doesNotContain("montoNetoArtista");

        String jsonArtista = mapper.writeValueAsString(
            PresupuestoDTO.desdePresupuesto(presupuesto(), null, null, null, true));
        assertThat(jsonArtista)
            .contains("comisionPorcentaje")
            .contains("comisionMonto")
            .contains("montoNetoArtista");
    }

    private Presupuesto presupuesto() {
        Moneda ars = Moneda.builder()
            .idMoneda(3L)
            .codigo("ARS")
            .nombre("Peso argentino")
            .build();

        Presupuesto presupuesto = new Presupuesto();
        presupuesto.setIdPresupuesto(1L);
        presupuesto.setPrecioBaseTotal(new BigDecimal("100.00"));
        presupuesto.setPrecioFinal(new BigDecimal("100.00"));
        presupuesto.setMoneda(ars);
        presupuesto.setMonedaCobro(ars);
        presupuesto.setCotizacionCobro(BigDecimal.ONE);
        presupuesto.setPrecioBaseTotalCobro(new BigDecimal("100.00"));
        presupuesto.setPrecioFinalCobro(new BigDecimal("100.00"));
        presupuesto.setComisionPorcentaje(new BigDecimal("10.00"));
        presupuesto.setComisionMonto(new BigDecimal("10.00"));
        presupuesto.setComisionMontoCobro(new BigDecimal("10.00"));
        presupuesto.setMontoNetoArtista(new BigDecimal("90.00"));
        presupuesto.setMontoNetoArtistaCobro(new BigDecimal("90.00"));
        presupuesto.setTiempoEntregaValor(2);
        presupuesto.setTiempoEntregaUnidad(ETiempoEntregaUnidad.SEMANAS);
        presupuesto.setTiempoEntregaDias(14);
        presupuesto.setFechaEmision(LocalDateTime.now());
        presupuesto.setFechaVencimiento(LocalDateTime.now().plusDays(3));

        HitoPresupuesto hito = new HitoPresupuesto();
        hito.setOrden(0);
        hito.setNombre("Pago único");
        hito.setTipo(TipoHito.SENA);
        hito.setPorcentaje(new BigDecimal("100.00"));
        hito.setMonto(new BigDecimal("100.00"));
        hito.setMontoCobro(new BigDecimal("100.00"));
        hito.setComisionMonto(new BigDecimal("10.00"));
        hito.setComisionMontoCobro(new BigDecimal("10.00"));
        hito.setMontoNeto(new BigDecimal("90.00"));
        hito.setMontoNetoCobro(new BigDecimal("90.00"));
        hito.setRequiereEntrega(true);
        presupuesto.agregarHito(hito);

        return presupuesto;
    }
}
