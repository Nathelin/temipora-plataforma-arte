package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
@Table(name = "conversion_moneda")
@Getter
@Setter
@NoArgsConstructor
public class ConversionMoneda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conversion_moneda")
    private Long idConversionMoneda;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_moneda_entrada", nullable = false)
    private Moneda monedaEntrada;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_moneda_salida", nullable = false)
    private Moneda monedaSalida;

    @Column(name = "factor", nullable = false, precision = 18, scale = 8)
    private BigDecimal factor;

    @Column(name = "inicio_validez", nullable = false)
    private LocalDateTime inicioValidez;

    @Column(name = "fin_validez")
    private LocalDateTime finValidez;

    @Column(name = "origen", nullable = false, length = 20)
    private String origen = "MANUAL";

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
