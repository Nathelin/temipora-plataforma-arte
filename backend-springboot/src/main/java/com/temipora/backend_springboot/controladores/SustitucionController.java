package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.CierreSustitucionDTO;
import com.temipora.backend_springboot.modelo.CrearSustitucionRequest;
import com.temipora.backend_springboot.modelo.ElegirSustitucionRequest;
import com.temipora.backend_springboot.modelo.SustitucionDTO;
import com.temipora.backend_springboot.servicios.SustitucionService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@RestController
@RequestMapping("/api/sustituciones")
public class SustitucionController {

    private final SustitucionService sustitucionService;
    private final UsuarioService usuarioService;

    public SustitucionController(
            SustitucionService sustitucionService,
            UsuarioService usuarioService) {
        this.sustitucionService = sustitucionService;
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public SustitucionDTO crear(
            @RequestBody CrearSustitucionRequest request,
            Authentication authentication) {
        return sustitucionService.crear(idVisor(authentication), request);
    }

    @PostMapping("/{id}/elegir")
    @PreAuthorize("hasAnyRole('CLIENTE','ARTISTA')")
    public SustitucionDTO elegir(
            @PathVariable Long id,
            @RequestBody ElegirSustitucionRequest request,
            Authentication authentication) {
        if (request == null || request.getIdServicio() == null) {
            throw new IllegalArgumentException("El servicio elegido es obligatorio.");
        }
        return sustitucionService.elegir(
            id, idAutenticado(authentication), request.getIdServicio());
    }

    @PostMapping("/{id}/descartar")
    @PreAuthorize("hasAnyRole('CLIENTE','ARTISTA')")
    public void descartar(
            @PathVariable Long id,
            Authentication authentication) {
        sustitucionService.descartar(id, idAutenticado(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENTE','ARTISTA')")
    public SustitucionDTO obtener(
            @PathVariable Long id,
            Authentication authentication) {
        return sustitucionService.obtener(id, idAutenticado(authentication));
    }

    @PostMapping("/por-solicitud/{idSolicitud}")
    @PreAuthorize("hasAnyRole('CLIENTE','ARTISTA')")
    public CierreSustitucionDTO cierrePorSolicitud(
            @PathVariable Long idSolicitud,
            Authentication authentication) {
        return sustitucionService.detalleCierrePorSolicitud(
            idSolicitud, idAutenticado(authentication));
    }

    @PostMapping("/por-pedido/{idPedido}")
    @PreAuthorize("hasAnyRole('CLIENTE','ARTISTA')")
    public CierreSustitucionDTO cierrePorPedido(
            @PathVariable Long idPedido,
            Authentication authentication) {
        return sustitucionService.detalleCierrePorPedido(
            idPedido, idAutenticado(authentication));
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }

    private Long idVisor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return idAutenticado(authentication);
    }
}
