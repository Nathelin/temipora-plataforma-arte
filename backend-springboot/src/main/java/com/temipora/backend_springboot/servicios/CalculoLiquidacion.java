package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;

/**
 * Calcula el importe que efectivamente recibe el vendedor.
 *
 * <p>MercadoPago informa {@code transaction_details.net_received_amount} sin
 * el "cargo por uso de plataforma de terceros" (la comisión de Temipora), que
 * liquida por separado. Por eso el neto informado por el proveedor queda por
 * encima de lo que recibe el artista: el importe real es el neto del vendedor
 * menos el fee propio de MercadoPago, que es el arancel informado menos la
 * comisión.</p>
 */
public final class CalculoLiquidacion {

    private CalculoLiquidacion() {
    }

    public static BigDecimal netoRecibido(
            BigDecimal bruto, BigDecimal comision, BigDecimal arancel) {
        BigDecimal cargoMercadoPago = nulo(arancel).subtract(nulo(comision));
        if (cargoMercadoPago.signum() < 0) {
            cargoMercadoPago = BigDecimal.ZERO;
        }
        return nulo(bruto).subtract(nulo(comision)).subtract(cargoMercadoPago);
    }

    private static BigDecimal nulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
