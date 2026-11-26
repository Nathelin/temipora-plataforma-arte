package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ApelacionRequest;
import com.temipora.backend_springboot.modelo.ApelacionSancionDTO;
import com.temipora.backend_springboot.modelo.EstadoSancionDTO;
import com.temipora.backend_springboot.servicios.ApelacionService;
import com.temipora.backend_springboot.servicios.SancionService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/apelaciones")
public class ApelacionController {

    private final ApelacionService apelacionService;
    private final UsuarioService usuarioService;
    private final SancionService sancionService;

    public ApelacionController(
            ApelacionService apelacionService,
            UsuarioService usuarioService,
            SancionService sancionService) {
        this.apelacionService = apelacionService;
        this.usuarioService = usuarioService;
        this.sancionService = sancionService;
    }

    @GetMapping
    public List<ApelacionSancionDTO> mias(Authentication authentication) {
        return apelacionService.mias(idAutenticado(authentication));
    }

    @GetMapping("/sancion-activa")
    public EstadoSancionDTO sancionActiva(Authentication authentication) {
        return sancionService.estadoUsuario(idAutenticado(authentication));
    }

    @PostMapping("/sancion/{idSancion}")
    public ResponseEntity<ApelacionSancionDTO> crear(
            @PathVariable Long idSancion,
            @Valid @RequestBody ApelacionRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            apelacionService.crear(
                idAutenticado(authentication), idSancion, request.getMensaje()));
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }
}
