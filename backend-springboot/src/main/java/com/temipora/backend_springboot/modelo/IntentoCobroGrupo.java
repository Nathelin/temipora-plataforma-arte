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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "intento_cobro_grupo",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_intento_grupo",
        columnNames = { "id_intento_cobro", "id_compra_grupo" }))
@Getter
@Setter
@NoArgsConstructor
public class IntentoCobroGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_intento_cobro_grupo")
    private Long idIntentoCobroGrupo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_intento_cobro", nullable = false)
    private IntentoCobro intentoCobro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_compra_grupo", nullable = false)
    private CompraGrupo grupo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal comision;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal neto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoIntentoCobro estado = EstadoIntentoCobro.PENDIENTE;

    @Column(name = "receiver_id", length = 100)
    private String receiverId;
}
