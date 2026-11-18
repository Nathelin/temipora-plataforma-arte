package com.temipora.backend_springboot.modelo;

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
public class PaginaReseniasDTO {

    private ResumenReseniasDTO resumen;
    private PaginaDTO<ReseniaPublicaDTO> pagina;
    private List<FiltroReseniaServicioDTO> servicios;

    @Builder.Default
    private List<FiltroReseniaProductoDTO> productos = new ArrayList<>();
}
