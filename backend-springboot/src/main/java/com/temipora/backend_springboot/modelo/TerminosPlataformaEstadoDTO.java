package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

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
public class TerminosPlataformaEstadoDTO {

    private Integer version;
    private String titulo;
    private String contenido;
    private LocalDateTime fechaPublicacion;
    private boolean aceptada;
    private LocalDateTime fechaAceptacion;
}
