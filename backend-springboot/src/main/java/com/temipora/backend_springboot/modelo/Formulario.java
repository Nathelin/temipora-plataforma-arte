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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "formulario")
@Getter
@Setter
@NoArgsConstructor
public class Formulario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFormulario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_artista")
    private Usuario artista;

    @Column(name = "id_servicio")
    private Long idServicio;

    @Column(name = "es_sistema", nullable = false)
    private boolean esSistema = false;

    @Column(nullable = false)
    private boolean vigente = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "formulario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<Pregunta> preguntas = new ArrayList<>();

    public void reemplazarPreguntas(List<Pregunta> nuevas) {
        this.preguntas.clear();
        for (Pregunta p : nuevas) {
            p.setFormulario(this);
            this.preguntas.add(p);
        }
    }
}
