package com.temipora.backend_springboot.controladores;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.EtiquetaDTO;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;
import com.temipora.backend_springboot.modelo.EtiquetaRequest;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.EtiquetaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/etiquetas")
@PreAuthorize("hasRole('ADMIN')")
public class EtiquetaAdminController {

    private final EtiquetaService etiquetaService;
    private final UsuarioRepository usuarioRepository;

    public EtiquetaAdminController(EtiquetaService etiquetaService, UsuarioRepository usuarioRepository) {
        this.etiquetaService = etiquetaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public PaginaDTO<EtiquetaDTO> listar(
            @RequestParam(required = false) EtiquetaEstado estado,
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        return etiquetaService.listarAdmin(estado, idCategoria, pagina, tamanio);
    }

    @GetMapping("/conteo-pendientes")
    public Map<String, Long> conteoPendientes() {
        return Map.of("pendientes", etiquetaService.contarPendientes());
    }

    @PostMapping
    public ResponseEntity<EtiquetaDTO> crear(
            @Valid @RequestBody EtiquetaRequest request,
            Authentication authentication) {
        Usuario admin = usuarioActual(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(etiquetaService.crearDesdeAdmin(admin, request));
    }

    @PutMapping("/{id}")
    public EtiquetaDTO modificar(
            @PathVariable Long id,
            @Valid @RequestBody EtiquetaRequest request) {
        return etiquetaService.modificar(id, request);
    }

    @PostMapping("/{id}/aprobar")
    public EtiquetaDTO aprobar(@PathVariable Long id, Authentication authentication) {
        return etiquetaService.aprobar(usuarioActual(authentication), id);
    }

    @PostMapping("/{id}/rechazar")
    public EtiquetaDTO rechazar(@PathVariable Long id, Authentication authentication) {
        return etiquetaService.rechazar(usuarioActual(authentication), id);
    }

    @PostMapping("/{id}/baja")
    public EtiquetaDTO darDeBaja(@PathVariable Long id) {
        return etiquetaService.darDeBaja(id);
    }

    @PostMapping("/{id}/reactivar")
    public EtiquetaDTO reactivar(@PathVariable Long id, Authentication authentication) {
        return etiquetaService.reactivar(usuarioActual(authentication), id);
    }

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioRepository.findByNombreUsuarioIgnoreCase(authentication.getName())
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + authentication.getName()));
    }
}
