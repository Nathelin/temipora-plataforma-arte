package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
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
public class ReseniaVersionDTO {

    private int numero;
    private String descripcion;
    private int calificacionGeneral;
    private LocalDateTime fechaVersion;
    private boolean actual;
    private List<ReseniaValorDTO> valores;
    private List<ReseniaOpcionDTO> opciones;
    private List<ReseniaImagenDTO> imagenes;

    public static ReseniaVersionDTO desdeVersion(ReseniaVersion version, boolean actual) {
        return ReseniaVersionDTO.builder()
            .numero(version.getNumero())
            .descripcion(version.getDescripcion())
            .calificacionGeneral(version.getCalificacionGeneral())
            .fechaVersion(version.getFechaVersion())
            .actual(actual)
            .valores(version.getValores().stream()
                .map(ReseniaValorDTO::desdeValor)
                .toList())
            .opciones(version.getOpciones().stream()
                .map(ReseniaOpcionDTO::desdeOpcion)
                .toList())
            .imagenes(version.getImagenes().stream()
                .map(ReseniaImagenDTO::desdeImagen)
                .toList())
            .build();
    }
}
