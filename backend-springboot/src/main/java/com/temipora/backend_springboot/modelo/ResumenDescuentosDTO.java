package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

/**
 * Totales de los pedidos con descuento del período, en la moneda de cobro.
 */
public record ResumenDescuentosDTO(
        long pedidos,
        long pedidosConComisionCero,
        long totalPedidosPeriodo,
        BigDecimal porcentajePedidos,
        BigDecimal descuentoTotal,
        BigDecimal descuentoConComisionCero,
        BigDecimal volumenCliente,
        BigDecimal volumenSinDescuento,
        BigDecimal comisionEfectiva,
        BigDecimal comisionNominal,
        BigDecimal ticketPromedio) {
}
