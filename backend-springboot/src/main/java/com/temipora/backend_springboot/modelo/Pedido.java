package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPedido;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud", nullable = false, unique = true)
    private Solicitud solicitud;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_presupuesto", nullable = false, unique = true)
    private Presupuesto presupuesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_artista", nullable = false)
    private Usuario artista;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_servicio", nullable = false)
    private Servicio servicio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado = EstadoPedido.PENDIENTE_PAGO;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "limite_entrega")
    private LocalDate limiteEntrega;

    @Column(name = "finalizado_en")
    private LocalDateTime finalizadoEn;

    @Column(name = "precio_final_legacy", precision = 12, scale = 2)
    private BigDecimal precioFinal;

    @Column(name = "precio_final_cobro_legacy", precision = 12, scale = 2)
    private BigDecimal precioFinalCobro;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_moneda_legacy")
    private Moneda moneda;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_moneda_cobro_legacy")
    private Moneda monedaCobro;

    @Column(name = "cotizacion_cobro_legacy", precision = 18, scale = 8)
    private BigDecimal cotizacionCobro;

    @Column(name = "comision_porcentaje_legacy", precision = 5, scale = 2)
    private BigDecimal comisionPorcentaje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_oferta_reactivacion_legacy")
    private OfertaReactivacion ofertaReactivacion;

    @Column(name = "descuento_porcentaje_legacy", precision = 5, scale = 2)
    private BigDecimal descuentoPorcentaje;

    @Column(name = "descuento_monto_legacy", precision = 12, scale = 2)
    private BigDecimal descuentoMonto;

    @Column(name = "descuento_monto_cobro_legacy", precision = 12, scale = 2)
    private BigDecimal descuentoMontoCobro;

    @Column(name = "precio_final_cliente_legacy", precision = 12, scale = 2)
    private BigDecimal precioFinalCliente;

    @Column(name = "precio_final_cliente_cobro_legacy", precision = 12, scale = 2)
    private BigDecimal precioFinalClienteCobro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_aceptacion_terminos_plataforma", nullable = false)
    private AceptacionTerminosPlataforma aceptacionTerminosPlataforma;

    @Column(name = "terminos_plataforma_version_legacy")
    private Integer terminosPlataformaVersion;

    @Column(name = "terminos_plataforma_snapshot_legacy", columnDefinition = "TEXT")
    private String terminosPlataformaSnapshot;

    @Column(name = "terminos_plataforma_aceptados_en_legacy")
    private LocalDateTime terminosPlataformaAceptadosEn;

    @Column(name = "publicacion_autorizada", nullable = false)
    private boolean publicacionAutorizada = false;

    @Column(name = "publicacion_autorizada_en")
    private LocalDateTime publicacionAutorizadaEn;

    @Column(name = "publicacion_ocultada_cliente", nullable = false)
    private boolean publicacionOcultadaCliente = false;

    @Column(name = "publicacion_ocultada_en")
    private LocalDateTime publicacionOcultadaEn;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<HitoPedido> hitos = new ArrayList<>();

    public void agregarHito(HitoPedido hito) {
        hito.setPedido(this);
        this.hitos.add(hito);
    }
}
