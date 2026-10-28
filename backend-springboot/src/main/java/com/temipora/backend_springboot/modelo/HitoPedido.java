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
@Table(name = "hito_pedido")
@Getter
@Setter
@NoArgsConstructor
public class HitoPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idHitoPedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_hito_presupuesto", nullable = false)
    private HitoPresupuesto hitoPresupuesto;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoHito tipo;

    @Column(name = "requiere_entrega", nullable = false)
    private boolean requiereEntrega;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "monto_cobro", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoCobro;

    @Column(name = "comision_monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal comisionMonto;

    @Column(name = "comision_monto_cobro", nullable = false, precision = 12, scale = 2)
    private BigDecimal comisionMontoCobro;

    @Column(name = "monto_neto", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoNeto;

    @Column(name = "monto_neto_cobro", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoNetoCobro;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 20)
    private EstadoPagoHito estadoPago = EstadoPagoHito.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_entrega", nullable = false, length = 30)
    private EstadoEntregaHito estadoEntrega = EstadoEntregaHito.NO_INICIADO;

    @Column(name = "fecha_limite_pago")
    private LocalDateTime fechaLimitePago;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "id_pago_aprobado")
    private Long idPagoAprobado;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "fecha_limite_aprobacion")
    private LocalDateTime fechaLimiteAprobacion;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @Column(name = "entrega_descripcion", length = 500)
    private String entregaDescripcion;

    @Column(name = "entrega_imagen", length = 255)
    private String entregaImagen;
}
