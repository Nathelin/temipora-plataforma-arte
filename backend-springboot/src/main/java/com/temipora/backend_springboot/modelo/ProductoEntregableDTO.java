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
public class ProductoEntregableDTO {

    private Long idEntregable;
    private AlcanceEntregable alcance;
    private Long idVariante;
    private String nombreVariante;
    private String nombre;
    private String mime;
    private Long tamanoBytes;
    private String sha256;
    private Integer orden;
}
