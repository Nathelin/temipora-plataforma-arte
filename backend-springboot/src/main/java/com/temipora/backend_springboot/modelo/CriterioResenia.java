package com.temipora.backend_springboot.modelo;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
@Table(name = "criterio_resenia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriterioResenia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCriterioResenia;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
    @Column(nullable = false, length = 50)
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 250, message = "La descripción no puede superar los 250 caracteres")
    @Column(nullable = false, length = 250)
    private String descripcion;

    @Size(max = 500, message = "Las observaciones no pueden superar los 500 caracteres")
    @Column(length = 500)
    private String observaciones;

    @NotNull(message = "El ámbito es obligatorio.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AmbitoResenia ambito;

    @NotNull(message = "La escala es obligatoria.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EscalaCriterio escala;

    @Builder.Default
    @Column(nullable = false)
    private boolean obligatorio = false;

    @Builder.Default
    @Column(nullable = false)
    private int orden = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    @Builder.Default
    @OneToMany(mappedBy = "criterio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<OpcionCriterio> opciones = new ArrayList<>();

    public void agregarOpcion(OpcionCriterio opcion) {
        opcion.setCriterio(this);
        this.opciones.add(opcion);
    }
}
