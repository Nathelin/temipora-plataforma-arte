package com.temipora.backend_springboot.modelo;

import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos de un producto. Se usa tanto para guardar un borrador como para
 * publicar: la diferencia no está en las anotaciones sino en la validación
 * que aplica el servicio ({@code validarParaPublicar}).
 */
@Getter
@Setter
public class ProductoRequest {

    private Long idTipoProducto;

    private Long idMedio;

    @Size(max = 100, message = "El título no puede superar los 100 caracteres")
    private String titulo;

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    private String descripcion;

    private Long idMoneda;

    private boolean contenidoSensible;

    private Set<@NotNull ContenidoSensible> contenidosSensibles;

    @Size(max = 60, message = "Un producto puede tener como máximo 60 imágenes.")
    private List<@Valid ProductoImagenRequest> imagenes;

    @Size(max = 50, message = "Un producto puede tener como máximo 50 variantes.")
    private List<@Valid ProductoVarianteRequest> variantes;

    @Size(max = 5, message = "Un producto puede tener como máximo 5 etiquetas.")
    private Set<@NotNull(message = "El id de la etiqueta no puede ser nulo.") Long> idsEtiquetas;

    @Valid
    private LicenciaProductoRequest licencia;

    /**
     * Archivos que reciben todos los compradores, además de su variante.
     * Opcional.
     */
    @Size(max = 20, message = "Un producto puede tener como máximo 20 entregables comunes.")
    private List<@Valid ProductoEntregableRequest> entregablesComunes;
}
