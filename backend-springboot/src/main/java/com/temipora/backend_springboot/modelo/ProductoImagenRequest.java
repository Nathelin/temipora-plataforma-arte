package com.temipora.backend_springboot.modelo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductoImagenRequest {

    @NotBlank(message = "La URL de la imagen es obligatoria.")
    @Size(max = 255, message = "La URL de la imagen no puede superar los 255 caracteres")
    private String url;

    @NotBlank(message = "La huella SHA-256 de la imagen es obligatoria.")
    @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "La huella SHA-256 de la imagen no es válida")
    private String sha256;

    @Size(max = 255, message = "La clave del archivo original no puede superar los 255 caracteres")
    private String claveArchivoOriginal;

    @Min(value = 0, message = "El orden de la imagen no puede ser negativo")
    private Integer orden;
}
