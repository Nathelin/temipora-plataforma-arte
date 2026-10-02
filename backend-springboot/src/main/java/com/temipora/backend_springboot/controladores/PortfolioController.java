package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import com.temipora.backend_springboot.modelo.FiltroPortfolioRequest;
import com.temipora.backend_springboot.modelo.ImagenPortfolioDTO;
import com.temipora.backend_springboot.modelo.ImagenPortfolioRequest;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.PortfolioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final UsuarioRepository usuarioRepository;

    public PortfolioController(PortfolioService portfolioService, UsuarioRepository usuarioRepository) {
        this.portfolioService = portfolioService;
        this.usuarioRepository = usuarioRepository;
    }

    // Endpoint original (sin paginación, para compatibilidad)
    @GetMapping("/{nombreUsuario}")
    public List<ImagenPortfolioDTO> listar(@PathVariable String nombreUsuario) {
        return portfolioService.listar(nombreUsuario);
    }

    // Nuevo endpoint con paginación y filtrado por preferencias del visitante
    @GetMapping("/{nombreUsuario}/paginado")
    public Page<ImagenPortfolioDTO> listarPaginado(
            @PathVariable String nombreUsuario,
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) Long idEtiqueta,
            @RequestParam(required = false) String medio,
            @RequestParam(required = false) String sensible,
            @RequestParam(required = false) String orden,
            Pageable pageable,
            Authentication authentication) {

        Long viewerId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            viewerId = usuarioRepository.findByNombreUsuarioIgnoreCase(username)
                .map(u -> u.getIdUsuario())
                .orElse(null);
        }

        FiltroPortfolioRequest filtro = new FiltroPortfolioRequest();
        filtro.setTitulo(titulo);
        filtro.setIdEtiqueta(idEtiqueta);
        filtro.setMedio(medio);
        filtro.setSensible(sensible);
        filtro.setOrden(orden);

        return portfolioService.listarParaVisorPorNombreUsuario(nombreUsuario, viewerId, filtro, pageable);
    }

    @GetMapping("/{nombreUsuario}/etiquetas")
    public List<EtiquetaDTO> etiquetasDelPortfolio(@PathVariable String nombreUsuario) {
        return portfolioService.etiquetasDePortfolio(nombreUsuario);
    }

    @PostMapping("/usuarios/{idUsuario}/imagenes")
    @PreAuthorize("hasRole('ARTISTA')")
    public ResponseEntity<ImagenPortfolioDTO> crear(
            @PathVariable Long idUsuario,
            @Valid @RequestBody ImagenPortfolioRequest request,
            Authentication authentication) {
        if (!authentication.getName().equalsIgnoreCase(
                portfolioService.nombreUsuario(idUsuario))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(portfolioService.crear(idUsuario, request));
    }

    @PutMapping("/usuarios/{idUsuario}/imagenes/{idImagen}")
    @PreAuthorize("hasRole('ARTISTA')")
    public ResponseEntity<ImagenPortfolioDTO> modificar(
            @PathVariable Long idUsuario,
            @PathVariable Long idImagen,
            @Valid @RequestBody ImagenPortfolioRequest request,
            Authentication authentication) {
        if (!authentication.getName().equalsIgnoreCase(
                portfolioService.nombreUsuario(idUsuario))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(portfolioService.modificar(idUsuario, idImagen, request));
    }

    @PostMapping("/usuarios/{idUsuario}/imagenes/{idImagen}/baja")
    @PreAuthorize("hasRole('ARTISTA')")
    public ResponseEntity<ImagenPortfolioDTO> darDeBaja(
            @PathVariable Long idUsuario,
            @PathVariable Long idImagen,
            Authentication authentication) {
        if (!authentication.getName().equalsIgnoreCase(
                portfolioService.nombreUsuario(idUsuario))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(portfolioService.darDeBaja(idUsuario, idImagen));
    }
}