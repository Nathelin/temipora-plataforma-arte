package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

/**
 * Contexto agregado de las ofertas generadas en el período. {@code aceptadas}
 * incluye las que después se usaron; {@code utilizadas} es el subconjunto que
 * llegó a un presupuesto. Se informa aparte de los pedidos para no contar dos
 * veces la misma promoción.
 */
public record OfertasReactivacionResumenDTO(
        long campanas,
        long ofertas,
        long ofrecidas,
        long aceptadas,
        long utilizadas,
        long vencidas,
        long canceladas,
        BigDecimal tasaUtilizacion) {
}
