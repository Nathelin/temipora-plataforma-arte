package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "producto_favorito")
@Getter
@Setter
@NoArgsConstructor
public class ProductoFavorito {

    @EmbeddedId
    private ProductoFavoritoId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idUsuario")
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idProducto")
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "fecha_agregado", nullable = false)
    private LocalDateTime fechaAgregado = LocalDateTime.now();
}
