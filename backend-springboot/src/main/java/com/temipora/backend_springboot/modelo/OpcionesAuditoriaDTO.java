package com.temipora.backend_springboot.modelo;

import java.util.List;

public record OpcionesAuditoriaDTO(
        List<String> entidades,
        List<String> acciones,
        List<CategoriaAuditoriaDTO> categorias) {
}
