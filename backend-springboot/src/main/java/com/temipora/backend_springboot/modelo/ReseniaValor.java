package com.temipora.backend_springboot.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resenia_valor")
@Getter
@Setter
@NoArgsConstructor
public class ReseniaValor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idValor;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_version", nullable = false)
    private ReseniaVersion version;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_criterio", nullable = false)
    private CriterioResenia criterio;

    @Min(value = 1, message = "El valor debe ser de 1 a 5 estrellas.")
    @Max(value = 5, message = "El valor debe ser de 1 a 5 estrellas.")
    @Column(nullable = false)
    private int valor;
}
