package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

public record HistorialResumenDTO(
        long operaciones,
        long pedidos,
        long compras,
        long completadas,
        long enCurso,
        long pendientes,
        long canceladas,
        BigDecimal importeTotal,
        BigDecimal importeCompletado,
        BigDecimal descuentos,
        String moneda) {
}
