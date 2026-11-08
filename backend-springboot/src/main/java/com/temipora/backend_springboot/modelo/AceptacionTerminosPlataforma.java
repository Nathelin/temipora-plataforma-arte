package com.temipora.backend_springboot.modelo;

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
@Table(name = "aceptacion_terminos_plataforma")
@Getter
@Setter
@NoArgsConstructor
public class AceptacionTerminosPlataforma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAceptacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_terminos_plataforma", nullable = false)
    private TerminosPlataforma terminosPlataforma;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String snapshot;

    @Column(name = "fecha_aceptacion", nullable = false, updatable = false)
    private LocalDateTime fechaAceptacion = LocalDateTime.now();
}
