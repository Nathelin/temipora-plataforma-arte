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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resenia_imagen")
@Getter
@Setter
@NoArgsConstructor
public class ReseniaImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReseniaImagen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_version", nullable = false)
    private ReseniaVersion version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_archivo", nullable = false)
    private EntregaArchivo archivo;

    @Column(nullable = false)
    private Integer orden = 0;
}
