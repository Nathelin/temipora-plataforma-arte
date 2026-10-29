package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfiguracionReseniaRequest {

    @NotNull(message = "Los días para dejar una reseña son obligatorios.")
    @Min(value = 0, message = "La ventana de creación no puede ser negativa.")
    @Max(value = 365, message = "La ventana de creación no puede superar los 365 días.")
    private Integer diasVentanaCreacion;

    @NotNull(message = "Los días de edición son obligatorios.")
    @Min(value = 1, message = "La ventana de edición debe ser al menos 1 día.")
    @Max(value = 30, message = "La ventana de edición no puede superar los 30 días.")
    private Integer diasEdicion;

    @NotNull(message = "El mínimo de reseñas para mostrar la etiqueta es obligatorio.")
    @Min(value = 1, message = "El mínimo de reseñas debe ser al menos 1.")
    @Max(value = 50, message = "El mínimo de reseñas no puede superar 50.")
    private Integer minimoReseniasEtiqueta;

    @NotNull(message = "El máximo de imágenes por reseña es obligatorio.")
    @Min(value = 0, message = "El máximo de imágenes no puede ser negativo.")
    @Max(value = 10, message = "El máximo de imágenes no puede superar 10.")
    private Integer maxImagenesResenia;

    @NotNull(message = "El umbral de excelencia es obligatorio.")
    @DecimalMin(value = "1.00", message = "El umbral de excelencia debe ser al menos 1.00.")
    @DecimalMax(value = "5.00", message = "El umbral de excelencia no puede superar 5.00.")
    @Digits(integer = 1, fraction = 2, message = "El umbral admite hasta 2 decimales.")
    private BigDecimal umbralExcelente;

    @NotNull(message = "El umbral de buena reputación es obligatorio.")
    @DecimalMin(value = "1.00", message = "El umbral de buena reputación debe ser al menos 1.00.")
    @DecimalMax(value = "5.00", message = "El umbral de buena reputación no puede superar 5.00.")
    @Digits(integer = 1, fraction = 2, message = "El umbral admite hasta 2 decimales.")
    private BigDecimal umbralBuena;
}
