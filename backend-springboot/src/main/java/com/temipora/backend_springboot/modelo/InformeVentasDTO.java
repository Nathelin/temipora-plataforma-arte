package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
import java.util.List;

public record InformeVentasDTO(
        InformePeriodoDTO periodo,
        String moneda,
        String tipo,
        LocalDateTime generadoEn,
        InformeTotalesDTO totales,
        InformeComparacionDTO comparacion,
        long operacionesSinNetoReal,
        List<InformeSerieDTO> serie,
        List<InformeItemDTO> items) {
}
