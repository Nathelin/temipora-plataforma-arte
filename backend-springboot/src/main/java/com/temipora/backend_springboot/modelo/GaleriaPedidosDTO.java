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
public class GaleriaPedidosDTO {

    private boolean visible;
    private boolean esDuenio;
    private PaginaDTO<GaleriaPiezaDTO> piezas;

    @Builder.Default
    private List<FiltroGaleriaDTO> servicios = List.of();

    @Builder.Default
    private List<FiltroGaleriaDTO> artistas = List.of();
}
