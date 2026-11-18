package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
public class CompraDTO {

    private Long idCompra;
    private EstadoCompra estado;
    private BigDecimal subtotal;
    private BigDecimal total;
    private String moneda;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private boolean puedeDescargar;
    @Builder.Default
    private List<CompraGrupoDTO> grupos = new ArrayList<>();
}
