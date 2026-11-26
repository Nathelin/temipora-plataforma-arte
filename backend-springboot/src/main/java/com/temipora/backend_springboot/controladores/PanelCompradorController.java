package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.PanelCompradorResumenDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.PanelCompradorService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@RestController
@RequestMapping("/api/panel-comprador")
public class PanelCompradorController {

    private final PanelCompradorService panelCompradorService;
    private final UsuarioService usuarioService;

    public PanelCompradorController(
            PanelCompradorService panelCompradorService,
            UsuarioService usuarioService) {
        this.panelCompradorService = panelCompradorService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public PanelCompradorResumenDTO resumen(Authentication authentication) {
        Usuario usuario = usuarioService.getByNombreUsuario(
            authentication.getName());
        return panelCompradorService.resumen(
            usuario.getIdUsuario(), usuario.getNombreUsuario());
    }
}
