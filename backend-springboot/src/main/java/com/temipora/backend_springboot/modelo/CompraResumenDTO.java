package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
public class CompraResumenDTO {

    private Long idCompra;
    private EstadoCompra estado;
    private BigDecimal total;
    private String moneda;
    private LocalDateTime fechaCreacion;
    private int cantidadItems;
    private int cantidadGrupos;
    private boolean puedePagar;
}
