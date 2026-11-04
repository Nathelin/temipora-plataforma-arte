package com.temipora.backend_springboot.modelo;

public record ResumenOperacionDTO(
        long entregasAtrasadas,
        long aprobacionesPendientes) {
}
