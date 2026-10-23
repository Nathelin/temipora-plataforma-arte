package com.temipora.backend_springboot.modelo;

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
public class ItemSolicitudDTO {

    private Long idItemSolicitud;
    private String nombre;
    private Integer orden;
    private Integer ordenPrioridad;
    private String estado;
    private List<RespuestaDTO> respuestas;

    public static ItemSolicitudDTO desdeItem(ItemSolicitud item) {
        return ItemSolicitudDTO.builder()
            .idItemSolicitud(item.getIdItemSolicitud())
            .nombre(item.getNombre())
            .orden(item.getOrden())
            .ordenPrioridad(item.getOrdenPrioridad())
            .estado(item.getEstado())
            .respuestas(item.getRespuestas().stream()
                .map(RespuestaDTO::desdeRespuesta)
                .toList())
            .build();
    }
}
