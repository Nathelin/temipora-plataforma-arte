package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resenia_version")
@Getter
@Setter
@NoArgsConstructor
public class ReseniaVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_resenia", nullable = false)
    private Resenia resenia;

    @NotNull
    @Column(nullable = false)
    private int numero;

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
    @Column(length = 1000)
    private String descripcion;

    @Min(value = 1, message = "La calificación debe ser de 1 a 5 estrellas.")
    @Max(value = 5, message = "La calificación debe ser de 1 a 5 estrellas.")
    @Column(name = "calificacion_general", nullable = false)
    private int calificacionGeneral;

    @Column(name = "fecha_version", nullable = false)
    private LocalDateTime fechaVersion = LocalDateTime.now();

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReseniaValor> valores = new ArrayList<>();

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReseniaOpcion> opciones = new ArrayList<>();

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<ReseniaImagen> imagenes = new ArrayList<>();

    public void agregarValor(ReseniaValor valor) {
        valor.setVersion(this);
        this.valores.add(valor);
    }

    public void agregarOpcion(ReseniaOpcion opcion) {
        opcion.setVersion(this);
        this.opciones.add(opcion);
    }

    public void agregarImagen(ReseniaImagen imagen) {
        imagen.setVersion(this);
        this.imagenes.add(imagen);
    }
}
