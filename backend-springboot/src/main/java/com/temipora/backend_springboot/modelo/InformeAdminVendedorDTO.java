package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

/**
 * Ventas agrupadas por vendedor. En servicios es el artista del pedido; en
 * productos es el vendedor del grupo, que en una reventa puede no ser el
 * artista original.
 */
public record InformeAdminVendedorDTO(
        Long idUsuario,
        String nombre,
        long operaciones,
        long unidades,
        BigDecimal volumen,
        BigDecimal comision,
        BigDecimal netoArtistas,
        BigDecimal arancel) {
}
