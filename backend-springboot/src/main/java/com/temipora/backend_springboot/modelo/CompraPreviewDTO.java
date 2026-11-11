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
public class CompraPreviewDTO {

    private Long idCompraItem;
    private Long idProducto;
    private Long idVariante;
    private String tituloProducto;
    private String nombreVariante;
    private String imagenUrl;
    private int cantidad;
    private boolean puedeDescargar;
    private String motivoNoDescarga;
}
