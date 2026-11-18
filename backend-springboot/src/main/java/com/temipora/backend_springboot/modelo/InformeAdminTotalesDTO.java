package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

/**
 * Totales del informe de la plataforma, en ARS.
 *
 * <p>{@code comision} es el ingreso de Temipora por el uso de la plataforma.
 * {@code netoPercibido} es lo que el medio de pago liquidó realmente a los
 * vendedores; cuando falta ese dato se usa el neto del artista.</p>
 */
public record InformeAdminTotalesDTO(
        BigDecimal volumen,
        BigDecimal comision,
        BigDecimal netoArtistas,
        BigDecimal arancel,
        BigDecimal netoPercibido,
        long operaciones,
        long unidades,
        BigDecimal ticketPromedio) {
}
