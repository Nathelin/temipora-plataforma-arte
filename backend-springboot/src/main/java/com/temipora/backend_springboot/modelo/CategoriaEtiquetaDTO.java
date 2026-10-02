package com.temipora.backend_springboot.modelo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaEtiquetaDTO {

    private Long idCategoriaEtiqueta;
    private String nombre;
    private String descripcion;
    private String observaciones;
    private boolean activo;

    public static CategoriaEtiquetaDTO desdeCategoria(CategoriaEtiqueta categoria) {
        return CategoriaEtiquetaDTO.builder()
            .idCategoriaEtiqueta(categoria.getIdCategoriaEtiqueta())
            .nombre(categoria.getNombre())
            .descripcion(categoria.getDescripcion())
            .observaciones(categoria.getObservaciones())
            .activo(categoria.isActivo())
            .build();
    }
}
