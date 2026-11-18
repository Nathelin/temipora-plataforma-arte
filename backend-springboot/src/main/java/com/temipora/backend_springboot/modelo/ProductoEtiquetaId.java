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
public class ProductoEtiquetaId implements Serializable {

    @Column(name = "id_producto", nullable = false)
    private Long idProducto;

    @Column(name = "id_etiqueta", nullable = false)
    private Long idEtiqueta;
}
