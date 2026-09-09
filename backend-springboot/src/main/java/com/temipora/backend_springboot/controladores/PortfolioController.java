package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ImagenPortfolioDTO;
import com.temipora.backend_springboot.modelo.ImagenPortfolioRequest;
import com.temipora.backend_springboot.servicios.PortfolioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/{nombreUsuario}")
    public List<ImagenPortfolioDTO> listar(@PathVariable String nombreUsuario) {
        return portfolioService.listar(nombreUsuario);
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
}
