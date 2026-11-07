package com.temipora.backend_springboot.modelo;

import java.util.Map;

public record ContadorNotificacionesDTO(
        long noLeidas,
        Map<String, Long> porCategoria) {
}
