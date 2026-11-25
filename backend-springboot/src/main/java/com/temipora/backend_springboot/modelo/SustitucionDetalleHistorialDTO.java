package com.temipora.backend_springboot.modelo;

import java.util.List;

public record SustitucionDetalleHistorialDTO(
        SustitucionHistorialDTO evento,
        List<SustitucionOpcionHistorialDTO> opciones) {
}
