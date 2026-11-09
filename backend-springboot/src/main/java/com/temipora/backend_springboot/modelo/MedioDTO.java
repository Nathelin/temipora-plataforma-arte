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
public class MedioDTO {

    private Long idMedio;
    private String nombre;
    private String descripcion;
    private String observaciones;
    private TipoEntrega tipoEntrega;
    private boolean activo;

    public static MedioDTO desdeMedio(Medio medio) {
        return MedioDTO.builder()
            .idMedio(medio.getIdMedio())
            .nombre(medio.getNombre())
            .descripcion(medio.getDescripcion())
            .observaciones(medio.getObservaciones())
            .tipoEntrega(medio.getTipoEntrega())
            .activo(medio.isActivo())
            .build();
    }
}
