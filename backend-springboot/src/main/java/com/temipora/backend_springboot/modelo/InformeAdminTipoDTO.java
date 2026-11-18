package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

public record InformeAdminTipoDTO(
        String tipo,
        long operaciones,
        long unidades,
        BigDecimal volumen,
        BigDecimal comision,
        BigDecimal netoArtistas,
        BigDecimal arancel) {
}
