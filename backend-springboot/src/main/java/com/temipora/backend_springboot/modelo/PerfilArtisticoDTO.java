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
public class PerfilArtisticoDTO {

    private String tipoArtePrincipal;
    private String medioPrincipal;

    @Builder.Default
    private List<String> etiquetasDestacadas = List.of();

    @Builder.Default
    private List<String> tiposServicioDestacados = List.of();

    private int cantidadImagenes;
    private int cantidadServicios;
}
