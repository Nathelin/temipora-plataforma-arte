package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoProrroga;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.HitoPresupuesto;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.ProrrogaPedido;
import com.temipora.backend_springboot.modelo.TipoHito;

class EntregaPendienteServiceTest {

    private static final LocalDate LIMITE = LocalDate.of(2026, 1, 10);

    private final EntregaPendienteService service =
        new EntregaPendienteService();

    @Test
    void cuotaPendienteDePago_noGeneraMora() {
        Pedido pedido = pedido();
        pedido.agregarHito(hito(1L, 1, TipoHito.CUOTA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.NO_INICIADO));

        assertThat(service.plazoVigente(pedido)).isNull();
        assertThat(service.estaEnMora(pedido, despuesDelLimite())).isFalse();
        assertThat(service.artistaDebeEntregar(pedido)).isFalse();
    }

    @Test
    void cuotaPagadaSinEntregar_generaMoraDesdeLaEntregaPrevista() {
        Pedido pedido = pedido();
        pedido.agregarHito(hito(1L, 1, TipoHito.CUOTA,
            EstadoPagoHito.PAGADO, EstadoEntregaHito.NO_INICIADO));

        assertThat(service.plazoVigente(pedido))
            .isEqualTo(LIMITE.atTime(23, 59, 59));
        assertThat(service.estaEnMora(pedido, despuesDelLimite())).isTrue();
        assertThat(service.artistaDebeEntregar(pedido)).isTrue();
    }

    @Test
    void entregadoSinAprobar_sigueEnMoraPeroNoSeRecuerda() {
        Pedido pedido = pedido();
        pedido.agregarHito(hito(1L, 1, TipoHito.CUOTA,
            EstadoPagoHito.PAGADO, EstadoEntregaHito.ENTREGADO));

        assertThat(service.esExigibleAlArtista(pedido, pedido.getHitos().get(0)))
            .isTrue();
        assertThat(service.estaEnMora(pedido, despuesDelLimite())).isTrue();
        assertThat(service.artistaDebeEntregar(pedido)).isFalse();
    }

    @Test
    void aprobado_noGeneraMora() {
        Pedido pedido = pedido();
        pedido.agregarHito(hito(1L, 1, TipoHito.CUOTA,
            EstadoPagoHito.PAGADO, EstadoEntregaHito.APROBADO));

        assertThat(service.plazoVigente(pedido)).isNull();
        assertThat(service.estaEnMora(pedido, despuesDelLimite())).isFalse();
    }

    @Test
    void conRevision_mandaElPlazoDeReentrega() {
        Pedido pedido = pedido();
        HitoPedido hito = hito(1L, 1, TipoHito.CUOTA,
            EstadoPagoHito.PAGADO, EstadoEntregaHito.CON_REVISION);
        LocalDateTime reentrega = LIMITE.plusDays(10).atTime(10, 0);
        hito.setFechaLimiteReentrega(reentrega);
        pedido.agregarHito(hito);

        assertThat(service.plazoVigente(pedido)).isEqualTo(reentrega);
        assertThat(service.estaEnMora(
            pedido, LIMITE.plusDays(5).atTime(12, 0))).isFalse();
        assertThat(service.estaEnMora(
            pedido, reentrega.plusMinutes(1))).isTrue();
    }

    @Test
    void entregaFinal_noEsExigibleHastaAprobarLaEtapaAnterior() {
        Pedido pedido = pedido();
        pedido.agregarHito(hito(1L, 0, TipoHito.CUOTA,
            EstadoPagoHito.PAGADO, EstadoEntregaHito.ENTREGADO));
        pedido.agregarHito(hito(2L, 1, TipoHito.ENTREGA,
            EstadoPagoHito.PENDIENTE, EstadoEntregaHito.NO_INICIADO));

        assertThat(service.plazoVigente(pedido))
            .isEqualTo(LIMITE.atTime(23, 59, 59));

        pedido.getHitos().get(0).setEstadoEntrega(EstadoEntregaHito.APROBADO);

        assertThat(service.esExigibleAlArtista(pedido, pedido.getHitos().get(1)))
            .isTrue();
    }

    @Test
    void tomaElPlazoMasAntiguoEntreLosExigibles() {
        Pedido pedido = pedido();
        pedido.agregarHito(hito(1L, 1, TipoHito.CUOTA,
            EstadoPagoHito.PAGADO, EstadoEntregaHito.NO_INICIADO));
        HitoPedido segundo = hito(2L, 2, TipoHito.CUOTA,
            EstadoPagoHito.PAGADO, EstadoEntregaHito.CON_REVISION);
        segundo.setFechaLimiteReentrega(LIMITE.minusDays(3).atTime(9, 0));
        pedido.agregarHito(segundo);

        assertThat(service.plazoVigente(pedido))
            .isEqualTo(LIMITE.minusDays(3).atTime(9, 0));
    }

    @Test
    void prorrogaPendiente_pausaLaMoraHastaLaRespuesta() {
        Pedido pedido = pedido();
        pedido.agregarHito(hito(1L, 1, TipoHito.CUOTA,
            EstadoPagoHito.PAGADO, EstadoEntregaHito.NO_INICIADO));
        LocalDateTime limiteRespuesta = LocalDateTime.now().plusHours(12);
        prorroga(pedido, EstadoProrroga.PENDIENTE, 5, limiteRespuesta);

        assertThat(service.plazoVigente(pedido)).isEqualTo(limiteRespuesta);
        assertThat(service.estaEnMora(pedido, despuesDelLimite())).isFalse();
        assertThat(service.plazoVigente(pedido, pedido.getHitos().get(0)))
            .isEqualTo(limiteRespuesta);
    }

    @Test
    void prorrogaVencida_noPausaLaMora() {
        Pedido pedido = pedido();
        pedido.agregarHito(hito(1L, 1, TipoHito.CUOTA,
            EstadoPagoHito.PAGADO, EstadoEntregaHito.NO_INICIADO));
        prorroga(pedido, EstadoProrroga.VENCIDA, 5,
            LocalDateTime.now().plusHours(12));

        assertThat(service.plazoVigente(pedido))
            .isEqualTo(LIMITE.atTime(23, 59, 59));
        assertThat(service.estaEnMora(pedido, despuesDelLimite())).isTrue();
    }

    @Test
    void prorrogaAceptada_correElPlazoVigente() {
        Pedido pedido = pedido();
        pedido.agregarHito(hito(1L, 1, TipoHito.CUOTA,
            EstadoPagoHito.PAGADO, EstadoEntregaHito.NO_INICIADO));
        prorroga(pedido, EstadoProrroga.ACEPTADA, 5,
            LocalDateTime.now().plusHours(12));

        assertThat(service.plazoVigente(pedido))
            .isEqualTo(LIMITE.plusDays(5).atTime(23, 59, 59));
        assertThat(service.estaEnMora(
            pedido, LIMITE.plusDays(4).atTime(12, 0))).isFalse();
    }

    private ProrrogaPedido prorroga(
            Pedido pedido,
            EstadoProrroga estado,
            int dias,
            LocalDateTime limiteRespuesta) {
        ProrrogaPedido prorroga = new ProrrogaPedido();
        prorroga.setEstado(estado);
        prorroga.setDiasSolicitados(dias);
        prorroga.setMotivo("Motivo de prueba");
        prorroga.setFechaLimiteRespuesta(limiteRespuesta);
        pedido.agregarProrroga(prorroga);
        return prorroga;
    }

    private Pedido pedido() {
        Pedido pedido = new Pedido();
        pedido.setEstado(EstadoPedido.EN_CURSO);
        pedido.setLimiteEntrega(LIMITE);
        return pedido;
    }

    private HitoPedido hito(
            Long id,
            int orden,
            TipoHito tipo,
            EstadoPagoHito pago,
            EstadoEntregaHito entrega) {
        HitoPresupuesto presupuesto = new HitoPresupuesto();
        presupuesto.setOrden(orden);
        presupuesto.setTipo(tipo);
        presupuesto.setNombre("Etapa " + orden);
        presupuesto.setRequiereEntrega(true);

        HitoPedido hito = new HitoPedido();
        hito.setIdHitoPedido(id);
        hito.setHitoPresupuesto(presupuesto);
        hito.setOrden(orden);
        hito.setEstadoPago(pago);
        hito.setEstadoEntrega(entrega);
        return hito;
    }

    private LocalDateTime despuesDelLimite() {
        return LIMITE.atTime(23, 59, 59).plusSeconds(1);
    }
}
