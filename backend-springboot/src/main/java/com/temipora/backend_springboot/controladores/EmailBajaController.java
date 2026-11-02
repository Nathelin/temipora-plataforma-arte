package com.temipora.backend_springboot.controladores;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.EmailPlantillaService;

@RestController
@RequestMapping("/api/emails")
public class EmailBajaController {

    private final UsuarioRepository usuarioRepository;
    private final EmailPlantillaService plantillaService;

    public EmailBajaController(
            UsuarioRepository usuarioRepository,
            EmailPlantillaService plantillaService) {
        this.usuarioRepository = usuarioRepository;
        this.plantillaService = plantillaService;
    }

    @GetMapping(value = "/baja/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> baja(@PathVariable String token) {
        Usuario usuario = usuarioRepository.findByTokenBajaEmail(token)
            .orElse(null);
        if (usuario == null) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_HTML)
                .body(plantillaService.bajaInvalida());
        }
        usuario.setRecibirEmailsPromocionales(false);
        usuarioRepository.save(usuario);
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(plantillaService.bajaConfirmada(nombre(usuario)));
    }

    private String nombre(Usuario usuario) {
        if (usuario.getNombreDisplay() != null
                && !usuario.getNombreDisplay().isBlank()) {
            return usuario.getNombreDisplay();
        }
        return usuario.getNombreUsuario() != null
            ? usuario.getNombreUsuario()
            : "";
    }
}
