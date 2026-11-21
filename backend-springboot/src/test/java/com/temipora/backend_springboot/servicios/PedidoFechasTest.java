package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.HitoPresupuesto;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.TipoHito;

class PedidoFechasTest {

    private static final LocalDate PROMETIDA = LocalDate.of(2026, 12, 12);

    @Test
    void pedidoSinIniciar_noTieneEntregaPrevista() {
        Pedido pedido = pedido(null, EstadoPedido.PENDIENTE_PAGO);

        assertThat(PedidoFechas.entregaPrevista(pedido)).isNull();
        assertThat(PedidoFechas.diasDemoraCliente(pedido)).isZero();
    }

    @Test
    void pagoEnFecha_noExtiendeLaEntrega() {
        Pedido pedido = pedido(PROMETIDA, EstadoPedido.EN_CURSO);
        hito(pedido, TipoHito.CUOTA,
            LocalDateTime.of(2026, 11, 20, 10, 0),
            LocalDateTime.of(2026, 11, 20, 18, 0));

        assertThat(PedidoFechas.diasDemoraCliente(pedido)).isZero();
        assertThat(PedidoFechas.entregaPrevista(pedido)).isEqualTo(PROMETIDA);
    }

    @Test
    void pagoAntesDeLaFecha_noExtiendeLaEntrega() {
        Pedido pedido = pedido(PROMETIDA, EstadoPedido.EN_CURSO);
        hito(pedido, TipoHito.CUOTA,
            LocalDateTime.of(2026, 11, 20, 10, 0),
            LocalDateTime.of(2026, 11, 15, 9, 0));

        assertThat(PedidoFechas.diasDemoraCliente(pedido)).isZero();
    }

    @Test
    void pagoTardioDeCuota_extiendeLosDiasDeDemora() {
        Pedido pedido = pedido(PROMETIDA, EstadoPedido.EN_CURSO);
        hito(pedido, TipoHito.CUOTA,
            LocalDateTime.of(2026, 11, 20, 10, 0),
            LocalDateTime.of(2026, 11, 26, 18, 0));

        assertThat(PedidoFechas.diasDemoraCliente(pedido)).isEqualTo(6);
        assertThat(PedidoFechas.entregaPrevista(pedido))
            .isEqualTo(PROMETIDA.plusDays(6));
    }

    @Test
    void demoraDeSenaODeEntregaFinal_noExtiendeLaEntrega() {
        Pedido pedido = pedido(PROMETIDA, EstadoPedido.EN_CURSO);
        hito(pedido, TipoHito.SENA,
            LocalDateTime.of(2026, 11, 10, 10, 0),
            LocalDateTime.of(2026, 11, 20, 10, 0));
        hito(pedido, TipoHito.ENTREGA,
            LocalDateTime.of(2026, 12, 20, 10, 0),
            LocalDateTime.of(2026, 12, 30, 10, 0));

        assertThat(PedidoFechas.diasDemoraCliente(pedido)).isZero();
        assertThat(PedidoFechas.entregaPrevista(pedido)).isEqualTo(PROMETIDA);
    }

    @Test
    void cuotaImpagaNoCuentaComoDemora() {
        Pedido pedido = pedido(PROMETIDA, EstadoPedido.EN_CURSO);
        hito(pedido, TipoHito.CUOTA,
            LocalDateTime.of(2026, 11, 20, 10, 0),
            null);

        assertThat(PedidoFechas.diasDemoraCliente(pedido)).isZero();
    }

    @Test
    void ventanaPago_sinEntregaPrevista_usaElPlazoConfigurado() {
        Pedido pedido = pedido(null, EstadoPedido.PENDIENTE_PAGO);
        LocalDateTime ahora = LocalDateTime.of(2026, 12, 5, 10, 0);

        assertThat(PedidoFechas.ventanaPago(pedido, ahora, 7))
            .isEqualTo(ahora.plusDays(7));
    }

    @Test
    void ventanaPago_noSuperaLaEntregaPrevista() {
        Pedido pedido = pedido(PROMETIDA, EstadoPedido.EN_CURSO);
        LocalDateTime ahora = LocalDateTime.of(2026, 12, 10, 10, 0);

        assertThat(PedidoFechas.ventanaPago(pedido, ahora, 7))
            .isEqualTo(PROMETIDA.atTime(23, 59, 59));
    }

    @Test
    void ventanaPago_dejaAlMenos24Horas() {
        Pedido pedido = pedido(PROMETIDA, EstadoPedido.EN_CURSO);
        LocalDateTime ahora = LocalDateTime.of(2026, 12, 12, 20, 0);

        assertThat(PedidoFechas.ventanaPago(pedido, ahora, 7))
            .isEqualTo(ahora.plusHours(24));
    }

    @Test
    void entregaAtrasada_soloEnPedidoAbierto() {
        Pedido enCurso = pedido(PROMETIDA, EstadoPedido.EN_CURSO);
        Pedido finalizado = pedido(PROMETIDA, EstadoPedido.FINALIZADO);

        assertThat(PedidoFechas.entregaAtrasada(
            enCurso, PROMETIDA.plusDays(1))).isTrue();
        assertThat(PedidoFechas.entregaAtrasada(
            finalizado, PROMETIDA.plusDays(1))).isFalse();
        assertThat(PedidoFechas.entregaAtrasada(
            enCurso, PROMETIDA)).isFalse();
    }

    @Test
    void escenarioSimulado_cuotaPagadaSeisDiasTarde() {
        // Pedido prometido para el 12/12 y una cuota que vencía el 20/11.
        Pedido pedido = pedido(PROMETIDA, EstadoPedido.EN_CURSO);
        hito(pedido, TipoHito.CUOTA,
            LocalDateTime.of(2026, 11, 20, 23, 59),
            LocalDateTime.of(2026, 11, 26, 18, 0));

        // 1) La entrega se corre exactamente los días de demora.
        assertThat(PedidoFechas.diasDemoraCliente(pedido)).isEqualTo(6);
        assertThat(PedidoFechas.entregaPrevista(pedido))
            .isEqualTo(LocalDate.of(2026, 12, 18));

        // 2) La siguiente ventana de pago usa la fecha ya corrida como tope.
        assertThat(PedidoFechas.ventanaPago(
            pedido, LocalDateTime.of(2026, 12, 10, 10, 0), 14))
            .isEqualTo(LocalDate.of(2026, 12, 18).atTime(23, 59, 59));

        // 3) Sin la demora, el mismo cálculo se cortaría el 12/12.
        assertThat(PedidoFechas.ventanaPago(
            pedido(PROMETIDA, EstadoPedido.EN_CURSO),
            LocalDateTime.of(2026, 12, 10, 10, 0), 14))
            .isEqualTo(PROMETIDA.atTime(23, 59, 59));
    }

    @Test
    void escenarioSimulado_atrasoDelArtistaNoMueveLaFecha() {
        Pedido pedido = pedido(PROMETIDA, EstadoPedido.EN_CURSO);

        assertThat(PedidoFechas.diasDemoraCliente(pedido)).isZero();
        assertThat(PedidoFechas.entregaPrevista(pedido)).isEqualTo(PROMETIDA);
        assertThat(PedidoFechas.entregaAtrasada(
            pedido, PROMETIDA.plusDays(3))).isTrue();
        assertThat(PedidoFechas.entregaPrevista(pedido)).isEqualTo(PROMETIDA);
    }

    @Test
    void escenarioSimulado_senaImpagaNoExtiendeNiDejaSinPlazo() {
        Pedido pedido = pedido(null, EstadoPedido.PENDIENTE_PAGO);
        hito(pedido, TipoHito.SENA,
            LocalDateTime.of(2026, 11, 27, 10, 0),
            null);
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 20, 10, 0);

        assertThat(PedidoFechas.entregaPrevista(pedido)).isNull();
        assertThat(PedidoFechas.diasDemoraCliente(pedido)).isZero();
        assertThat(PedidoFechas.ventanaPago(pedido, ahora, 7))
            .isEqualTo(ahora.plusDays(7));
    }

    private Pedido pedido(LocalDate limiteEntrega, EstadoPedido estado) {
        Pedido pedido = new Pedido();
        pedido.setEstado(estado);
        pedido.setLimiteEntrega(limiteEntrega);
        return pedido;
    }

    private HitoPedido hito(
            Pedido pedido,
            TipoHito tipo,
            LocalDateTime fechaLimitePago,
            LocalDateTime fechaPago) {
        HitoPresupuesto presupuesto = new HitoPresupuesto();
        presupuesto.setTipo(tipo);

        HitoPedido hito = new HitoPedido();
        hito.setHitoPresupuesto(presupuesto);
        hito.setFechaLimitePago(fechaLimitePago);
        hito.setFechaPago(fechaPago);
        hito.setEstadoPago(fechaPago != null
            ? EstadoPagoHito.PAGADO
            : EstadoPagoHito.PENDIENTE);
        pedido.agregarHito(hito);
        return hito;
    }
}
