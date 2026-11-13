package com.temipora.backend_springboot.controladores;

import java.math.BigDecimal;
import java.util.List;

import com.temipora.backend_springboot.modelo.ContenidoSensible;
import com.temipora.backend_springboot.modelo.FiltroProductoRequest;

import lombok.Getter;
import lombok.Setter;

/**
 * Parámetros de filtrado del catálogo de productos. Se usa como
 * {@code @ModelAttribute} para que el listado y los conteos de facetas
 * compartan exactamente el mismo contrato de query string.
 */
@Getter
@Setter
public class ParametrosFiltroProducto {

    private String titulo;
    private String nombreArtista;
    private String textoLibre;
    private Long idTipoProducto;
    private List<Long> idsTiposProducto;
    private Long idMedio;
    private List<Long> idsMedios;
    private List<Long> idsEtiquetas;
    private List<Long> idsEtiquetasExcluidas;
    private Boolean sensible;
    private List<ContenidoSensible> contenidosSensibles;
    private List<ContenidoSensible> excluirContenidosSensibles;
    private BigDecimal precioMin;
    private BigDecimal precioMax;
    private Boolean soloConStock;
    private Boolean soloPiezaUnica;
    private Boolean soloIlimitado;
    private Boolean incluirReventas;
    private Boolean soloReventas;

    public FiltroProductoRequest aFiltro() {
        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setTitulo(titulo);
        filtro.setNombreArtista(nombreArtista);
        filtro.setTextoLibre(textoLibre);
        filtro.setIdTipoProducto(idTipoProducto);
        if (idsTiposProducto != null && !idsTiposProducto.isEmpty()) {
            filtro.setIdsTiposProducto(idsTiposProducto);
        } else if (idTipoProducto != null) {
            // Compatibilidad con el parámetro histórico de selección única.
            filtro.setIdsTiposProducto(List.of(idTipoProducto));
        }
        filtro.setIdMedio(idMedio);
        if (idsMedios != null && !idsMedios.isEmpty()) {
            filtro.setIdsMedios(idsMedios);
        } else if (idMedio != null) {
            filtro.setIdsMedios(List.of(idMedio));
        }
        filtro.setIdsEtiquetas(idsEtiquetas);
        filtro.setIdsEtiquetasExcluidas(idsEtiquetasExcluidas);
        filtro.setSensible(sensible);
        filtro.setContenidosSensibles(contenidosSensibles);
        filtro.setExcluirContenidosSensibles(excluirContenidosSensibles);
        filtro.setPrecioMin(precioMin);
        filtro.setPrecioMax(precioMax);
        filtro.setSoloConStock(soloConStock);
        filtro.setSoloPiezaUnica(soloPiezaUnica);
        filtro.setSoloIlimitado(soloIlimitado);
        filtro.setIncluirReventas(incluirReventas);
        filtro.setSoloReventas(soloReventas);
        return filtro;
    }
}
