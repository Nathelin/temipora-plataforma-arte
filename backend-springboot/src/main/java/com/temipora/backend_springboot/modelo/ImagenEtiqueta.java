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
@Table(name = "ImagenEtiqueta",
        uniqueConstraints = @UniqueConstraint(columnNames = { "idImagen", "idEtiqueta" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenEtiqueta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idImagenEtiqueta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idImagen", nullable = false)
    private Imagen imagen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idEtiqueta", nullable = false)
    private Etiqueta etiqueta;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime fechaAsignacion = LocalDateTime.now();
}
