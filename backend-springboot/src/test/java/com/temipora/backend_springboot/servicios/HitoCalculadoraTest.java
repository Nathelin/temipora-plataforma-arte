package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.temipora.backend_springboot.modelo.HitoPresupuesto;
import com.temipora.backend_springboot.modelo.HitoPresupuestoRequest;

class HitoCalculadoraTest {

    private static final HitoCalculadora.Limites LIMITES = new HitoCalculadora.Limites(
        4, new BigDecimal("50.00"), new BigDecimal("10.00"));

    @Test
    void sinHitos_creaUnicoPagoAlInicio() {
        List<HitoPresupuesto> hitos = construir(null,
            "200.00", "200.00", "20.00", "20.00");

        assertThat(hitos).hasSize(1);
        assertThat(hitos.get(0).getTipo().name()).isEqualTo("SENA");
        assertThat(hitos.get(0).getNombre()).isEqualTo("Pago único");
        assertThat(hitos.get(0).getPorcentaje()).isEqualByComparingTo("100.00");
        assertThat(hitos.get(0).getMonto()).isEqualByComparingTo("200.00");
        assertThat(hitos.get(0).getMontoNeto()).isEqualByComparingTo("180.00");
        assertThat(hitos.get(0).isRequiereEntrega()).isTrue();
    }

    @Test
    void dosHitos_generaSenaYEntregaConComisionProporcional() {
        List<HitoPresupuesto> hitos = construir(List.of(hito("50"), hito("50")),
            "200.00", "200.00", "20.00", "20.00");

        assertThat(hitos).hasSize(2);
        assertThat(hitos.get(0).getTipo().name()).isEqualTo("SENA");
        assertThat(hitos.get(0).getNombre()).isEqualTo("Seña");
        assertThat(hitos.get(0).isRequiereEntrega()).isTrue();
        assertThat(hitos.get(0).getComisionMonto()).isEqualByComparingTo("10.00");
        assertThat(hitos.get(0).getMontoNeto()).isEqualByComparingTo("90.00");

        assertThat(hitos.get(1).getTipo().name()).isEqualTo("ENTREGA");
        assertThat(hitos.get(1).isRequiereEntrega()).isTrue();
    }

    @Test
    void senaSkeb_sinEntregable() {
        HitoPresupuestoRequest sena = hito("50");
        sena.setRequiereEntrega(false);

        List<HitoPresupuesto> hitos = construir(
            List.of(sena, hito("50")),
            "200.00", "200.00", "20.00", "20.00");

        assertThat(hitos).hasSize(2);
        assertThat(hitos.get(0).getTipo().name()).isEqualTo("SENA");
        assertThat(hitos.get(0).isRequiereEntrega()).isFalse();
        assertThat(hitos.get(1).getTipo().name()).isEqualTo("ENTREGA");
        assertThat(hitos.get(1).isRequiereEntrega()).isTrue();
    }

    @Test
    void pagoUnico_fuerzaEntregableAunqueSeSoliciteFalse() {
        HitoPresupuestoRequest unico = hito("100");
        unico.setRequiereEntrega(false);

        List<HitoPresupuesto> hitos = construir(
            List.of(unico), "100.00", "100.00", "10.00", "10.00");

        assertThat(hitos).hasSize(1);
        assertThat(hitos.get(0).getTipo().name()).isEqualTo("SENA");
        assertThat(hitos.get(0).isRequiereEntrega()).isTrue();
    }

    @Test
    void tresHitos_entregaSiempreTieneEntregable() {
        HitoPresupuestoRequest cuota = hito("40");
        cuota.setRequiereEntrega(false);
        HitoPresupuestoRequest entrega = hito("30");
        entrega.setRequiereEntrega(false);

        List<HitoPresupuesto> hitos = construir(
            List.of(hito("30"), cuota, entrega),
            "100.00", "100.00", "10.00", "10.00");

        assertThat(hitos).hasSize(3);
        assertThat(hitos.get(1).getTipo().name()).isEqualTo("CUOTA");
        assertThat(hitos.get(1).isRequiereEntrega()).isTrue();
        assertThat(hitos.get(2).getTipo().name()).isEqualTo("ENTREGA");
        assertThat(hitos.get(2).isRequiereEntrega()).isTrue();
    }

    @Test
    void tresHitos_elUltimoAbsorbeElRedondeo() {
        List<HitoPresupuesto> hitos = construir(
            List.of(hito("33.33"), hito("33.33"), hito("33.34")),
            "100.00", "100.00", "10.00", "10.00");

        assertThat(hitos).hasSize(3);
        assertThat(hitos.get(0).getMonto()).isEqualByComparingTo("33.33");
        assertThat(hitos.get(1).getMonto()).isEqualByComparingTo("33.33");
        assertThat(hitos.get(2).getMonto()).isEqualByComparingTo("33.34");

        BigDecimal sumaMontos = hitos.stream()
            .map(HitoPresupuesto::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumaComisiones = hitos.stream()
            .map(HitoPresupuesto::getComisionMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(sumaMontos).isEqualByComparingTo("100.00");
        assertThat(sumaComisiones).isEqualByComparingTo("10.00");
        assertThat(hitos.get(2).getMontoNeto()).isEqualByComparingTo("30.00");
    }

    @Test
    void porcentajesNoSumanCien_lanzaError() {
        assertThatThrownBy(() -> construir(List.of(hito("50"), hito("40")),
            "100.00", "100.00", "10.00", "10.00"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("100%");
    }

    @Test
    void primeraCuotaSuperaElMaximo_lanzaError() {
        assertThatThrownBy(() -> construir(List.of(hito("60"), hito("40")),
            "100.00", "100.00", "10.00", "10.00"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("primera cuota");
    }

    @Test
    void cuotaMenorAlMinimo_lanzaError() {
        HitoCalculadora.Limites limites = new HitoCalculadora.Limites(
            4, new BigDecimal("50.00"), new BigDecimal("100.00"));

        assertThatThrownBy(() -> construir(List.of(hito("50"), hito("50")),
            "100.00", "100.00", "10.00", "10.00", limites))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("al menos");
    }

    @Test
    void unicoHito_noAplicaElMinimo() {
        HitoCalculadora.Limites limites = new HitoCalculadora.Limites(
            4, new BigDecimal("50.00"), new BigDecimal("5000.00"));

        List<HitoPresupuesto> hitos = construir(List.of(hito("100")),
            "100.00", "100.00", "10.00", "10.00", limites);

        assertThat(hitos).hasSize(1);
        assertThat(hitos.get(0).getMonto()).isEqualByComparingTo("100.00");
    }

    @Test
    void superaElMaximoDeHitos_lanzaError() {
        assertThatThrownBy(() -> construir(
            List.of(hito("25"), hito("25"), hito("25"), hito("20"), hito("5")),
            "100.00", "100.00", "10.00", "10.00"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("máximo 4");
    }

    @Test
    void usaElNombreIndicadoYGeneraNombresDeCuota() {
        HitoPresupuestoRequest personalizado = hito("30");
        personalizado.setNombre("  Boceto  ");

        List<HitoPresupuesto> hitos = construir(
            List.of(personalizado, hito("40"), hito("30")),
            "100.00", "100.00", "10.00", "10.00");

        assertThat(hitos.get(0).getNombre()).isEqualTo("Boceto");
        assertThat(hitos.get(1).getNombre()).isEqualTo("Cuota 2");
        assertThat(hitos.get(2).getNombre()).isEqualTo("Entrega final");
    }

    private List<HitoPresupuesto> construir(
            List<HitoPresupuestoRequest> requests,
            String precioFinal,
            String precioFinalCobro,
            String comision,
            String comisionCobro) {
        return construir(requests, precioFinal, precioFinalCobro,
            comision, comisionCobro, LIMITES);
    }

    private List<HitoPresupuesto> construir(
            List<HitoPresupuestoRequest> requests,
            String precioFinal,
            String precioFinalCobro,
            String comision,
            String comisionCobro,
            HitoCalculadora.Limites limites) {
        return HitoCalculadora.construir(
            requests,
            new BigDecimal(precioFinal),
            new BigDecimal(precioFinalCobro),
            new BigDecimal(comision),
            new BigDecimal(comisionCobro),
            limites);
    }

    private HitoPresupuestoRequest hito(String porcentaje) {
        HitoPresupuestoRequest hito = new HitoPresupuestoRequest();
        hito.setPorcentaje(new BigDecimal(porcentaje));
        return hito;
    }
}
