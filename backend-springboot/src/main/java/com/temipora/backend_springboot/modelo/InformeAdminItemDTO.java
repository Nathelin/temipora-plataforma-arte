package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

public record InformeAdminItemDTO(
        String tipo,
        Long id,
        String nombre,
        long operaciones,
        long unidades,
        BigDecimal volumen,
        BigDecimal comision,
        BigDecimal netoArtistas,
        BigDecimal arancel) {
}
