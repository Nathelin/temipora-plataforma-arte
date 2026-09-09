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
public class TipoServicioDTO {

    private Long idTipoServicio;
    private String nombre;
    private String descripcion;
    private String observaciones;
    private boolean activo;

    public static TipoServicioDTO desdeTipoServicio(TipoServicio tipo) {
        return TipoServicioDTO.builder()
            .idTipoServicio(tipo.getIdTipoServicio())
            .nombre(tipo.getNombre())
            .descripcion(tipo.getDescripcion())
            .observaciones(tipo.getObservaciones())
            .activo(tipo.isActivo())
            .build();
    }
}