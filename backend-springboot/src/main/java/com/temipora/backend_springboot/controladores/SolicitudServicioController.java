package com.temipora.backend_springboot.controladores;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.SolicitudDTO;
import com.temipora.backend_springboot.modelo.SolicitudServicioRequest;
import com.temipora.backend_springboot.servicios.SolicitudServicioService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/solicitudes-servicio")
public class SolicitudServicioController {

    private final SolicitudServicioService solicitudServicioService;
    private final UsuarioService usuarioService;

    public SolicitudServicioController(
            SolicitudServicioService solicitudServicioService,
            UsuarioService usuarioService) {
        this.solicitudServicioService = solicitudServicioService;
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<SolicitudDTO> crear(
            @Valid @RequestBody SolicitudServicioRequest request,
            Authentication authentication) {
        Long idCliente = idAutenticado(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(solicitudServicioService.crear(idCliente, request));
    }

    @GetMapping("/{idSolicitud}")
    @PreAuthorize("isAuthenticated()")
    public SolicitudDTO obtenerSolicitud(
            @PathVariable Long idSolicitud,
            Authentication authentication) {
        return solicitudServicioService.obtenerSolicitud(
            idSolicitud, idAutenticado(authentication));
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName()).getIdUsuario();
    }
}
