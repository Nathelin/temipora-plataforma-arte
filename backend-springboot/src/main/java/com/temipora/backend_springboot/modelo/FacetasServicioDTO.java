package com.temipora.backend_springboot.modelo;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cantidad de servicios que devolvería cada opción de filtro.
 *
 * Cada mapa se calcula aplicando todos los filtros activos menos el de su propia
 * dimensión: si no, al elegir "Acuarela" todas las demás opciones de medio
 * quedarían en cero y el panel se volvería inutilizable.
 */
@Getter
@Setter
@NoArgsConstructor
public class FacetasServicioDTO {

    /** idTipoServicio → cantidad */
    private Map<String, Long> tipos = new LinkedHashMap<>();
    /** idMedio → cantidad */
    private Map<String, Long> medios = new LinkedHashMap<>();
    /** idEtiqueta → cantidad */
    private Map<String, Long> etiquetas = new LinkedHashMap<>();
    /** nombre del enum ContenidoSensible → cantidad */
    private Map<String, Long> contenidosSensibles = new LinkedHashMap<>();
    /** abiertos | sinCupos */
    private Map<String, Long> disponibilidad = new LinkedHashMap<>();
    /**
     * Código de moneda → cantidad. Sólo se completa cuando el resultado tiene
     * más de una moneda: una faceta que no puede discriminar es ruido.
     */
    private Map<String, Long> monedas = new LinkedHashMap<>();
}
