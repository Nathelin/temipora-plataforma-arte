package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistorialItemDTO(
        String tipo,
        String tipoEtiqueta,
        Long id,
        LocalDateTime fecha,
        String titulo,
        String contraparte,
        BigDecimal monto,
        String moneda,
        String estado,
        String estadoEtiqueta,
        String estadoGrupo,
        BigDecimal descuentoPorcentaje,
        BigDecimal descuentoMonto,
        long cantidadItems,
        long cantidadGrupos) {
}
