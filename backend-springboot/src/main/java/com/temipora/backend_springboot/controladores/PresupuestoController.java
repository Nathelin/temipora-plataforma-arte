package com.temipora.backend_springboot.controladores;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.PresupuestoRequest;
import com.temipora.backend_springboot.modelo.SolicitudDTO;
import com.temipora.backend_springboot.servicios.PresupuestoService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/solicitudes-servicio/{idSolicitud}/presupuesto")
public class PresupuestoController {

    private final PresupuestoService presupuestoService;
    private final UsuarioService usuarioService;

    public PresupuestoController(
            PresupuestoService presupuestoService,
            UsuarioService usuarioService) {
        this.presupuestoService = presupuestoService;
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ARTISTA')")
    public ResponseEntity<SolicitudDTO> crear(
            @PathVariable Long idSolicitud,
            @Valid @RequestBody PresupuestoRequest request,
            Authentication authentication) {
        Long idArtista = usuarioService
            .getByNombreUsuario(authentication.getName())
            .getIdUsuario();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(presupuestoService.crear(idSolicitud, idArtista, request));
    }
}
