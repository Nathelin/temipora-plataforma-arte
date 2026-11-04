package com.temipora.backend_springboot.modelo;

public record ResumenComunidadDTO(
        long total,
        long artistas,
        long clientes,
        long administradores,
        long nuevosSemana) {
}
