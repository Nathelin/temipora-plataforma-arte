package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServicioRequest {

    @NotNull(message = "El tipo de servicio es obligatorio.")
    private Long idTipoServicio;

    @NotNull(message = "El medio es obligatorio.")
    private Long idMedio;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 250, message = "La descripción no puede superar los 250 caracteres")
    private String descripcion;

    @NotNull(message = "El precio base es obligatorio.")
    @DecimalMin(value = "0", message = "El precio base no puede ser negativo")
    private BigDecimal precioBase;

    // Moneda del precio base. Si no se especifica, se usa la moneda predeterminada.
    private Long idMoneda;

    private boolean contenidoSensible;

    // Tipos de contenido sensible que pueden aparecer en el servicio.
    // Solo tiene sentido si contenidoSensible es true; el servicio valida que no esté vacío en ese caso.
    private Set<@NotNull ContenidoSensible> contenidosSensibles;

    @NotNull(message = "El tiempo estimado de entrega es obligatorio.")
    @Min(value = 1, message = "El tiempo de entrega debe ser al menos 1")
    private Integer tiempoEntregaValor;

    @NotNull(message = "La unidad de tiempo es obligatoria.")
    private ETiempoEntregaUnidad tiempoEntregaUnidad;

    @NotNull(message = "La cantidad de cupos es obligatoria.")
    @Min(value = 0, message = "Los cupos no pueden ser negativos")
    private Integer cupos;

    private boolean habilitarCuposTrasEntrega;

    @Size(max = 255, message = "La URL de portada no puede superar los 255 caracteres")
    private String imagenPortada;

    private List<@Size(max = 255, message = "La URL no puede superar los 255 caracteres") String> imagenes;
}
