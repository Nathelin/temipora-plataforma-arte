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
public class MaterialDTO {

    private Long idMaterial;
    private String nombre;
    private String descripcion;
    private String observaciones;
    private boolean activo;
    private Long idTipoArte;
    private String nombreTipoArte;
    private Long idMedio;
    private String nombreMedio;

    public static MaterialDTO desdeMaterial(Material material) {
        return MaterialDTO.builder()
            .idMaterial(material.getIdMaterial())
            .nombre(material.getNombre())
            .descripcion(material.getDescripcion())
            .observaciones(material.getObservaciones())
            .activo(material.isActivo())
            .idTipoArte(material.getTipoArte() != null
                ? material.getTipoArte().getIdTipoArte() : null)
            .nombreTipoArte(material.getTipoArte() != null
                ? material.getTipoArte().getNombre() : null)
            .idMedio(material.getTipoArte() != null && material.getTipoArte().getMedio() != null
                ? material.getTipoArte().getMedio().getIdMedio() : null)
            .nombreMedio(material.getTipoArte() != null && material.getTipoArte().getMedio() != null
                ? material.getTipoArte().getMedio().getNombre() : null)
            .build();
    }
}
