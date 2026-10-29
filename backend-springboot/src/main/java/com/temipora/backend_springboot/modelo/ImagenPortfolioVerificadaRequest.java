package com.temipora.backend_springboot.modelo;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImagenPortfolioVerificadaRequest {

    @NotNull(message = "La entrega final es obligatoria.")
    private Long idArchivoEntrega;

    @NotBlank(message = "El título es obligatorio.")
    @Size(max = 100, message = "El título no puede superar los 100 caracteres.")
    private String titulo;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres.")
    private String descripcion;

    @NotNull(message = "El tipo de arte es obligatorio.")
    private Long idTipoArte;

    private ContenidoSensible contenidoSensible;

    @Size(max = 5, message = "Una imagen puede tener como máximo 5 etiquetas.")
    private List<Long> idsEtiqueta;

    @Size(max = 5, message = "Una imagen puede tener como máximo 5 materiales.")
    private List<Long> idsMaterial;
}
