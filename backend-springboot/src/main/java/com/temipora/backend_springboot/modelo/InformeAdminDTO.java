package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
import java.util.List;

public record InformeAdminDTO(
        InformePeriodoDTO periodo,
        String moneda,
        String tipo,
        LocalDateTime generadoEn,
        InformeAdminTotalesDTO totales,
        InformeAdminComparacionDTO comparacion,
        long operacionesSinNetoReal,
        List<InformeAdminSerieDTO> serie,
        List<InformeAdminTipoDTO> tipos,
        List<InformeAdminVendedorDTO> vendedores,
        List<InformeAdminItemDTO> items) {
}
