package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

/**
 * Una línea del informe: un producto o un servicio vendido en el período.
 * {@code participacion} es el porcentaje que representa su neto sobre el neto
 * total del informe.
 */
public record InformeItemDTO(
        String tipo,
        Long id,
        String nombre,
        long unidades,
        long operaciones,
        BigDecimal bruto,
        BigDecimal comision,
        BigDecimal arancel,
        BigDecimal netoPercibido,
        BigDecimal participacion) {
}
