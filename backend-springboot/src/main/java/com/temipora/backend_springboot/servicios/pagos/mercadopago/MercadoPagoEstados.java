package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.Pago;

final class MercadoPagoEstados {

    private MercadoPagoEstados() {
    }

    static EstadoPago mapear(String estado) {
        if (estado == null) {
            return EstadoPago.PENDIENTE;
        }
        return switch (estado) {
            case "approved" -> EstadoPago.APROBADO;
            case "rejected" -> EstadoPago.RECHAZADO;
            case "cancelled" -> EstadoPago.CANCELADO;
            case "refunded", "charged_back" -> EstadoPago.REEMBOLSADO;
            default -> EstadoPago.PENDIENTE;
        };
    }

    static void verificarMontoYMoneda(MercadoPagoPago pagoMp, Pago pago) {
        if (pagoMp.transactionAmount() == null
                || pago.getMontoCobro() == null
                || pagoMp.transactionAmount().compareTo(pago.getMontoCobro()) != 0) {
            throw new MercadoPagoException(
                "El monto del pago " + pagoMp.id()
                    + " no coincide con el esperado.");
        }
        if (pagoMp.currencyId() != null
                && pago.getMonedaCobro() != null
                && !pagoMp.currencyId()
                    .equalsIgnoreCase(pago.getMonedaCobro().getCodigo())) {
            throw new MercadoPagoException(
                "La moneda del pago " + pagoMp.id() + " no coincide.");
        }
    }
}
