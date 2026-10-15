package com.temipora.backend_springboot.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "derecho_termino")
@Getter
@Setter
@NoArgsConstructor
public class DerechoTermino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDerechoTermino;

    @Size(max = 30)
    @Column(unique = true, length = 30)
    private String codigo;

    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "requiere_pieza_unica", nullable = false)
    private boolean requierePiezaUnica = false;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(nullable = false)
    private boolean activo = true;
}
