package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ApelacionSancionDTO;
import com.temipora.backend_springboot.modelo.EstadoApelacion;
import com.temipora.backend_springboot.modelo.ResolucionApelacionRequest;
import com.temipora.backend_springboot.servicios.ApelacionService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/apelaciones")
@PreAuthorize("hasRole('ADMIN')")
public class ApelacionAdminController {

    private final ApelacionService apelacionService;
    private final UsuarioService usuarioService;

    public ApelacionAdminController(
            ApelacionService apelacionService,
            UsuarioService usuarioService) {
        this.apelacionService = apelacionService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<ApelacionSancionDTO> listar(
            @RequestParam(required = false) EstadoApelacion estado) {
        return apelacionService.listar(estado);
    }

    @GetMapping("/sancion/{idSancion}")
    public List<ApelacionSancionDTO> historial(@PathVariable Long idSancion) {
        return apelacionService.historial(idSancion);
    }

    @PostMapping("/{id}/resolver")
    public ApelacionSancionDTO resolver(
            @PathVariable Long id,
            @Valid @RequestBody ResolucionApelacionRequest request,
            Authentication authentication) {
        Long idAdmin = usuarioService.getByNombreUsuario(authentication.getName())
            .getIdUsuario();
        return apelacionService.resolver(
            idAdmin, id, request.getAceptada(), request.getRespuesta());
    }
}
