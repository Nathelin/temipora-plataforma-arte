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
public class TipoProductoDTO {

    private Long idTipoProducto;
    private String nombre;
    private String descripcion;
    private String observaciones;
    private boolean activo;

    public static TipoProductoDTO desdeTipoProducto(TipoProducto tipo) {
        return TipoProductoDTO.builder()
            .idTipoProducto(tipo.getIdTipoProducto())
            .nombre(tipo.getNombre())
            .descripcion(tipo.getDescripcion())
            .observaciones(tipo.getObservaciones())
            .activo(tipo.isActivo())
            .build();
    }
}