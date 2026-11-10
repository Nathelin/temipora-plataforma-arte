package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Producto tal como se muestra en el catálogo público. En los listados es
 * liviano: las variantes, imágenes, licencia y etiquetas quedan vacías. El
 * detalle (GET /api/productos/{id}) sí las completa con un subconjunto seguro
 * para que la vista pública coincida con la vista previa del artista.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoCatalogoDTO {

    private Long idProducto;
    private String titulo;
    private String descripcion;
    private String imagenPortada;

    private Long idArtista;
    private String nombreArtista;
    private String nombreDisplayArtista;
    private String urlFotoArtista;

    private Long idTipoProducto;
    private String nombreTipoProducto;
    private Long idMedio;
    private String nombreMedio;

    /** Precio de la variante más barata disponible. */
    private BigDecimal precioDesde;
    private String moneda;

    private TipoEntrega tipoEntrega;

    /** Stock total sumando variantes; null si alguna es ilimitada. */
    private Integer stockTotal;
    private boolean tieneStockIlimitado;
    private boolean esPiezaUnica;
    private boolean disponible;
    private int cantidadVariantes;
    private int cantidadVariantesDisponibles;

    private boolean contenidoSensible;
    private List<String> contenidosSensibles;

    private BigDecimal calificacionPromedio;
    private int cantidadResenias;

    /** Solo en el detalle: galería completa ordenada. */
    private List<ProductoImagenDTO> imagenes;
    /** Solo en el detalle: variantes públicas (sin hash ni archivo original). */
    private List<ProductoVarianteDTO> variantes;
    /** Solo en el detalle: licencia vigente. */
    private LicenciaProductoDTO licencia;
    /** Solo en el detalle: etiquetas válidas asignadas. */
    private List<EtiquetaDTO> etiquetas;
}
