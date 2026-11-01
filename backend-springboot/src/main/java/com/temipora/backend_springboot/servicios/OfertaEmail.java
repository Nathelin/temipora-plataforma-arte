package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;

public record OfertaEmail(
        Long idOferta,
        String nombreServicio,
        String nombreArtista,
        String imagen,
        String precioDesde,
        String moneda,
        BigDecimal porcentajeDescuento,
        String motivo,
        String url) {
}
