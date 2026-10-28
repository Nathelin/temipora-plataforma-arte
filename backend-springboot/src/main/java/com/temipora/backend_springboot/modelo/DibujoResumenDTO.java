package com.temipora.backend_springboot.modelo;

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
public class DibujoResumenDTO {

    private Long idItemSolicitud;
    private String nombre;
    private Integer orden;

    public static DibujoResumenDTO desdeItem(ItemSolicitud item) {
        if (item == null) {
            return null;
        }
        return DibujoResumenDTO.builder()
            .idItemSolicitud(item.getIdItemSolicitud())
            .nombre(item.getNombre())
            .orden(item.getOrden())
            .build();
    }
}
