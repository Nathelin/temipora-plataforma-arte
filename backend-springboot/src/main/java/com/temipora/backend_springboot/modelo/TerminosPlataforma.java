package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "terminos_plataforma")
@Getter
@Setter
@NoArgsConstructor
public class TerminosPlataforma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTerminosPlataforma;

    @Column(nullable = false, unique = true)
    private Integer version;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "comision_servicio", nullable = false, precision = 5, scale = 2)
    private BigDecimal comisionServicio;

    @Column(name = "comision_producto", nullable = false, precision = 5, scale = 2)
    private BigDecimal comisionProducto;

    @Column(name = "fecha_publicacion", nullable = false, updatable = false)
    private LocalDateTime fechaPublicacion = LocalDateTime.now();

    @Column(nullable = false)
    private boolean vigente = true;
}
