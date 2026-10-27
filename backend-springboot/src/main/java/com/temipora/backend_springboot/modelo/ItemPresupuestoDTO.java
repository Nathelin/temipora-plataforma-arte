package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
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
public class ItemPresupuestoDTO {

    private Long idItemPresupuesto;
    private Long idItemSolicitud;
    private String nombre;
    private Integer orden;
    private BigDecimal precioBase;
    private BigDecimal precioFinal;
    // Precios convertidos a la moneda que prefiere el usuario.
    private BigDecimal precioBaseMostrado;
    private BigDecimal precioFinalMostrado;
    private String nota;
    private List<RecargoPresupuestoDTO> recargos;

    public static ItemPresupuestoDTO desdeItem(ItemPresupuesto item) {
        return desdeItem(item, BigDecimal.ONE);
    }

    public static ItemPresupuestoDTO desdeItem(ItemPresupuesto item, BigDecimal factorMostrado) {
        return ItemPresupuestoDTO.builder()
            .idItemPresupuesto(item.getIdItemPresupuesto())
            .idItemSolicitud(item.getItemSolicitud() != null
                ? item.getItemSolicitud().getIdItemSolicitud()
                : null)
            .nombre(item.getItemSolicitud() != null
                ? item.getItemSolicitud().getNombre()
                : null)
            .orden(item.getOrden())
            .precioBase(item.getPrecioBase())
            .precioFinal(item.getPrecioFinal())
            .precioBaseMostrado(PresupuestoDTO.convertir(item.getPrecioBase(), factorMostrado))
            .precioFinalMostrado(PresupuestoDTO.convertir(item.getPrecioFinal(), factorMostrado))
            .nota(item.getNota())
            .recargos(item.getRecargos().stream()
                .map(recargo -> RecargoPresupuestoDTO.desdeRecargo(recargo, factorMostrado))
                .toList())
            .build();
    }
}
