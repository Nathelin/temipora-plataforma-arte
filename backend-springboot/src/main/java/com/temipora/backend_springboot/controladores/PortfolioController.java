package com.temipora.backend_springboot.controladores;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import com.temipora.backend_springboot.modelo.ImagenPortfolioVerificadaRequest;
import com.temipora.backend_springboot.modelo.PortfolioEntregaVerificada;
import com.temipora.backend_springboot.modelo.VerificacionPortfolioDTO;
import com.temipora.backend_springboot.repositorios.PortfolioEntregaVerificadaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.ImagenGaleriaService;
import com.temipora.backend_springboot.servicios.PortfolioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final UsuarioRepository usuarioRepository;
    private final PortfolioEntregaVerificadaRepository verificacionRepository;
    private final ImagenGaleriaService imagenGaleriaService;

    public PortfolioController(
            PortfolioService portfolioService,
            UsuarioRepository usuarioRepository,
            PortfolioEntregaVerificadaRepository verificacionRepository,
            ImagenGaleriaService imagenGaleriaService) {
        this.portfolioService = portfolioService;
        this.usuarioRepository = usuarioRepository;
        this.verificacionRepository = verificacionRepository;
        this.imagenGaleriaService = imagenGaleriaService;
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
            @RequestParam(required = false) Long idTipoArte,
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
        filtro.setIdTipoArte(idTipoArte);
        filtro.setSensible(sensible);
        filtro.setOrden(orden);

        return portfolioService.listarParaVisorPorNombreUsuario(nombreUsuario, viewerId, filtro, pageable);
    }

    @GetMapping("/{nombreUsuario}/etiquetas")
    public List<EtiquetaDTO> etiquetasDelPortfolio(@PathVariable String nombreUsuario) {
        return portfolioService.etiquetasDePortfolio(nombreUsuario);
    }

    @GetMapping("/{nombreUsuario}/verificadas")
    public List<VerificacionPortfolioDTO> listarVerificadas(
            @PathVariable String nombreUsuario) {
        return portfolioService.listarVerificadas(nombreUsuario);
    }

    @GetMapping("/verificadas/archivos/{idArchivo}/preview")
    public ResponseEntity<Resource> previewVerificada(
            @PathVariable Long idArchivo) {
        PortfolioEntregaVerificada verificacion = verificacionRepository
            .buscarActivaPorArchivo(idArchivo)
            .orElseThrow(() -> new NoSuchElementException(
                "Imagen no disponible."));

        ContentDisposition disposition = ContentDisposition.inline()
            .filename(
                "portfolio-" + idArchivo + ".webp",
                StandardCharsets.UTF_8)
            .build();

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("image/webp"))
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(imagenGaleriaService.abrirLimpia(
                verificacion.getArchivoEntrega()));
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

    @PostMapping("/usuarios/{idUsuario}/imagenes/verificadas")
    @PreAuthorize("hasRole('ARTISTA')")
    public ResponseEntity<ImagenPortfolioDTO> crearVerificada(
            @PathVariable Long idUsuario,
            @Valid @RequestBody ImagenPortfolioVerificadaRequest request,
            Authentication authentication) {
        if (!authentication.getName().equalsIgnoreCase(
                portfolioService.nombreUsuario(idUsuario))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(portfolioService.crearDesdeEntrega(idUsuario, request));
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