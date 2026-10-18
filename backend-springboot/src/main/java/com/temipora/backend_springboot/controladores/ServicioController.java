package com.temipora.backend_springboot.controladores;

import java.math.BigDecimal;
import java.util.List;

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

import com.temipora.backend_springboot.modelo.FiltroServicioRequest;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.ServicioRequest;
import com.temipora.backend_springboot.servicios.ServicioService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ServicioController {

    private final ServicioService servicioService;
    private final UsuarioService usuarioService;

    public ServicioController(ServicioService servicioService, UsuarioService usuarioService) {
        this.servicioService = servicioService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/mis-servicios")
    @PreAuthorize("hasRole('ARTISTA')")
    public List<ServicioDTO> misServicios(Authentication authentication) {
        return servicioService.listarMios(idAutenticado(authentication));
    }

    @GetMapping("/servicios")
    public List<ServicioDTO> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String nombreArtista,
            @RequestParam(required = false) Long idTipoServicio,
            @RequestParam(required = false) Boolean sensible,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) Integer tiempoMaxDias,
            Authentication authentication) {
        FiltroServicioRequest filtro = new FiltroServicioRequest();
        filtro.setNombre(nombre);
        filtro.setNombreArtista(nombreArtista);
        filtro.setIdTipoServicio(idTipoServicio);
        filtro.setSensible(sensible);
        filtro.setPrecioMin(precioMin);
        filtro.setPrecioMax(precioMax);
        filtro.setTiempoMaxDias(tiempoMaxDias);
        return servicioService.listarActivos(filtro, idVisor(authentication));
    }

    @GetMapping("/servicios/artista/{nombreUsuario}")
    public List<ServicioDTO> listarPorArtista(
            @PathVariable String nombreUsuario,
            Authentication authentication) {
        return servicioService.listarActivosDeArtista(nombreUsuario, idVisor(authentication));
    }

    @GetMapping("/servicios/{id}")
    public ServicioDTO obtener(@PathVariable Long id, Authentication authentication) {
        return servicioService.obtener(id, idVisor(authentication));
    }

    @PostMapping("/servicios")
    @PreAuthorize("hasRole('ARTISTA')")
    public ResponseEntity<ServicioDTO> crear(
            @Valid @RequestBody ServicioRequest request,
            Authentication authentication) {
        ServicioDTO creado = servicioService.crear(idAutenticado(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/servicios/{id}")
    @PreAuthorize("hasRole('ARTISTA')")
    public ServicioDTO modificar(
            @PathVariable Long id,
            @Valid @RequestBody ServicioRequest request,
            Authentication authentication) {
        return servicioService.modificar(id, idAutenticado(authentication), request);
    }

    @PostMapping("/servicios/{id}/baja")
    @PreAuthorize("hasRole('ARTISTA')")
    public ServicioDTO darDeBaja(@PathVariable Long id, Authentication authentication) {
        return servicioService.darDeBaja(id, idAutenticado(authentication));
    }

    @PostMapping("/servicios/{id}/activar")
    @PreAuthorize("hasRole('ARTISTA')")
    public ServicioDTO darDeAlta(@PathVariable Long id, Authentication authentication) {
        return servicioService.darDeAlta(id, idAutenticado(authentication));
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName()).getIdUsuario();
    }

    private Long idVisor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return usuarioService.getByNombreUsuario(authentication.getName()).getIdUsuario();
    }
}
