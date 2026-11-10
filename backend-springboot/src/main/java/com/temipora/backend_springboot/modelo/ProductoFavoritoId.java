package com.temipora.backend_springboot.modelo;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ProductoFavoritoId implements Serializable {

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "id_producto")
    private Long idProducto;

    public ProductoFavoritoId(Long idUsuario, Long idProducto) {
        this.idUsuario = idUsuario;
        this.idProducto = idProducto;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) return true;
        if (!(otro instanceof ProductoFavoritoId)) return false;
        ProductoFavoritoId that = (ProductoFavoritoId) otro;
        return Objects.equals(idUsuario, that.idUsuario)
            && Objects.equals(idProducto, that.idProducto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUsuario, idProducto);
    }
}
