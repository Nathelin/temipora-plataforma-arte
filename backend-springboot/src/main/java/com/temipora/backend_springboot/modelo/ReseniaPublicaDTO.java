package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
import java.util.List;

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
public class ReseniaPublicaDTO {

    private Long idResenia;
    private String nombreServicio;
    private int calificacionGeneral;
    private String descripcion;
    private LocalDateTime fechaVersion;
    private boolean editada;
    private UsuarioResumenDTO autor;
    private String autorMostrado;
    private boolean anonima;
    private List<ReseniaValorDTO> valores;
    private List<ReseniaOpcionDTO> opciones;
    private List<ReseniaImagenDTO> imagenes;
}
