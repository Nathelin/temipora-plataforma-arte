package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.temipora.backend_springboot.servicios.pagos.DatosPagoProveedor;

class MercadoPagoEstadosTest {

    @Test
    void datos_sumaSoloLosFeesDelCobrador() {
        MercadoPagoPago pago = new MercadoPagoPago(
            1L, "approved", "accredited",
            new BigDecimal("50.00"), "ARS", "30", "visa",
            "credit_card", "2026-11-02T10:00:00.000-03:00",
            "2026-11-05T10:00:00.000-03:00", "released",
            new MercadoPagoPago.TransactionDetails(
                new BigDecimal("45.50"), new BigDecimal("50.00")),
            List.of(
                new MercadoPagoPago.FeeDetail(
                    "mercadopago_fee", new BigDecimal("4.50"), "collector"),
                new MercadoPagoPago.FeeDetail(
                    "financing_fee", new BigDecimal("1.00"), "payer")),
            3, new MercadoPagoPago.Card("1234"));

        DatosPagoProveedor datos = MercadoPagoEstados.datos(pago);

        assertThat(datos.arancel()).isEqualByComparingTo("4.50");
        assertThat(datos.netoRecibido()).isEqualByComparingTo("45.50");
        assertThat(datos.fechaAprobacion()).isNotNull();
        assertThat(datos.fechaLiberacion()).isNotNull();
        assertThat(datos.medioPago()).isEqualTo("visa");
        assertThat(datos.tipoMedioPago()).isEqualTo("credit_card");
        assertThat(datos.cuotas()).isEqualTo(3);
        assertThat(datos.ultimosCuatro()).isEqualTo("1234");
    }

    @Test
    void datos_sinDatosDevuelveNulls() {
        MercadoPagoPago pago = new MercadoPagoPago(
            1L, "pending", null,
            new BigDecimal("50.00"), "ARS", "30", null,
            null, null, null, null, null, null, null, null);

        DatosPagoProveedor datos = MercadoPagoEstados.datos(pago);

        assertThat(datos.arancel()).isNull();
        assertThat(datos.netoRecibido()).isNull();
        assertThat(datos.fechaAprobacion()).isNull();
        assertThat(datos.fechaLiberacion()).isNull();
        assertThat(datos.medioPago()).isNull();
        assertThat(datos.tipoMedioPago()).isNull();
        assertThat(datos.cuotas()).isNull();
        assertThat(datos.ultimosCuatro()).isNull();
    }

    @Test
    void fecha_parseaIsoYDescartaValoresInvalidos() {
        assertThat(MercadoPagoEstados.fecha(
            "2026-11-02T10:00:00.000-03:00")).isNotNull();
        assertThat(MercadoPagoEstados.fecha("no-es-fecha")).isNull();
        assertThat(MercadoPagoEstados.fecha(null)).isNull();
        assertThat(MercadoPagoEstados.fecha("  ")).isNull();
    }
}
