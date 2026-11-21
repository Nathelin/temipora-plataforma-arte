package com.temipora.backend_springboot.modelo;

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

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "fecha_limite_aprobacion")
    private LocalDateTime fechaLimiteAprobacion;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @Column(nullable = false)
    private int revisiones = 0;

    @Column(name = "fecha_limite_reentrega")
    private LocalDateTime fechaLimiteReentrega;

    @Column(name = "entrega_descripcion", length = 500)
    private String entregaDescripcion;

    @Column(name = "entrega_imagen", length = 255)
    private String entregaImagen;
}
