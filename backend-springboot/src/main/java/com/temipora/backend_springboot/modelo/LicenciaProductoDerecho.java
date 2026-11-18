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
@Table(name = "licencia_producto_derecho")
@Getter
@Setter
@NoArgsConstructor
public class LicenciaProductoDerecho {

    @EmbeddedId
    private LicenciaProductoDerechoId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("idLicenciaProducto")
    @JoinColumn(name = "id_licencia_producto", nullable = false)
    private LicenciaProducto licencia;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @MapsId("idDerechoTermino")
    @JoinColumn(name = "id_derecho_termino", nullable = false)
    private DerechoTermino derecho;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion = LocalDateTime.now();
}
