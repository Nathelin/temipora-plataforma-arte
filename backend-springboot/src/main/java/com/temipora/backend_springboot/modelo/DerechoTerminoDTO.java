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
public class DerechoTerminoDTO {

    private Long idDerechoTermino;
    private String codigo;
    private String nombre;
    private String descripcion;
    private boolean requierePiezaUnica;
    private Integer orden;

    public static DerechoTerminoDTO desdeDerecho(DerechoTermino derecho) {
        return DerechoTerminoDTO.builder()
            .idDerechoTermino(derecho.getIdDerechoTermino())
            .codigo(derecho.getCodigo())
            .nombre(derecho.getNombre())
            .descripcion(derecho.getDescripcion())
            .requierePiezaUnica(derecho.isRequierePiezaUnica())
            .orden(derecho.getOrden())
            .build();
    }
}
