package com.temipora.backend_springboot.controladores;

import java.util.NoSuchElementException;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.Pago;
import com.temipora.backend_springboot.modelo.PedidoDTO;
import com.temipora.backend_springboot.repositorios.PagoRepository;
import com.temipora.backend_springboot.servicios.AccesoDenegadoException;
import com.temipora.backend_springboot.servicios.PagoService;
import com.temipora.backend_springboot.servicios.PedidoService;
import com.temipora.backend_springboot.servicios.UsuarioService;

/**
 * Permite que el pagador fuerce una consulta del estado real del cobro al
 * proveedor cuando el webhook se demora. Es idempotente.
 */
@RestController
@RequestMapping("/api/pagos")
public class PagoSincronizacionController {

    private final PagoService pagoService;
    private final PagoRepository pagoRepository;
    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;

    public PagoSincronizacionController(
            PagoService pagoService,
            PagoRepository pagoRepository,
            PedidoService pedidoService,
            UsuarioService usuarioService) {
        this.pagoService = pagoService;
        this.pagoRepository = pagoRepository;
        this.pedidoService = pedidoService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/{idPago}/sincronizar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public PedidoDTO sincronizar(
            @PathVariable Long idPago,
            Authentication authentication) {
        Long idUsuario = usuarioService
            .getByNombreUsuario(authentication.getName())
            .getIdUsuario();
        Pago pago = pagoRepository.findByIdPagoConPedido(idPago)
            .orElseThrow(() -> new NoSuchElementException(
                "Pago no encontrado: " + idPago));
        if (pago.getUsuarioPagador() == null
                || !pago.getUsuarioPagador().getIdUsuario().equals(idUsuario)) {
            throw new AccesoDenegadoException(
                "No podés operar sobre este pago.");
        }
        pagoService.sincronizar(idPago);
        return pedidoService.obtener(
            pago.getHitoPedido().getPedido().getIdPedido(), idUsuario);
    }
}
