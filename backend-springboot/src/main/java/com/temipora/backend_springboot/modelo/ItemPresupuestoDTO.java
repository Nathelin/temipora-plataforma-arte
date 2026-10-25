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
    private String nota;
    private List<RecargoPresupuestoDTO> recargos;

    public static ItemPresupuestoDTO desdeItem(ItemPresupuesto item) {
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
            .nota(item.getNota())
            .recargos(item.getRecargos().stream()
                .map(RecargoPresupuestoDTO::desdeRecargo)
                .toList())
            .build();
    }
}
