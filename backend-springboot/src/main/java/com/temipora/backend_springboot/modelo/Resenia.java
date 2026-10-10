package com.temipora.backend_springboot.modelo;


import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
public class Resenia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idResenia;

    @NotNull
    private LocalDateTime fechaHora;

    private String titulo;

    private String descripcion;

    @NotNull
    private int calificacion;

    @NotNull
    private boolean estado;

}
