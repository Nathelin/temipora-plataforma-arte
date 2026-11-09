package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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

    private boolean permiteReventa;

    @DecimalMin(value = "0", message = "El porcentaje de regalía no puede ser negativo")
    @DecimalMax(value = "10", message = "El porcentaje de regalía no puede superar el 10%")
    private BigDecimal porcentajeRegalia;

    @NotEmpty(message = "Debés agregar al menos una imagen del producto.")
    private List<@Valid ProductoImagenRequest> imagenes;

    @NotEmpty(message = "Debés agregar al menos una variante.")
    private List<@Valid ProductoVarianteRequest> variantes;

    @Size(max = 5, message = "Un producto puede tener como máximo 5 etiquetas.")
    private Set<@NotNull(message = "El id de la etiqueta no puede ser nulo.") Long> idsEtiquetas;

    @Valid
    private TerminosRequest terminos;
}
