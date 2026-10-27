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
    // Para recargos fijos, el valor convertido a la moneda mostrada.
    // En los porcentajes se conserva el porcentaje original.
    private BigDecimal valorMostrado;
    private Integer orden;

    public static RecargoPresupuestoDTO desdeRecargo(RecargoItemPresupuesto recargo) {
        return desdeRecargo(recargo, BigDecimal.ONE);
    }

    public static RecargoPresupuestoDTO desdeRecargo(
            RecargoItemPresupuesto recargo,
            BigDecimal factorMostrado) {
        BigDecimal valorMostrado = recargo.getTipo() == TipoRecargo.FIJO
            ? PresupuestoDTO.convertir(recargo.getValor(), factorMostrado)
            : recargo.getValor();

        return RecargoPresupuestoDTO.builder()
            .idRecargo(recargo.getIdRecargo())
            .descripcion(recargo.getDescripcion())
            .tipo(recargo.getTipo())
            .valor(recargo.getValor())
            .valorMostrado(valorMostrado)
            .orden(recargo.getOrden())
            .build();
    }
}
