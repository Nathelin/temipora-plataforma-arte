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
public class TipoArteDTO {

    private Long idTipoArte;
    private String nombre;
    private String descripcion;
    private Long idMedio;
    private String nombreMedio;
    private String observaciones;
    private boolean activo;

    public static TipoArteDTO desdeTipoArte(TipoArte tipoArte) {
        return TipoArteDTO.builder()
            .idTipoArte(tipoArte.getIdTipoArte())
            .nombre(tipoArte.getNombre())
            .descripcion(tipoArte.getDescripcion())
            .idMedio(tipoArte.getMedio() != null ? tipoArte.getMedio().getIdMedio() : null)
            .nombreMedio(tipoArte.getMedio() != null ? tipoArte.getMedio().getNombre() : null)
            .observaciones(tipoArte.getObservaciones())
            .activo(tipoArte.isActivo())
            .build();
    }
}
