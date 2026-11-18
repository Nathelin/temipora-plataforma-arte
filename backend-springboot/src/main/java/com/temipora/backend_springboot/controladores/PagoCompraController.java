package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.CompraDTO;
import com.temipora.backend_springboot.modelo.PagoCompraDTO;
import com.temipora.backend_springboot.servicios.CompraPagoService;

@RestController
@RequestMapping("/api/compras")
@PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
public class PagoCompraController {

    private final CompraPagoService compraPagoService;

    public PagoCompraController(CompraPagoService compraPagoService) {
        this.compraPagoService = compraPagoService;
    }

    @PostMapping("/{idCompra}/grupos/{idGrupo}/pagar")
    public PagoCompraDTO pagar(
            @PathVariable Long idCompra,
            @PathVariable Long idGrupo,
            Authentication authentication) {
        return compraPagoService.iniciarPago(
            authentication.getName(), idCompra, idGrupo);
    }

    @PostMapping("/{idCompra}/sincronizar")
    public CompraDTO sincronizar(
            @PathVariable Long idCompra,
            Authentication authentication) {
        return compraPagoService.sincronizar(
            authentication.getName(), idCompra);
    }
}
