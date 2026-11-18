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
public class LicenciaProductoDerechoId implements Serializable {

    @Column(name = "id_licencia_producto", nullable = false)
    private Long idLicenciaProducto;

    @Column(name = "id_derecho_termino", nullable = false)
    private Long idDerechoTermino;
}
