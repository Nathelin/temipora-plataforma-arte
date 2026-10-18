package com.temipora.backend_springboot.controladores;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.TerminosDTO;
import com.temipora.backend_springboot.modelo.TerminosRequest;
import com.temipora.backend_springboot.servicios.TerminosService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios/{idUsuario}/terminos")
public class TerminosController {

    private final TerminosService terminosService;
    private final UsuarioService usuarioService;

    public TerminosController(TerminosService terminosService, UsuarioService usuarioService) {
        this.terminosService = terminosService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ARTISTA')")
    public ResponseEntity<TerminosDTO> obtener(
            @PathVariable Long idUsuario,
            Authentication authentication) {
        if (esOtroUsuario(idUsuario, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(terminosService.obtenerGlobal(idUsuario));
    }

    @PutMapping
    @PreAuthorize("hasRole('ARTISTA')")
    public ResponseEntity<TerminosDTO> guardar(
            @PathVariable Long idUsuario,
            @Valid @RequestBody TerminosRequest request,
            Authentication authentication) {
        if (esOtroUsuario(idUsuario, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(terminosService.guardarGlobal(idUsuario, request));
    }

    private boolean esOtroUsuario(Long idUsuario, Authentication authentication) {
        if (authentication == null) {
            return true;
        }
        return !authentication.getName().equalsIgnoreCase(usuarioService.nombreUsuario(idUsuario));
    }
}
