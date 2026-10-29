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
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ReporteReseniaDTO;
import com.temipora.backend_springboot.modelo.ReporteReseniaRequest;
import com.temipora.backend_springboot.servicios.ReseniaModeracionService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/resenias")
public class ReseniaReporteController {

    private final ReseniaModeracionService moderacionService;
    private final UsuarioService usuarioService;

    public ReseniaReporteController(
            ReseniaModeracionService moderacionService,
            UsuarioService usuarioService) {
        this.moderacionService = moderacionService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/{idResenia}/reportes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReporteReseniaDTO> reportar(
            @PathVariable Long idResenia,
            @Valid @RequestBody ReporteReseniaRequest request,
            Authentication authentication) {
        ReporteReseniaDTO dto = moderacionService.reportar(
            idResenia, idAutenticado(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/reportes/mios")
    @PreAuthorize("isAuthenticated()")
    public List<Long> misReportes(Authentication authentication) {
        return moderacionService.reportesDelUsuario(
            idAutenticado(authentication));
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }
}
