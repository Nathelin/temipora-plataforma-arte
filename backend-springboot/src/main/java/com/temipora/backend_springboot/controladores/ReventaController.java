package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.PiezaRevendibleDTO;
import com.temipora.backend_springboot.modelo.PublicacionReventaDTO;
import com.temipora.backend_springboot.modelo.ReventaRequest;
import com.temipora.backend_springboot.servicios.ReventaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reventas")
@PreAuthorize("hasAnyRole('CLIENTE','ARTISTA')")
public class ReventaController {

    private final ReventaService reventaService;

    public ReventaController(ReventaService reventaService) {
        this.reventaService = reventaService;
    }

    @GetMapping("/mias")
    public List<PublicacionReventaDTO> mias(Authentication authentication) {
        return reventaService.mias(authentication.getName());
    }

    @GetMapping("/elegibles")
    public List<PiezaRevendibleDTO> elegibles(Authentication authentication) {
        return reventaService.elegibles(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PublicacionReventaDTO publicar(
            Authentication authentication,
            @Valid @RequestBody ReventaRequest request) {
        return reventaService.publicar(authentication.getName(), request);
    }

    @PatchMapping("/{idPublicacion}")
    public PublicacionReventaDTO editar(
            Authentication authentication,
            @PathVariable Long idPublicacion,
            @Valid @RequestBody ReventaRequest request) {
        return reventaService.editar(authentication.getName(), idPublicacion, request);
    }

    @DeleteMapping("/{idPublicacion}")
    public PublicacionReventaDTO cancelar(
            Authentication authentication,
            @PathVariable Long idPublicacion) {
        return reventaService.cancelar(authentication.getName(), idPublicacion);
    }
}
