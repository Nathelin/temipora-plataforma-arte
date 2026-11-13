package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FiltroServicioRequest {

    private String nombre;
    private String nombreArtista;
    /**
     * Búsqueda libre sobre nombre y descripción del servicio.
     */
    private String textoLibre;
    private Long idTipoServicio;
    /**
     * Tipos de servicio aceptados. OR entre ellos. Si llega un solo tipo por la
     * vía histórica (`idTipoServicio`) se normaliza a esta lista.
     */
    private List<Long> idsTiposServicio;
    private Boolean sensible;
    private BigDecimal precioMin;
    private BigDecimal precioMax;
    private Integer tiempoMaxDias;

    /**
     * Medios artísticos aceptados. Dentro de la lista se combinan con OR.
     */
    private List<Long> idsMedios;
    /**
     * Etiquetas requeridas. OR entre ellas, AND con el resto de las facetas.
     */
    private List<Long> idsEtiquetas;
    /**
     * Etiquetas excluidas explícitamente (patrón AO3 acotado).
     */
    private List<Long> idsEtiquetasExcluidas;

    private List<ContenidoSensible> contenidosSensibles;
    private List<ContenidoSensible> excluirContenidosSensibles;

    /**
     * Solo servicios cuyo artista acepta lista de espera.
     */
    private Boolean soloConListaEspera;
    /**
     * Promedio de reseñas mínimo (inclusive).
     */
    private BigDecimal valoracionMinima;
    /**
     * Código de la moneda en la que el artista declaró el precio base ("ARS", "USD").
     * Independiente del rango de precio, que siempre se evalúa en la moneda del usuario.
     */
    private String monedaDeclarada;

    /**
     * Incluye en el catálogo los servicios que hoy no pueden recibir una
     * solicitud (sin cupos, solo espera o cerrados), siempre después de los
     * disponibles. Por defecto se incluyen.
     */
    private Boolean incluirSinCupos;
}
