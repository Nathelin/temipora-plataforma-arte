package com.temipora.backend_springboot.modelo;

public record CambioAuditoriaDTO(
        String campo,
        String etiqueta,
        String anterior,
        String nuevo) {
}
