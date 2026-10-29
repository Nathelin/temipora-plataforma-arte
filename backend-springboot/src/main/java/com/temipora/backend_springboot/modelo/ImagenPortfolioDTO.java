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
    private TipoArteDTO tipoArte;
    private ContenidoSensible contenidoSensible;
    private boolean requiereAdvertencia;

    @Setter
    private boolean verificada;

    @Setter
    private String servicioNombre;

    @Setter
    private Long idServicio;

    @Setter
    private boolean servicioDisponible;

    @Builder.Default
    @Setter
    private List<EtiquetaDTO> etiquetas = List.of();

    @Builder.Default
    @Setter
    private List<MaterialDTO> materiales = List.of();

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
            .tipoArte(TipoArteDTO.desdeTipoArte(imagen.getTipoArte()))
            .contenidoSensible(imagen.getContenidoSensible())
            .requiereAdvertencia(requiereAdvertencia)
            .verificada(false)
            .build();
    }
}