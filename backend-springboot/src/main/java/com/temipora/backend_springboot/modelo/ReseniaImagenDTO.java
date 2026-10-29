package com.temipora.backend_springboot.modelo;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReseniaImagenDTO {

    private Long idArchivo;
    private String urlPreview;
    private Integer ancho;
    private Integer alto;

    public static ReseniaImagenDTO desdeImagen(ReseniaImagen imagen) {
        EntregaArchivo archivo = imagen.getArchivo();
        return ReseniaImagenDTO.builder()
            .idArchivo(archivo.getIdArchivo())
            .urlPreview("/resenias/imagenes/"
                + archivo.getIdArchivo() + "/preview")
            .ancho(archivo.getAncho())
            .alto(archivo.getAlto())
            .build();
    }
}
