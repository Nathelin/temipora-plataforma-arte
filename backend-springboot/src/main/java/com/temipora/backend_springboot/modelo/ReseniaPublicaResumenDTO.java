package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReseniaPublicaResumenDTO {

    private Long idResenia;
    private int calificacionGeneral;
    private String descripcion;
    private LocalDateTime fechaVersion;
    private String autorMostrado;
    private boolean anonima;
}
