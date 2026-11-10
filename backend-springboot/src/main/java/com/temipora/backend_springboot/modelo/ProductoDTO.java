package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoDTO {

    private Long idProducto;
    private Long idArtista;
    private String nombreArtista;
    private String urlFotoArtista;
    private Long idTipoProducto;
    private String nombreTipoProducto;
    private Long idMedio;
    private String nombreMedio;
    private TipoEntrega tipoEntregaMedio;
    private String titulo;
    private String descripcion;
    private Long idMoneda;
    private String moneda;
    private boolean contenidoSensible;
    private List<String> contenidosSensibles;
    private boolean permiteReventa;
    private BigDecimal porcentajeRegalia;
    private LicenciaProductoDTO licencia;
    private EstadoProducto estado;
    private String imagenPortada;
    private List<ProductoImagenDTO> imagenes;
    private List<ProductoVarianteDTO> variantes;
    private List<ProductoEntregableDTO> entregables;
    private List<EtiquetaDTO> etiquetas;
    private BigDecimal precioDesde;
    private boolean disponible;
    private int cantidadVariantes;
    private int cantidadVariantesDisponibles;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private LocalDateTime pausadoEn;
    private LocalDateTime archivadoEn;
}
