package com.temipora.backend_springboot.modelo;

import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductoRequest {

    @NotNull(message = "El tipo de producto es obligatorio.")
    private Long idTipoProducto;

    @NotNull(message = "El medio es obligatorio.")
    private Long idMedio;

    @NotBlank(message = "El título es obligatorio.")
    @Size(max = 100, message = "El título no puede superar los 100 caracteres")
    private String titulo;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    private String descripcion;

    private Long idMoneda;

    private boolean contenidoSensible;

    private Set<@NotNull ContenidoSensible> contenidosSensibles;

    @NotEmpty(message = "Debés agregar al menos una imagen del producto.")
    private List<@Valid ProductoImagenRequest> imagenes;

    @NotEmpty(message = "Debés agregar al menos una variante.")
    @Size(max = 50, message = "Un producto puede tener como máximo 50 variantes.")
    private List<@Valid ProductoVarianteRequest> variantes;

    @Size(max = 5, message = "Un producto puede tener como máximo 5 etiquetas.")
    private Set<@NotNull(message = "El id de la etiqueta no puede ser nulo.") Long> idsEtiquetas;

    @NotNull(message = "La licencia del producto es obligatoria.")
    @Valid
    private LicenciaProductoRequest licencia;
}
