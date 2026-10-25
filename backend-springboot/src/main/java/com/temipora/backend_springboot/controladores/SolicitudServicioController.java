package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.SolicitudDTO;
import com.temipora.backend_springboot.modelo.SolicitudRechazoRequest;
import com.temipora.backend_springboot.modelo.SolicitudResumenDTO;
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
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public ResponseEntity<SolicitudDTO> crear(
            @Valid @RequestBody SolicitudServicioRequest request,
            Authentication authentication) {
        Long idCliente = idAutenticado(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(solicitudServicioService.crear(idCliente, request));
    }

    @GetMapping("/recibidas")
    @PreAuthorize("hasRole('ARTISTA')")
    public List<SolicitudResumenDTO> listarRecibidas(
            @RequestParam(required = false) EstadoSolicitud estado,
            Authentication authentication) {
        return solicitudServicioService.listarRecibidas(idAutenticado(authentication), estado);
    }

    @GetMapping("/enviadas")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public List<SolicitudResumenDTO> listarEnviadas(
            @RequestParam(required = false) EstadoSolicitud estado,
            Authentication authentication) {
        return solicitudServicioService.listarEnviadas(idAutenticado(authentication), estado);
    }

    @GetMapping("/{idSolicitud}")
    @PreAuthorize("isAuthenticated()")
    public SolicitudDTO obtenerSolicitud(
            @PathVariable Long idSolicitud,
            Authentication authentication) {
        return solicitudServicioService.obtenerSolicitud(
            idSolicitud, idAutenticado(authentication));
    }

    @PostMapping("/{idSolicitud}/rechazar")
    @PreAuthorize("hasRole('ARTISTA')")
    public SolicitudDTO rechazar(
            @PathVariable Long idSolicitud,
            @Valid @RequestBody(required = false) SolicitudRechazoRequest request,
            Authentication authentication) {
        String motivo = request != null ? request.getMotivo() : null;
        return solicitudServicioService.rechazar(
            idSolicitud, idAutenticado(authentication), motivo);
    }

    @PostMapping("/{idSolicitud}/retirar-presupuesto")
    @PreAuthorize("hasRole('ARTISTA')")
    public SolicitudDTO retirarPresupuesto(
            @PathVariable Long idSolicitud,
            Authentication authentication) {
        return solicitudServicioService.retirarPresupuesto(
            idSolicitud, idAutenticado(authentication));
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName()).getIdUsuario();
    }
}
