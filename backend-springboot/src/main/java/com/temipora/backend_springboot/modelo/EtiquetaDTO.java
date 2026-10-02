package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

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
public class EtiquetaDTO {

    private Long idEtiqueta;
    private String nombre;
    private String descripcion;
    private String observaciones;
    private Long idCategoria;
    private String nombreCategoria;
    private EtiquetaEstado estado;
    private int vecesSolicitada;
    private String creadoPor;
    private String evaluadoPor;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaEvaluacion;

    public static EtiquetaDTO desdeEtiqueta(Etiqueta etiqueta) {
        return EtiquetaDTO.builder()
            .idEtiqueta(etiqueta.getIdEtiqueta())
            .nombre(etiqueta.getNombre())
            .descripcion(etiqueta.getDescripcion())
            .observaciones(etiqueta.getObservaciones())
            .idCategoria(etiqueta.getCategoria().getIdCategoriaEtiqueta())
            .nombreCategoria(etiqueta.getCategoria().getNombre())
            .estado(etiqueta.getEstado())
            .vecesSolicitada(etiqueta.getVecesSolicitada())
            .creadoPor(etiqueta.getCreadoPor() != null ? etiqueta.getCreadoPor().getNombreUsuario() : null)
            .evaluadoPor(etiqueta.getEvaluadoPor() != null ? etiqueta.getEvaluadoPor().getNombreUsuario() : null)
            .fechaCreacion(etiqueta.getFechaCreacion())
            .fechaEvaluacion(etiqueta.getFechaEvaluacion())
            .build();
    }
}
