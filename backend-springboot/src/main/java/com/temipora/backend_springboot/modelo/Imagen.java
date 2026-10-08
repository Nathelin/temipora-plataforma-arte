package com.temipora.backend_springboot.modelo;


import java.time.LocalDateTime;

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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Imagen")
@Getter
@Setter
@NoArgsConstructor

public class Imagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idImagen;

    @NotNull
    private String url;

    @NotNull
    private boolean estado = true;

    @NotBlank
    private String titulo;

    @NotBlank
    private String descripcion;

    @NotNull
    private LocalDateTime fechaSubida;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idTipoArte", nullable = false)
    private TipoArte tipoArte;

    // TODO: agregar las advertencias en imágenes propias....... hasta ahora solo están en ajenas
    @Enumerated(EnumType.STRING)
    private ContenidoSensible contenidoSensible;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idPortfolio", nullable = false)
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idUsuario", nullable = false)
    private Usuario usuario;

}