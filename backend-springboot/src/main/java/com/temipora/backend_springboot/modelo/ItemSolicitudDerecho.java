package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "item_solicitud_derecho")
@Getter
@Setter
@NoArgsConstructor
public class ItemSolicitudDerecho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idItemSolicitudDerecho;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_item_solicitud", nullable = false)
    private ItemSolicitud itemSolicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_derecho_termino", nullable = false)
    private DerechoTermino derecho;

    @Column(length = 30)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "porcentaje_extra", nullable = false, precision = 6, scale = 2)
    private BigDecimal porcentajeExtra = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer orden = 0;
}
