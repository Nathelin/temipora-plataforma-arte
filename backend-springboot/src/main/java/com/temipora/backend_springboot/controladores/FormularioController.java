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

import com.temipora.backend_springboot.modelo.FormularioDTO;
import com.temipora.backend_springboot.modelo.FormularioRequest;
import com.temipora.backend_springboot.servicios.FormularioService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios/{idUsuario}/formulario-pedidos")
public class FormularioController {

    private final FormularioService formularioService;
    private final UsuarioService usuarioService;

    public FormularioController(FormularioService formularioService, UsuarioService usuarioService) {
        this.formularioService = formularioService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ARTISTA')")
    public ResponseEntity<FormularioDTO> obtener(
            @PathVariable Long idUsuario,
            Authentication authentication) {
        if (esOtroUsuario(idUsuario, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(formularioService.obtenerGlobal(idUsuario));
    }

    @PutMapping
    @PreAuthorize("hasRole('ARTISTA')")
    public ResponseEntity<FormularioDTO> guardar(
            @PathVariable Long idUsuario,
            @Valid @RequestBody FormularioRequest request,
            Authentication authentication) {
        if (esOtroUsuario(idUsuario, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(formularioService.guardarGlobal(idUsuario, request));
    }

    private boolean esOtroUsuario(Long idUsuario, Authentication authentication) {
        if (authentication == null) {
            return true;
        }
        return !authentication.getName().equalsIgnoreCase(usuarioService.nombreUsuario(idUsuario));
    }
}
