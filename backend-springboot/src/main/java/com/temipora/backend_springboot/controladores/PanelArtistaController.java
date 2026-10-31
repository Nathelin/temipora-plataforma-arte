package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.PanelArtistaResumenDTO;
import com.temipora.backend_springboot.servicios.PanelArtistaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@RestController
@RequestMapping("/api/panel-artista")
public class PanelArtistaController {

    private final PanelArtistaService panelArtistaService;
    private final UsuarioService usuarioService;

    public PanelArtistaController(
            PanelArtistaService panelArtistaService,
            UsuarioService usuarioService) {
        this.panelArtistaService = panelArtistaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ARTISTA')")
    public PanelArtistaResumenDTO resumen(Authentication authentication) {
        Long idArtista = usuarioService
            .getByNombreUsuario(authentication.getName())
            .getIdUsuario();
        return panelArtistaService.resumen(idArtista);
    }
}
