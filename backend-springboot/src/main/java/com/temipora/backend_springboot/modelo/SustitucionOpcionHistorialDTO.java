package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

/**
 * Alternativa tal como se mostró en el momento del evento, con su precio
 * congelado y si fue la elegida por el cliente.
 */
public record SustitucionOpcionHistorialDTO(
        Long idSustitucionOpcion,
        int orden,
        Long idServicio,
        String servicio,
        Long idArtista,
        String artista,
        BigDecimal score,
        String motivo,
        BigDecimal precioMomento,
        String moneda,
        boolean elegida) {
}
