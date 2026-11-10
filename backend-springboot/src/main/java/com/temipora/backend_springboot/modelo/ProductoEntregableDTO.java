package com.temipora.backend_springboot.modelo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Archivo que recibe el comprador. Hoy hay exactamente uno por variante y es
 * su imagen original; el modelo deja lugar a varios (bundles, packs) sin
 * cambiar el contrato.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoEntregableDTO {

    private Long idProducto;
    private Long idVariante;
    private String nombreVariante;
    private String nombre;
    private String mime;
    private Long tamanoBytes;
    private String sha256;
    private Integer orden;
}
