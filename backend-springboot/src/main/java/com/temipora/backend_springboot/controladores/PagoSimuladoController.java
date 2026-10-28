package com.temipora.backend_springboot.controladores;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.PedidoDTO;
import com.temipora.backend_springboot.servicios.PagoService;
import com.temipora.backend_springboot.servicios.PedidoService;
import com.temipora.backend_springboot.servicios.UsuarioService;

/**
 * Endpoints de desarrollo del proveedor simulado. No se registran cuando
 * {@code pagos.proveedor} apunta a un proveedor real.
 */
@RestController
@RequestMapping("/api/pagos/simulado")
@ConditionalOnProperty(
    name = "pagos.proveedor",
    havingValue = "simulado",
    matchIfMissing = true)
public class PagoSimuladoController {

    private final PagoService pagoService;
    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;

    public PagoSimuladoController(
            PagoService pagoService,
            PedidoService pedidoService,
            UsuarioService usuarioService) {
        this.pagoService = pagoService;
        this.pedidoService = pedidoService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/{idPago}/confirmar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public PedidoDTO confirmar(
            @PathVariable Long idPago,
            Authentication authentication) {
        Long idUsuario = idAutenticado(authentication);
        Pedido pedido = pagoService.confirmar(idPago, idUsuario, null);
        return pedidoService.obtener(pedido.getIdPedido(), idUsuario);
    }

    @PostMapping("/{idPago}/rechazar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public PedidoDTO rechazar(
            @PathVariable Long idPago,
            @RequestParam(required = false) String motivo,
            Authentication authentication) {
        Long idUsuario = idAutenticado(authentication);
        Pedido pedido = pagoService.rechazar(idPago, idUsuario, motivo);
        return pedidoService.obtener(pedido.getIdPedido(), idUsuario);
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }
}
