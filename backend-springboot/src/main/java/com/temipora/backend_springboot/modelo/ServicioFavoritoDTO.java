package com.temipora.backend_springboot.modelo;

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
public class ServicioFavoritoDTO {

    private Long idServicio;
    private LocalDateTime fechaAgregado;
    private boolean disponible;
    private ServicioDTO servicio;

    public static ServicioFavoritoDTO desde(
            ServicioFavorito favorito,
            ServicioDTO servicio,
            boolean disponible) {
        return ServicioFavoritoDTO.builder()
            .idServicio(favorito.getId().getIdServicio())
            .fechaAgregado(favorito.getFechaAgregado())
            .disponible(disponible)
            .servicio(servicio)
            .build();
    }
}
