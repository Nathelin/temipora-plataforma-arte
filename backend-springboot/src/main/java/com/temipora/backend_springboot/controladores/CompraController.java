package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.CompraDTO;
import com.temipora.backend_springboot.modelo.CompraResumenDTO;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.servicios.CompraService;

@RestController
@RequestMapping("/api/compras")
@PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @GetMapping
    public PaginaDTO<CompraResumenDTO> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamanio,
            Authentication authentication) {
        return compraService.listar(
            authentication.getName(), pagina, tamanio);
    }

    @GetMapping("/{idCompra}")
    public CompraDTO obtener(
            @PathVariable Long idCompra,
            Authentication authentication) {
        return compraService.obtener(authentication.getName(), idCompra);
    }
}
