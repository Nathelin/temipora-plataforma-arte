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
@Table(name = "producto_etiqueta")
@Getter
@Setter
@NoArgsConstructor
public class ProductoEtiqueta {

    @EmbeddedId
    private ProductoEtiquetaId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idProducto")
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idEtiqueta")
    @JoinColumn(name = "id_etiqueta", nullable = false)
    private Etiqueta etiqueta;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion = LocalDateTime.now();
}
