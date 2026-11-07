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
@Table(name = "sustitucion_opcion")
@Getter
@Setter
@NoArgsConstructor
public class SustitucionOpcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idSustitucionOpcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sustitucion", nullable = false)
    private Sustitucion sustitucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_servicio", nullable = false)
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_artista", nullable = false)
    private Usuario artista;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal score;

    @Column(length = 80)
    private String motivo;

    @Column(name = "precio_momento", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioMomento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_moneda", nullable = false)
    private Moneda moneda;

    @Column(nullable = false)
    private boolean elegida = false;
}
