package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.util.List;

public record ResumenSustitucionesHistorialDTO(
        long total,
        long resueltasConAlternativa,
        long resueltasConCola,
        long alternativaElegida,
        long descartadas,
        long abiertas,
        long expiradas,
        long anonimas,
        BigDecimal tasaSustitucion,
        BigDecimal opcionesPromedio,
        List<SustitucionConteoDTO> motivos) {
}
