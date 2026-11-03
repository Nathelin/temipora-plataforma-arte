package com.temipora.backend_springboot.modelo;

public record ResumenAuditoriaDTO(
        long total,
        long criticas,
        long loginFallido) {
}
