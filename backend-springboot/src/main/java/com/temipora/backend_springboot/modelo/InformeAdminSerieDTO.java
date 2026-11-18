package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

public record InformeAdminSerieDTO(
        String periodo,
        BigDecimal volumen,
        BigDecimal comision,
        long cobros) {
}
