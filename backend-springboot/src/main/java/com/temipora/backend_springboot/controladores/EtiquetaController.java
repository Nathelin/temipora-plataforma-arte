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

import com.temipora.backend_springboot.modelo.CategoriaEtiquetaDTO;
import com.temipora.backend_springboot.modelo.EtiquetaDTO;
import com.temipora.backend_springboot.modelo.EtiquetaRequest;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.EtiquetaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/etiquetas")
public class EtiquetaController {

    private final EtiquetaService etiquetaService;
    private final UsuarioRepository usuarioRepository;

    public EtiquetaController(EtiquetaService etiquetaService, UsuarioRepository usuarioRepository) {
        this.etiquetaService = etiquetaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/selector")
    public List<EtiquetaDTO> selector(Authentication authentication) {
        return etiquetaService.selector(usuarioActual(authentication));
    }

    @GetMapping("/categorias")
    public List<CategoriaEtiquetaDTO> categorias() {
        return etiquetaService.categoriasActivas();
    }

    @GetMapping("/buscar")
    public ResponseEntity<EtiquetaDTO> buscar(@RequestParam String nombre) {
        return etiquetaService.buscar(nombre)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/mis")
    public List<EtiquetaDTO> misEtiquetas(Authentication authentication) {
        return etiquetaService.misEtiquetas(usuarioActual(authentication));
    }

    @PostMapping
    @PreAuthorize("hasRole('ARTISTA')")
    public ResponseEntity<EtiquetaDTO> crear(
            @Valid @RequestBody EtiquetaRequest request,
            Authentication authentication) {
        EtiquetaDTO dto = etiquetaService.crearDesdeArtista(usuarioActual(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/{id}/re-solicitar")
    @PreAuthorize("hasRole('ARTISTA')")
    public EtiquetaDTO reSolicitar(@PathVariable Long id, Authentication authentication) {
        return etiquetaService.reSolicitar(usuarioActual(authentication), id);
    }

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioRepository.findByNombreUsuarioIgnoreCase(authentication.getName())
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + authentication.getName()));
    }
}
