package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

/**
 * Una porción del puntaje de similitud, ya expresada en porcentaje para el
 * panel de administración.
 */
public record PesoPuntajeDTO(
        String clave,
        String etiqueta,
        BigDecimal porcentaje) {
}
