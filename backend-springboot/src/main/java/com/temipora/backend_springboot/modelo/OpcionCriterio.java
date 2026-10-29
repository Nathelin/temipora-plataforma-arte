package com.temipora.backend_springboot.modelo;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "opcion_criterio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpcionCriterio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idOpcionCriterio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_criterio", nullable = false)
    private CriterioResenia criterio;

    @NotBlank(message = "La etiqueta es obligatoria.")
    @Size(max = 50, message = "La etiqueta no puede superar los 50 caracteres")
    @Column(nullable = false, length = 50)
    private String etiqueta;

    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolaridadOpcion polaridad = PolaridadOpcion.POSITIVA;

    @Builder.Default
    @Column(nullable = false)
    private int orden = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
