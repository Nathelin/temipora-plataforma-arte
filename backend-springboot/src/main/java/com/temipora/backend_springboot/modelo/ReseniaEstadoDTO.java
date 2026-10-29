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
public class ReseniaEstadoDTO {

    private AmbitoResenia direccion;
    private ReseniaDTO miResenia;
    private ReseniaDTO reseniaRecibida;
    private boolean puedeCrear;
    private boolean puedeEditar;
    private boolean puedeEliminar;
    private String motivoNoDisponible;
    private LocalDateTime fechaLimiteCreacion;
    private LocalDateTime fechaLimiteEdicion;
}
