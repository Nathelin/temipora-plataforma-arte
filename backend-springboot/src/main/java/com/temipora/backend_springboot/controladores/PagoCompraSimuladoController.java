package com.temipora.backend_springboot.controladores;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.CompraDTO;
import com.temipora.backend_springboot.servicios.CompraPagoService;

@RestController
@RequestMapping("/api/pagos/simulado/compras")
@ConditionalOnProperty(
    name = "pagos.proveedor",
    havingValue = "simulado",
    matchIfMissing = true)
@PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
public class PagoCompraSimuladoController {

    private final CompraPagoService compraPagoService;

    public PagoCompraSimuladoController(CompraPagoService compraPagoService) {
        this.compraPagoService = compraPagoService;
    }

    @PostMapping("/{idIntento}/confirmar")
    public CompraDTO confirmar(
            @PathVariable Long idIntento,
            Authentication authentication) {
        return compraPagoService.confirmarSimulado(
            authentication.getName(), idIntento);
    }

    @PostMapping("/{idIntento}/rechazar")
    public CompraDTO rechazar(
            @PathVariable Long idIntento,
            Authentication authentication) {
        return compraPagoService.rechazarSimulado(
            authentication.getName(), idIntento);
    }
}
