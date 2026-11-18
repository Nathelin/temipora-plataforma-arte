package com.temipora.backend_springboot.controladores;

import java.math.BigDecimal;
import java.util.List;

import com.temipora.backend_springboot.modelo.ContenidoSensible;
import com.temipora.backend_springboot.modelo.FiltroServicioRequest;

import lombok.Getter;
import lombok.Setter;

/**
 * Parámetros de filtrado del catálogo. Se usa como {@code @ModelAttribute} para
 * que el listado y el conteo de facetas compartan exactamente el mismo
 * contrato de query string.
 */
@Getter
@Setter
public class ParametrosFiltroServicio {

    private String nombre;
    private String nombreArtista;
    private String textoLibre;
    private Long idTipoServicio;
    private List<Long> idsTiposServicio;
    private Boolean sensible;
    private BigDecimal precioMin;
    private BigDecimal precioMax;
    private Integer tiempoMaxDias;
    private List<Long> idsMedios;
    private List<Long> idsEtiquetas;
    private List<Long> idsEtiquetasExcluidas;
    private List<ContenidoSensible> contenidosSensibles;
    private List<ContenidoSensible> excluirContenidosSensibles;
    private Boolean soloConListaEspera;
    private BigDecimal valoracionMinima;
    private String monedaDeclarada;
    private Boolean incluirSinCupos;

    public FiltroServicioRequest aFiltro() {
        FiltroServicioRequest filtro = new FiltroServicioRequest();
        filtro.setNombre(nombre);
        filtro.setNombreArtista(nombreArtista);
        filtro.setTextoLibre(textoLibre);
        filtro.setIdTipoServicio(idTipoServicio);
        if (idsTiposServicio != null && !idsTiposServicio.isEmpty()) {
            filtro.setIdsTiposServicio(idsTiposServicio);
        } else if (idTipoServicio != null) {
            // Compatibilidad con el parámetro histórico de selección única.
            filtro.setIdsTiposServicio(List.of(idTipoServicio));
        }
        filtro.setSensible(sensible);
        filtro.setPrecioMin(precioMin);
        filtro.setPrecioMax(precioMax);
        filtro.setTiempoMaxDias(tiempoMaxDias);
        filtro.setIdsMedios(idsMedios);
        filtro.setIdsEtiquetas(idsEtiquetas);
        filtro.setIdsEtiquetasExcluidas(idsEtiquetasExcluidas);
        filtro.setContenidosSensibles(contenidosSensibles);
        filtro.setExcluirContenidosSensibles(excluirContenidosSensibles);
        filtro.setSoloConListaEspera(soloConListaEspera);
        filtro.setValoracionMinima(valoracionMinima);
        filtro.setMonedaDeclarada(monedaDeclarada);
        filtro.setIncluirSinCupos(incluirSinCupos == null || incluirSinCupos);
        return filtro;
    }
}
