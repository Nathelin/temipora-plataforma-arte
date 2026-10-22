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
public class ArtistaFavoritoDTO {

    private Long idUsuario;
    private String nombreUsuario;
    private String nombreDisplay;
    private String biografia;
    private String urlFotoPerfil;
    private String urlFotoPerfilMiniatura;
    private LocalDateTime fechaAgregado;

    private String tipoArtePrincipal;
    private String medioPrincipal;

    @Builder.Default
    private List<String> etiquetasDestacadas = List.of();

    @Builder.Default
    private List<String> tiposServicioDestacados = List.of();

    private int cantidadImagenes;
    private int cantidadServicios;

    public static ArtistaFavoritoDTO desde(ArtistaFavorito favorito, PerfilArtisticoDTO perfil) {
        Usuario artista = favorito.getArtista();
        PerfilArtisticoDTO datos = perfil != null ? perfil : new PerfilArtisticoDTO();

        return ArtistaFavoritoDTO.builder()
            .idUsuario(artista.getIdUsuario())
            .nombreUsuario(artista.getNombreUsuario())
            .nombreDisplay(artista.getNombreDisplay() != null && !artista.getNombreDisplay().isBlank()
                ? artista.getNombreDisplay()
                : artista.getNombreUsuario())
            .biografia(artista.getBiografia())
            .urlFotoPerfil(artista.getUrlFotoPerfil())
            .urlFotoPerfilMiniatura(artista.getUrlFotoPerfilMiniatura())
            .fechaAgregado(favorito.getFechaAgregado())
            .tipoArtePrincipal(datos.getTipoArtePrincipal())
            .medioPrincipal(datos.getMedioPrincipal())
            .etiquetasDestacadas(datos.getEtiquetasDestacadas())
            .tiposServicioDestacados(datos.getTiposServicioDestacados())
            .cantidadImagenes(datos.getCantidadImagenes())
            .cantidadServicios(datos.getCantidadServicios())
            .build();
    }
}
