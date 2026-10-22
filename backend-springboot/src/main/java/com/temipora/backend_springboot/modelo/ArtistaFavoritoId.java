package com.temipora.backend_springboot.modelo;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ArtistaFavoritoId implements Serializable {

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "id_artista", nullable = false)
    private Long idArtista;
}
