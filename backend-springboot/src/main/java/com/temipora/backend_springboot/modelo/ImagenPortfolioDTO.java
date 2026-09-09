package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImagenPortfolioDTO {

    private Long idImagen;
    private String url;
    private boolean estado;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaSubida;
    private MedioPortfolio medio;
    private String materiales;
    private ContenidoSensible contenidoSensible;

    public static ImagenPortfolioDTO desdeImagen(Imagen imagen) {
        return ImagenPortfolioDTO.builder()
            .idImagen(imagen.getIdImagen())
            .url(imagen.getUrl())
            .estado(imagen.isEstado())
            .titulo(imagen.getTitulo())
            .descripcion(imagen.getDescripcion())
            .fechaSubida(imagen.getFechaSubida())
            .medio(imagen.getMedio())
            .materiales(imagen.getMateriales())
            .contenidoSensible(imagen.getContenidoSensible())
            .build();
    }
}
