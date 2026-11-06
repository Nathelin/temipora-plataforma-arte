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
import com.temipora.backend_springboot.modelo.OrdenServicio;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.RecomendacionArtistaDTO;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.ServicioRequest;
import com.temipora.backend_springboot.modelo.ServicioResumenEstadoDTO;
import com.temipora.backend_springboot.servicios.ServicioBajaService;
import com.temipora.backend_springboot.servicios.ServicioService;
import com.temipora.backend_springboot.servicios.ConfiguracionSustitucionService;
import com.temipora.backend_springboot.servicios.UsuarioService;
import com.temipora.backend_springboot.servicios.VisitaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ServicioController {

    private final ServicioService servicioService;
    private final ServicioBajaService servicioBajaService;
    private final UsuarioService usuarioService;
    private final VisitaService visitaService;
    private final ConfiguracionSustitucionService configuracionSustitucionService;

    public ServicioController(
            ServicioService servicioService,
            ServicioBajaService servicioBajaService,
            UsuarioService usuarioService,
            VisitaService visitaService,
            ConfiguracionSustitucionService configuracionSustitucionService) {
        this.servicioService = servicioService;
        this.servicioBajaService = servicioBajaService;
        this.usuarioService = usuarioService;
        this.visitaService = visitaService;
        this.configuracionSustitucionService = configuracionSustitucionService;
    }

    @GetMapping("/mis-servicios")
    @PreAuthorize("hasRole('ARTISTA')")
    public List<ServicioDTO> misServicios(Authentication authentication) {
        return servicioService.listarMios(idAutenticado(authentication));
    }

    @GetMapping("/servicios")
    public PaginaDTO<ServicioDTO> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String nombreArtista,
            @RequestParam(required = false) Long idTipoServicio,
            @RequestParam(required = false) Boolean sensible,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) Integer tiempoMaxDias,
            @RequestParam(required = false) Boolean incluirSinCupos,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "24") int tamanio,
            @RequestParam(defaultValue = "RELEVANCIA") OrdenServicio orden,
            Authentication authentication) {
        FiltroServicioRequest filtro = new FiltroServicioRequest();
        filtro.setNombre(nombre);
        filtro.setNombreArtista(nombreArtista);
        filtro.setIdTipoServicio(idTipoServicio);
        filtro.setSensible(sensible);
        filtro.setPrecioMin(precioMin);
        filtro.setPrecioMax(precioMax);
        filtro.setTiempoMaxDias(tiempoMaxDias);
        boolean incluir = incluirSinCupos == null || incluirSinCupos;
        boolean mostrarSinCupos =
            configuracionSustitucionService.obtener().isMostrarEnExplorar();
        filtro.setIncluirSinCupos(incluir && mostrarSinCupos);
        return servicioService.listarActivosPaginado(
            filtro, idVisor(authentication), pagina, tamanio, orden);
    }

    @GetMapping("/servicios/artista/{nombreUsuario}")
    public List<ServicioDTO> listarPorArtista(
            @PathVariable String nombreUsuario,
            Authentication authentication) {
        return servicioService.listarActivosDeArtista(nombreUsuario, idVisor(authentication));
    }

    @GetMapping("/servicios/{id}")
    public ServicioDTO obtener(@PathVariable Long id, Authentication authentication) {
        Long viewerId = idVisor(authentication);
        ServicioDTO servicio = servicioService.obtener(id, viewerId);
        if (viewerId != null
                && servicio.getIdArtista() != null
                && !viewerId.equals(servicio.getIdArtista())) {
            visitaService.registrarServicio(viewerId, id);
            visitaService.registrarArtista(viewerId, servicio.getIdArtista());
        }
        return servicio;
    }

    @GetMapping("/servicios/{id}/similares")
    public List<RecomendacionArtistaDTO> similares(
            @PathVariable Long id,
            Authentication authentication) {
        return servicioService.artistasSimilares(id, idVisor(authentication));
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

    @PostMapping("/servicios/{id}/pausar")
    @PreAuthorize("hasRole('ARTISTA')")
    public ServicioDTO pausar(@PathVariable Long id, Authentication authentication) {
        return servicioService.pausar(id, idAutenticado(authentication));
    }

    @PostMapping("/servicios/{id}/reactivar")
    @PreAuthorize("hasRole('ARTISTA')")
    public ServicioDTO reactivar(@PathVariable Long id, Authentication authentication) {
        return servicioService.reactivar(id, idAutenticado(authentication));
    }

    @PostMapping("/servicios/{id}/archivar")
    @PreAuthorize("hasRole('ARTISTA')")
    public ServicioDTO archivar(@PathVariable Long id, Authentication authentication) {
        return servicioBajaService.archivar(id, idAutenticado(authentication));
    }

    @GetMapping("/servicios/{id}/resumen")
    @PreAuthorize("hasRole('ARTISTA')")
    public ServicioResumenEstadoDTO resumen(@PathVariable Long id, Authentication authentication) {
        return servicioBajaService.resumenEstado(id, idAutenticado(authentication));
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
