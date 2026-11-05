package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.Pago;
import com.temipora.backend_springboot.servicios.pagos.DatosPagoProveedor;

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

    static DatosPagoProveedor datos(MercadoPagoPago pago) {
        if (pago == null) {
            return null;
        }
        return new DatosPagoProveedor(
            arancel(pago),
            pago.transactionDetails() != null
                ? pago.transactionDetails().netReceivedAmount()
                : null,
            fecha(pago.dateApproved()),
            fecha(pago.moneyReleaseDate()),
            pago.paymentMethodId(),
            pago.paymentTypeId(),
            pago.installments(),
            pago.card() != null ? pago.card().lastFourDigits() : null);
    }

    static BigDecimal arancel(MercadoPagoPago pago) {
        List<MercadoPagoPago.FeeDetail> fees = pago.feeDetails();
        if (fees == null || fees.isEmpty()) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        boolean encontrado = false;
        for (MercadoPagoPago.FeeDetail fee : fees) {
            if (fee == null || fee.amount() == null) {
                continue;
            }
            if (fee.feePayer() != null
                    && !"collector".equalsIgnoreCase(fee.feePayer())) {
                continue;
            }
            total = total.add(fee.amount());
            encontrado = true;
        }
        return encontrado ? total : null;
    }

    static LocalDateTime fecha(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(iso)
                .atZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
        } catch (DateTimeParseException e) {
            return null;
        }
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
