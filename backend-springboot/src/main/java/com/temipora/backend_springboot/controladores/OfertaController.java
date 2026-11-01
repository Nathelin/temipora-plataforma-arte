package com.temipora.backend_springboot.controladores;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.CampanaReactivacion;
import com.temipora.backend_springboot.modelo.CampanaReactivacionDTO;
import com.temipora.backend_springboot.modelo.OfertaReactivacionDTO;
import com.temipora.backend_springboot.servicios.OfertaReactivacionService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@RestController
@RequestMapping("/api/ofertas")
public class OfertaController {

    private final OfertaReactivacionService ofertaService;
    private final UsuarioService usuarioService;

    public OfertaController(
            OfertaReactivacionService ofertaService,
            UsuarioService usuarioService) {
        this.ofertaService = ofertaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<OfertaReactivacionDTO> misOfertas(Authentication authentication) {
        Long idUsuario = usuarioService
            .getByNombreUsuario(authentication.getName())
            .getIdUsuario();
        LocalDateTime ahora = LocalDateTime.now();
        return ofertaService.activasDeUsuario(idUsuario).stream()
            .map(oferta -> OfertaReactivacionDTO.desdeEntidad(oferta, ahora))
            .toList();
    }

    @GetMapping("/campana/{token}")
    public CampanaReactivacionDTO campana(@PathVariable String token) {
        CampanaReactivacion campana = ofertaService.campanaPorToken(token);
        return CampanaReactivacionDTO.desdeEntidad(campana, LocalDateTime.now());
    }

    @GetMapping("/{idOferta}")
    @PreAuthorize("isAuthenticated()")
    public OfertaReactivacionDTO oferta(
            @PathVariable Long idOferta,
            Authentication authentication) {
        Long idUsuario = usuarioService
            .getByNombreUsuario(authentication.getName())
            .getIdUsuario();
        return OfertaReactivacionDTO.desdeEntidad(
            ofertaService.ofertaDeUsuario(idOferta, idUsuario),
            LocalDateTime.now());
    }
}
