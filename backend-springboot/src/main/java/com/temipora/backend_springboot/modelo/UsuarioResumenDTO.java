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
public class UsuarioResumenDTO {

    private Long idUsuario;
    private String nombreUsuario;
    private String nombreDisplay;
    private String biografia;
    private String nombrePais;
    private String urlFotoPerfil;
    private String urlFotoPerfilMiniatura;

    public static UsuarioResumenDTO desdeUsuario(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return UsuarioResumenDTO.builder()
            .idUsuario(usuario.getIdUsuario())
            .nombreUsuario(usuario.getNombreUsuario())
            .nombreDisplay(usuario.getNombreDisplay() != null && !usuario.getNombreDisplay().isBlank()
                ? usuario.getNombreDisplay()
                : usuario.getNombreUsuario())
            .biografia(usuario.getBiografia())
            .nombrePais(usuario.getPais() != null ? usuario.getPais().getNombre() : null)
            .urlFotoPerfil(usuario.getUrlFotoPerfil())
            .urlFotoPerfilMiniatura(usuario.getUrlFotoPerfilMiniatura())
            .build();
    }
}
