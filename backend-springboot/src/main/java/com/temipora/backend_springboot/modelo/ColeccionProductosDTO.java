package com.temipora.backend_springboot.modelo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

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
public class ColeccionProductosDTO {

    private boolean visible;
    private boolean esDuenio;
    private PaginaDTO<ColeccionProductoDTO> productos;

    @Builder.Default
    private List<FiltroGaleriaDTO> artistas = List.of();

    /**
     * Solo para el dueño: sugiere una vez hacer pública la colección. Una vez
     * vista, el control queda como toggle dentro de la colección.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean sugerenciaPendiente;
}
