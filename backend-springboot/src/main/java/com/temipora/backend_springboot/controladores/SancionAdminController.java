package com.temipora.backend_springboot.controladores;

import java.util.List;
import java.util.NoSuchElementException;

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

import com.temipora.backend_springboot.modelo.EstadoSancionDTO;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.RevertirSancionRequest;
import com.temipora.backend_springboot.modelo.SancionCuentaDTO;
import com.temipora.backend_springboot.modelo.SancionRequest;
import com.temipora.backend_springboot.modelo.TipoSancion;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.SancionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/sanciones")
@PreAuthorize("hasRole('ADMIN')")
public class SancionAdminController {

    private final SancionService sancionService;
    private final UsuarioRepository usuarioRepository;

    public SancionAdminController(
            SancionService sancionService,
            UsuarioRepository usuarioRepository) {
        this.sancionService = sancionService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public PaginaDTO<SancionCuentaDTO> listar(
            @RequestParam(required = false) Long idUsuario,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) TipoSancion tipo,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        return sancionService.listar(idUsuario, texto, tipo, estado, pagina, tamanio);
    }

    @GetMapping("/usuario/{idUsuario}")
    public List<SancionCuentaDTO> historial(@PathVariable Long idUsuario) {
        return sancionService.historial(idUsuario);
    }

    @GetMapping("/usuario/{idUsuario}/estado")
    public EstadoSancionDTO estado(@PathVariable Long idUsuario) {
        return sancionService.estadoUsuario(idUsuario);
    }

    @PostMapping
    public ResponseEntity<SancionCuentaDTO> aplicar(
            @Valid @RequestBody SancionRequest request,
            Authentication authentication) {
        Usuario admin = usuarioActual(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(sancionService.aplicar(admin.getIdUsuario(), request));
    }

    @PostMapping("/{id}/revertir")
    public SancionCuentaDTO revertir(
            @PathVariable Long id,
            @RequestBody(required = false) RevertirSancionRequest request,
            Authentication authentication) {
        Usuario admin = usuarioActual(authentication);
        String observacion = request != null ? request.getObservacion() : null;
        return sancionService.revertir(admin.getIdUsuario(), id, observacion);
    }

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioRepository.findByNombreUsuarioIgnoreCase(authentication.getName())
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + authentication.getName()));
    }
}
