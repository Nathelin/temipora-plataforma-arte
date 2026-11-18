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
public class ProductoImagenDTO {

    private Long idProductoImagen;
    private String url;
    private Integer orden;
    private String sha256;
}
