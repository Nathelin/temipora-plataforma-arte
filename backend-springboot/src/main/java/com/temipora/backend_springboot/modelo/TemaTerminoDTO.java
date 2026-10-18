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
public class TemaTerminoDTO {

    private Long idTemaTermino;
    private String nombre;
    private String descripcion;
    private Integer orden;

    public static TemaTerminoDTO desdeTema(TemaTermino tema) {
        return TemaTerminoDTO.builder()
            .idTemaTermino(tema.getIdTemaTermino())
            .nombre(tema.getNombre())
            .descripcion(tema.getDescripcion())
            .orden(tema.getOrden())
            .build();
    }
}
