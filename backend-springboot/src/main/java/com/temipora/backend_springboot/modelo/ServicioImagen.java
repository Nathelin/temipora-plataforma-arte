package com.temipora.backend_springboot.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "servicio_imagen")
@Getter
@Setter
@NoArgsConstructor
public class ServicioImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idServicioImagen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_servicio", nullable = false)
    private Servicio servicio;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String url;

    @Column(nullable = false)
    private Integer orden;
}
