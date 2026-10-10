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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ImagenMaterial",
        uniqueConstraints = @UniqueConstraint(columnNames = { "idImagen", "idMaterial" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idImagenMaterial;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idImagen", nullable = false)
    private Imagen imagen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idMaterial", nullable = false)
    private Material material;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime fechaAsignacion = LocalDateTime.now();
}
