package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
    private boolean requiereAdvertencia;

    @Builder.Default
    @Setter
    private List<EtiquetaDTO> etiquetas = List.of();

    public static ImagenPortfolioDTO desdeImagen(Imagen imagen) {
        return desdeImagen(imagen, false);
    }

    public static ImagenPortfolioDTO desdeImagen(Imagen imagen, boolean requiereAdvertencia) {
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
            .requiereAdvertencia(requiereAdvertencia)
            .build();
    }
}