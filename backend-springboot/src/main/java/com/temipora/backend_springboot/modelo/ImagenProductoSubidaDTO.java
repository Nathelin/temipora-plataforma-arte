package com.temipora.backend_springboot.modelo;

public record ImagenProductoSubidaDTO(
        String url,
        String claveOriginal,
        String sha256,
        String nombreOriginal,
        String mime,
        int ancho,
        int alto,
        long tamanoBytes) {
}
