package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "configuracion_recomendacion_producto")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionRecomendacionProducto {

    public static final Long ID_UNICO = 1L;

    @Id
    @Column(name = "id_configuracion", nullable = false)
    private Long idConfiguracion = ID_UNICO;

    @Column(name = "peso_favorito_producto", nullable = false, precision = 5, scale = 4)
    private BigDecimal pesoFavoritoProducto = new BigDecimal("0.1800");

    @Column(name = "peso_favorito_artista", nullable = false, precision = 5, scale = 4)
    private BigDecimal pesoFavoritoArtista = new BigDecimal("0.1500");

    @Column(name = "peso_historial", nullable = false, precision = 5, scale = 4)
    private BigDecimal pesoHistorial = new BigDecimal("0.1500");

    @Column(name = "peso_co_compra", nullable = false, precision = 5, scale = 4)
    private BigDecimal pesoCoCompra = new BigDecimal("0.1800");

    @Column(name = "peso_co_visita", nullable = false, precision = 5, scale = 4)
    private BigDecimal pesoCoVisita = new BigDecimal("0.0800");

    @Column(name = "peso_etiquetas", nullable = false, precision = 5, scale = 4)
    private BigDecimal pesoEtiquetas = new BigDecimal("0.1400");

    @Column(name = "peso_calidad", nullable = false, precision = 5, scale = 4)
    private BigDecimal pesoCalidad = new BigDecimal("0.0600");

    @Column(name = "peso_recencia", nullable = false, precision = 5, scale = 4)
    private BigDecimal pesoRecencia = new BigDecimal("0.0300");

    @Column(name = "peso_precio", nullable = false, precision = 5, scale = 4)
    private BigDecimal pesoPrecio = new BigDecimal("0.0300");

    @Column(name = "max_sugerencias", nullable = false)
    private int maxSugerencias = 12;

    @Column(name = "max_por_artista", nullable = false)
    private int maxPorArtista = 2;

    @Column(name = "min_franjas_precio", nullable = false)
    private int minFranjasPrecio = 3;

    @Column(name = "rango_precio_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal rangoPrecioPct = new BigDecimal("40.00");

    @Column(name = "dias_decaimiento", nullable = false)
    private int diasDecaimiento = 90;

    @Column(name = "dias_visitas", nullable = false)
    private int diasVisitas = 180;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
