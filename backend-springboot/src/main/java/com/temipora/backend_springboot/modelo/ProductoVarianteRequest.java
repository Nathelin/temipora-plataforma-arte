package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductoVarianteRequest {

    private Long idVariante;

    @Size(max = 100, message = "El nombre de la variante no puede superar los 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripción de la variante no puede superar los 500 caracteres")
    private String descripcion;

    @DecimalMin(value = "0", message = "El precio de la variante no puede ser negativo")
    private BigDecimal precio;

    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stockDisponible;

    @Min(value = 0, message = "El orden no puede ser negativo")
    private Integer orden;

    private EstadoVariante estado;

    @Size(max = 255, message = "La URL de la imagen no puede superar los 255 caracteres")
    private String urlImagen;

    /**
     * Vacío o nulo significa que la variante todavía no tiene imagen (borrador).
     */
    @Pattern(regexp = "^\\s*$|^[0-9a-fA-F]{64}$",
        message = "La huella SHA-256 de la imagen no es válida")
    private String sha256;

    @Size(max = 255, message = "La clave del archivo original no puede superar los 255 caracteres")
    private String claveArchivoOriginal;

    /**
     * Imagen del producto que se entrega con esta variante. Si no se indica,
     * se deriva del archivo original de la variante.
     */
    @Pattern(regexp = "^\\s*$|^[0-9a-fA-F]{64}$",
        message = "La huella SHA-256 del entregable no es válida")
    private String entregableSha256;

    @Size(max = 255, message = "El nombre del entregable no puede superar los 255 caracteres")
    private String entregableNombre;

    @Min(value = 0, message = "El número de edición no puede ser negativo")
    private Integer numeroEdicion;
}
