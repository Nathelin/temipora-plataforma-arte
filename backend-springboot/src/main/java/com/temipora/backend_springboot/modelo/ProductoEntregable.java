package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Archivo que recibe el comprador. En v1 hay como máximo uno por alcance y
 * una variante lo deriva de su archivo privado original, pero la clave es
 * propia para poder separar el entregable de la vidriera en el futuro.
 */
@Entity
@Table(name = "producto_entregable")
@Getter
@Setter
@NoArgsConstructor
public class ProductoEntregable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEntregable;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_variante")
    private ProductoVariante variante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlcanceEntregable alcance;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(length = 100)
    private String mime;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "clave_archivo", nullable = false, length = 255)
    private String claveArchivo;

    @Column(length = 64)
    private String sha256;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
