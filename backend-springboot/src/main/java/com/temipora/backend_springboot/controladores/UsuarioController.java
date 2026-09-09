package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ActualizarPerfilDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.UsuarioDTO;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UsuarioDTO> obtenerUsuarios() {
        return usuarioService.obtenerTodosLosUsuarios();
    }

    @GetMapping("/perfil/{nombreUsuario}")
    public UsuarioDTO obtenerPerfil(@PathVariable String nombreUsuario) {
        Usuario usuario = usuarioService.getByNombreUsuario(nombreUsuario);
        return UsuarioDTO.desdeUsuario(usuario);
    }

    @PutMapping("/usuarios/{idUsuario}/perfil")
    public UsuarioDTO actualizarPerfil(
            @PathVariable Long idUsuario,
            @Valid @RequestBody ActualizarPerfilDTO dto) {
        return usuarioService.actualizarPerfil(idUsuario, dto);
    }
}