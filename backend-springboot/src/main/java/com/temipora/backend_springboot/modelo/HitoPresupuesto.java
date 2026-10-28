package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

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

/**
 * Plan de pagos propuesto por el artista al emitir el presupuesto.
 * Es un snapshot inmutable: no se modifica después de enviado.
 */
@Entity
@Table(name = "hito_presupuesto")
@Getter
@Setter
@NoArgsConstructor
public class HitoPresupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idHitoPresupuesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_presupuesto", nullable = false)
    private Presupuesto presupuesto;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(length = 250)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoHito tipo;

    @Column(precision = 5, scale = 2)
    private BigDecimal porcentaje;

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

    @Column(name = "requiere_entrega", nullable = false)
    private boolean requiereEntrega = true;
}
