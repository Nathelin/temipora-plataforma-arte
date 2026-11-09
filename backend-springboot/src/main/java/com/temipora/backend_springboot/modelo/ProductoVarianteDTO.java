package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoVarianteDTO {

    private Long idVariante;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stockDisponible;
    private Integer orden;
    private EstadoVariante estado;
    private String imagenUrl;
    private String sha256;
    private Integer numeroEdicion;
    private Long idPropietarioActual;
    private String nombrePropietario;
    private boolean esUnica;
    private boolean tieneLedger;
}
