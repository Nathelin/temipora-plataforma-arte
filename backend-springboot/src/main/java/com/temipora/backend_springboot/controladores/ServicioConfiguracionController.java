package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.DisponibilidadServicioDTO;
import com.temipora.backend_springboot.modelo.FormularioDTO;
import com.temipora.backend_springboot.modelo.FormularioRequest;
import com.temipora.backend_springboot.modelo.ListaEsperaDTO;
import com.temipora.backend_springboot.modelo.SolicitudPublicaDTO;
import com.temipora.backend_springboot.modelo.SolicitudResumenDTO;
import com.temipora.backend_springboot.modelo.TerminosDTO;
import com.temipora.backend_springboot.modelo.TerminosRequest;
import com.temipora.backend_springboot.servicios.FormularioService;
import com.temipora.backend_springboot.servicios.ListaEsperaService;
import com.temipora.backend_springboot.servicios.ServicioService;
import com.temipora.backend_springboot.servicios.SolicitudServicioService;
import com.temipora.backend_springboot.servicios.TerminosService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/servicios/{idServicio}")
public class ServicioConfiguracionController {

    private final ServicioService servicioService;
    private final FormularioService formularioService;
    private final TerminosService terminosService;
    private final UsuarioService usuarioService;
    private final SolicitudServicioService solicitudServicioService;
    private final ListaEsperaService listaEsperaService;

    public ServicioConfiguracionController(
            ServicioService servicioService,
            FormularioService formularioService,
            TerminosService terminosService,
            UsuarioService usuarioService,
            SolicitudServicioService solicitudServicioService,
            ListaEsperaService listaEsperaService) {
        this.servicioService = servicioService;
        this.formularioService = formularioService;
        this.terminosService = terminosService;
        this.usuarioService = usuarioService;
        this.solicitudServicioService = solicitudServicioService;
        this.listaEsperaService = listaEsperaService;
    }

    @GetMapping("/solicitud-publica")
    public SolicitudPublicaDTO obtenerSolicitudPublica(
            @PathVariable Long idServicio,
            Authentication authentication) {
        return solicitudServicioService.solicitudPublica(
            idServicio, idAutenticadoOpcional(authentication));
    }

    @GetMapping("/disponibilidad")
    public DisponibilidadServicioDTO obtenerDisponibilidad(
            @PathVariable Long idServicio,
            Authentication authentication) {
        return solicitudServicioService.disponibilidad(
            idServicio, idAutenticadoOpcional(authentication));
    }

    @GetMapping("/solicitudes")
    @PreAuthorize("hasRole('ARTISTA')")
    public List<SolicitudResumenDTO> obtenerSolicitudes(
            @PathVariable Long idServicio,
            Authentication authentication) {
        Long idArtista = idAutenticado(authentication);
        servicioService.verificarPropietario(idServicio, idArtista);
        return solicitudServicioService.listarPorServicio(idServicio);
    }

    @GetMapping("/lista-espera")
    @PreAuthorize("hasRole('ARTISTA')")
    public List<ListaEsperaDTO> obtenerListaEspera(
            @PathVariable Long idServicio,
            Authentication authentication) {
        Long idArtista = idAutenticado(authentication);
        servicioService.verificarPropietario(idServicio, idArtista);
        return listaEsperaService.listarPorServicio(idServicio);
    }

    @GetMapping("/formulario-pedidos")
    @PreAuthorize("hasRole('ARTISTA')")
    public FormularioDTO obtenerFormulario(
            @PathVariable Long idServicio,
            Authentication authentication) {
        Long idArtista = idAutenticado(authentication);
        servicioService.verificarPropietario(idServicio, idArtista);
        return formularioService.obtenerServicio(idServicio, idArtista);
    }

    @PutMapping("/formulario-pedidos")
    @PreAuthorize("hasRole('ARTISTA')")
    public FormularioDTO guardarFormulario(
            @PathVariable Long idServicio,
            @Valid @RequestBody FormularioRequest request,
            Authentication authentication) {
        Long idArtista = idAutenticado(authentication);
        servicioService.verificarPropietario(idServicio, idArtista);
        return formularioService.guardarServicio(idServicio, idArtista, request);
    }

    @DeleteMapping("/formulario-pedidos")
    @PreAuthorize("hasRole('ARTISTA')")
    public ResponseEntity<Void> eliminarFormulario(
            @PathVariable Long idServicio,
            Authentication authentication) {
        Long idArtista = idAutenticado(authentication);
        servicioService.verificarPropietario(idServicio, idArtista);
        formularioService.eliminarServicio(idServicio, idArtista);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/terminos")
    @PreAuthorize("hasRole('ARTISTA')")
    public TerminosDTO obtenerTerminos(
            @PathVariable Long idServicio,
            Authentication authentication) {
        Long idArtista = idAutenticado(authentication);
        servicioService.verificarPropietario(idServicio, idArtista);
        return terminosService.obtenerServicio(idServicio, idArtista);
    }

    @GetMapping("/terminos-publicos")
    public TerminosDTO obtenerTerminosPublicos(@PathVariable Long idServicio) {
        Long idArtista = servicioService.obtener(idServicio, null).getIdArtista();
        return terminosService.obtenerServicio(idServicio, idArtista);
    }

    @PutMapping("/terminos")
    @PreAuthorize("hasRole('ARTISTA')")
    public TerminosDTO guardarTerminos(
            @PathVariable Long idServicio,
            @Valid @RequestBody TerminosRequest request,
            Authentication authentication) {
        Long idArtista = idAutenticado(authentication);
        servicioService.verificarPropietario(idServicio, idArtista);
        return terminosService.guardarServicio(idServicio, idArtista, request);
    }

    @DeleteMapping("/terminos")
    @PreAuthorize("hasRole('ARTISTA')")
    public ResponseEntity<Void> eliminarTerminos(
            @PathVariable Long idServicio,
            Authentication authentication) {
        Long idArtista = idAutenticado(authentication);
        servicioService.verificarPropietario(idServicio, idArtista);
        terminosService.eliminarServicio(idServicio, idArtista);
        return ResponseEntity.noContent().build();
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName()).getIdUsuario();
    }

    private Long idAutenticadoOpcional(Authentication authentication) {
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return null;
        }
        return idAutenticado(authentication);
    }
}
