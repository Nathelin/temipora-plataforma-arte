package com.temipora.backend_springboot.modelo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemSolicitudRequest {

    @NotBlank(message = "El nombre del dibujo es obligatorio.")
    @Size(max = 100, message = "El nombre del dibujo no puede superar los 100 caracteres.")
    private String nombre;

    private Integer prioridad;

    @Size(max = 10, message = "Un dibujo puede tener como máximo 10 derechos.")
    private List<Long> derechos;

    @NotNull(message = "Las respuestas del formulario son obligatorias.")
    @Valid
    private List<RespuestaSolicitudRequest> respuestas;
}
