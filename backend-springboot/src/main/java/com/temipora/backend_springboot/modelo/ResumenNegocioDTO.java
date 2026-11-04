package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

public record ResumenNegocioDTO(
        String periodo,
        String moneda,
        BigDecimal volumen,
        BigDecimal volumenAnterior,
        BigDecimal comision,
        BigDecimal comisionAnterior,
        long pedidosPagados,
        long pedidosPagadosAnterior,
        BigDecimal ticketPromedio,
        BigDecimal ticketPromedioAnterior) {
}
