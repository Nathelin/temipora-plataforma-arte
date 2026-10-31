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
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_hito_pedido", nullable = false)
    private HitoPedido hitoPedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_pagador", nullable = false)
    private Usuario usuarioPagador;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_moneda", nullable = false)
    private Moneda moneda;

    @Column(name = "monto_cobro", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoCobro;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_moneda_cobro", nullable = false)
    private Moneda monedaCobro;

    @Column(name = "cotizacion_cobro", nullable = false, precision = 18, scale = 8)
    private BigDecimal cotizacionCobro;

    @Column(name = "comision_monto_cobro", nullable = false, precision = 12, scale = 2)
    private BigDecimal comisionMontoCobro;

    @Column(name = "monto_neto_cobro", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoNetoCobro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado = EstadoPago.PENDIENTE;

    @Column(nullable = false, length = 30)
    private String proveedor;

    @Column(name = "id_externo", length = 100, unique = true)
    private String idExterno;

    @Column(name = "clave_idempotencia", length = 100, unique = true)
    private String claveIdempotencia;

    @Column(name = "id_pago_externo", length = 100)
    private String idPagoExterno;

    @Column(name = "estado_externo", length = 50)
    private String estadoExterno;

    @Column(name = "expira_en")
    private LocalDateTime expiraEn;

    @Column(name = "fecha_actualizacion_proveedor")
    private LocalDateTime fechaActualizacionProveedor;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_acreditacion")
    private LocalDateTime fechaAcreditacion;
}
