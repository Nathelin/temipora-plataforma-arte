package com.temipora.backend_springboot.modelo;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cantidad de productos que devolvería cada opción de filtro.
 *
 * Cada mapa se calcula aplicando todos los filtros activos menos el de su propia
 * dimensión: si no, al elegir una etiqueta todas las demás quedarían en cero y el
 * panel se volvería inutilizable.
 */
@Getter
@Setter
@NoArgsConstructor
public class FacetasProductoDTO {

    /** idTipoProducto → cantidad */
    private Map<String, Long> tipos = new LinkedHashMap<>();
    /** idMedio → cantidad */
    private Map<String, Long> medios = new LinkedHashMap<>();
    /** idEtiqueta → cantidad */
    private Map<String, Long> etiquetas = new LinkedHashMap<>();
    /** nombre del enum ContenidoSensible → cantidad */
    private Map<String, Long> contenidosSensibles = new LinkedHashMap<>();
    /** conStock | sinStock */
    private Map<String, Long> disponibilidad = new LinkedHashMap<>();
    /** producto | reventa, solo si el catálogo incluye reventas */
    private Map<String, Long> origen = new LinkedHashMap<>();
}
