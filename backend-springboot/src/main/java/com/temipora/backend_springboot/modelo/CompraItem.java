package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "compra_item")
@Getter
@Setter
@NoArgsConstructor
public class CompraItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra_item")
    private Long idCompraItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_compra_grupo", nullable = false)
    private CompraGrupo grupo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_variante", nullable = false)
    private ProductoVariante variante;

    @Column(name = "titulo_producto", nullable = false, length = 100)
    private String tituloProducto;

    @Column(name = "nombre_variante", nullable = false, length = 100)
    private String nombreVariante;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entrega", nullable = false, length = 20)
    private TipoEntrega tipoEntrega;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "precio_original", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioOriginal;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_moneda_original", nullable = false)
    private Moneda monedaOriginal;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal cotizacion = BigDecimal.ONE;

    @Column(name = "precio_cobro", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioCobro;

    @Column(name = "comision_porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal comisionPorcentaje;

    @Column(name = "comision_monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal comisionMonto;

    @Column(name = "sha256_archivo", length = 64)
    private String sha256Archivo;

    @Column(name = "licencia_snapshot", columnDefinition = "TEXT")
    private String licenciaSnapshot;

    @Column(name = "licencia_aceptada_en")
    private LocalDateTime licenciaAceptadaEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenCompraItem origen = OrigenCompraItem.PRIMERA_VENTA;

    @Column(name = "id_publicacion_reventa")
    private Long idPublicacionReventa;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_entrega", nullable = false, length = 20)
    private EstadoEntregaItem estadoEntrega = EstadoEntregaItem.PENDIENTE;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;
}
