package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FiltroProductoRequest {

    private String titulo;
    private String nombreArtista;
    /** Búsqueda libre sobre título y descripción. */
    private String textoLibre;

    /** Selección única histórica; se normaliza a {@link #idsTiposProducto}. */
    private Long idTipoProducto;
    private List<Long> idsTiposProducto;
    /** Selección única histórica; se normaliza a {@link #idsMedios}. */
    private Long idMedio;
    private List<Long> idsMedios;

    private List<Long> idsEtiquetas;
    private List<Long> idsEtiquetasExcluidas;

    private Boolean sensible;
    private List<ContenidoSensible> contenidosSensibles;
    private List<ContenidoSensible> excluirContenidosSensibles;

    private BigDecimal precioMin;
    private BigDecimal precioMax;

    /** Deja fuera los productos sin stock disponible. */
    private Boolean soloConStock;
    /** Solo piezas únicas (stock == 1). */
    private Boolean soloPiezaUnica;
    /** Solo productos con stock ilimitado (stock == null). */
    private Boolean soloIlimitado;
    /** Incluye las publicaciones de reventa activas en el catálogo. */
    private Boolean incluirReventas;
    /** Excluye los productos del listado y deja solo las reventas. */
    private Boolean soloReventas;
}
