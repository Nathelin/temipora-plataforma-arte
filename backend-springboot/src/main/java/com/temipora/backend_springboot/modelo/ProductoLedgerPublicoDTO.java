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
public class ProductoLedgerPublicoDTO {

    private Long idProducto;
    private String titulo;
    private boolean integridad;
    private String mensaje;

    @Builder.Default
    private List<VarianteLedgerPublicoDTO> variantes = new ArrayList<>();

    private int pagina;
    private int tamano;
    private long totalElementos;
    private int totalPaginas;
}
