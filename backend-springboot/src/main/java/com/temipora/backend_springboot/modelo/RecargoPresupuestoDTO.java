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
public class RecargoPresupuestoDTO {

    private Long idRecargo;
    private String descripcion;
    private TipoRecargo tipo;
    private BigDecimal valor;
    private Integer orden;

    public static RecargoPresupuestoDTO desdeRecargo(RecargoItemPresupuesto recargo) {
        return RecargoPresupuestoDTO.builder()
            .idRecargo(recargo.getIdRecargo())
            .descripcion(recargo.getDescripcion())
            .tipo(recargo.getTipo())
            .valor(recargo.getValor())
            .orden(recargo.getOrden())
            .build();
    }
}
