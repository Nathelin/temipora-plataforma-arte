package com.temipora.backend_springboot.controladores;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.TerminosPlataformaAceptacionRequest;
import com.temipora.backend_springboot.modelo.TerminosPlataformaDTO;
import com.temipora.backend_springboot.modelo.TerminosPlataformaEstadoDTO;
import com.temipora.backend_springboot.servicios.TerminosPlataformaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/terminos-plataforma")
public class TerminosPlataformaController {

    private final TerminosPlataformaService terminosPlataformaService;
    private final UsuarioService usuarioService;

    public TerminosPlataformaController(
            TerminosPlataformaService terminosPlataformaService,
            UsuarioService usuarioService) {
        this.terminosPlataformaService = terminosPlataformaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public TerminosPlataformaDTO vigente() {
        return terminosPlataformaService.obtenerVigente();
    }

    @GetMapping("/estado")
    public TerminosPlataformaEstadoDTO estado(Authentication authentication) {
        return terminosPlataformaService.obtenerEstadoUsuario(
            idAutenticado(authentication));
    }

    @PostMapping("/aceptar")
    public TerminosPlataformaEstadoDTO aceptar(
            @Valid @RequestBody TerminosPlataformaAceptacionRequest request,
            Authentication authentication) {
        return terminosPlataformaService.aceptar(
            idAutenticado(authentication), request.getVersion());
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }
}
