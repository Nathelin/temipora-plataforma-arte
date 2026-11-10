package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Entregable declarado en el alta. En v1 el archivo se identifica por la
 * huella de una imagen ya subida del producto; todavía no hay subida de
 * archivos arbitrarios (ver fase-3-entrega-digital-y-packs.md).
 */
@Getter
@Setter
public class ProductoEntregableRequest {

    /**
     * SHA-256 de la imagen del producto que se entrega. Debe pertenecer a la
     * galería o a una variante del propio producto.
     */
    @Pattern(regexp = "^\\s*$|^[0-9a-fA-F]{64}$",
        message = "La huella SHA-256 del entregable no es válida")
    private String sha256;

    @Size(max = 255, message = "El nombre del entregable no puede superar los 255 caracteres")
    private String nombre;

    @Min(value = 0, message = "El orden del entregable no puede ser negativo")
    private Integer orden;
}
