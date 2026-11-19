package com.temipora.backend_springboot.modelo;

public record ConciliacionResultadoDTO(
        int revisados,
        int completados,
        int sinDatos,
        int errores) {
}
