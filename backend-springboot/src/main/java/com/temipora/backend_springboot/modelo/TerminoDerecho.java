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
@Table(name = "termino_derecho")
@Getter
@Setter
@NoArgsConstructor
public class TerminoDerecho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTerminoDerecho;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_terminos", nullable = false)
    private Terminos terminos;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_derecho_termino", nullable = false)
    private DerechoTermino derecho;

    @Column(nullable = false)
    private boolean disponible = false;

    @Column(name = "porcentaje_extra", precision = 6, scale = 2)
    private BigDecimal porcentajeExtra;
}
