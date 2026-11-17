package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

/**
 * Totales del informe, expresados en la moneda de cobro (ARS).
 *
 * <p>{@code netoPercibido} es el importe que el medio de pago liquidó
 * realmente (net_received_amount de MercadoPago). Cuando ese dato no existe,
 * se estima con el neto del artista y la operación se cuenta en
 * {@code operacionesSinNetoReal} del informe.</p>
 */
public record InformeTotalesDTO(
        BigDecimal bruto,
        BigDecimal netoPercibido,
        BigDecimal costoCobro,
        BigDecimal comision,
        BigDecimal arancel,
        long operaciones,
        long unidades,
        BigDecimal ticketPromedio) {
}
