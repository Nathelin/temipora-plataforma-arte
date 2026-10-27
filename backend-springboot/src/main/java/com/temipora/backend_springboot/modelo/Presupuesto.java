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
@Table(name = "presupuesto")
@Getter
@Setter
@NoArgsConstructor
public class Presupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPresupuesto;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud", nullable = false, unique = true)
    private Solicitud solicitud;

    @Column(name = "precio_base_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBaseTotal;

    @Column(name = "precio_final", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioFinal;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_moneda", nullable = false)
    private Moneda moneda;

    /**
     * Moneda en la que se cobra el presupuesto. Hoy siempre es ARS.
     * Se guarda la cotización usada al emitir para que el importe a pagar
     * quede congelado aunque después cambie el tipo de cambio.
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_moneda_cobro", nullable = false)
    private Moneda monedaCobro;

    @Column(name = "cotizacion_cobro", nullable = false, precision = 18, scale = 8)
    private BigDecimal cotizacionCobro;

    @Column(name = "precio_base_total_cobro", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBaseTotalCobro;

    @Column(name = "precio_final_cobro", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioFinalCobro;

    /**
     * Comisión de la plataforma congelada al emitir el presupuesto. Se descuenta
     * del precio del artista: el cliente paga el precio final sin cambios.
     */
    @Column(name = "comision_porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal comisionPorcentaje;

    @Column(name = "comision_monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal comisionMonto;

    @Column(name = "comision_monto_cobro", nullable = false, precision = 12, scale = 2)
    private BigDecimal comisionMontoCobro;

    @Column(name = "monto_neto_artista", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoNetoArtista;

    @Column(name = "monto_neto_artista_cobro", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoNetoArtistaCobro;

    @Column(name = "tiempo_entrega_valor", nullable = false)
    private Integer tiempoEntregaValor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tiempo_entrega_unidad", nullable = false, length = 10)
    private ETiempoEntregaUnidad tiempoEntregaUnidad;

    @Column(name = "tiempo_entrega_dias", nullable = false)
    private Integer tiempoEntregaDias;

    @Column(name = "fecha_inicio_estimada")
    private LocalDate fechaInicioEstimada;

    @Column(length = 500)
    private String mensaje;

    @Column(name = "fecha_emision", nullable = false, updatable = false)
    private LocalDateTime fechaEmision = LocalDateTime.now();

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDateTime fechaVencimiento;

    @OneToMany(mappedBy = "presupuesto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<ItemPresupuesto> items = new ArrayList<>();

    public void agregarItem(ItemPresupuesto item) {
        item.setPresupuesto(this);
        this.items.add(item);
    }
}
