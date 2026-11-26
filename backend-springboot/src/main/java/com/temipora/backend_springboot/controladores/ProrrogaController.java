package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.PedidoDTO;
import com.temipora.backend_springboot.modelo.ProrrogaRequest;
import com.temipora.backend_springboot.modelo.RespuestaProrrogaRequest;
import com.temipora.backend_springboot.servicios.PedidoService;
import com.temipora.backend_springboot.servicios.ProrrogaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

/**
 * Propuestas de prórroga del plazo de un pedido. Vive en su propio controlador
 * para no tocar el flujo existente de pedidos.
 */
@RestController
@RequestMapping("/api/pedidos")
public class ProrrogaController {

    private final ProrrogaService prorrogaService;
    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;

    public ProrrogaController(
            ProrrogaService prorrogaService,
            PedidoService pedidoService,
            UsuarioService usuarioService) {
        this.prorrogaService = prorrogaService;
        this.pedidoService = pedidoService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/{idPedido}/prorrogas")
    @PreAuthorize("hasRole('ARTISTA')")
    public PedidoDTO solicitar(
            @PathVariable Long idPedido,
            @Valid @RequestBody ProrrogaRequest request,
            Authentication authentication) {
        Long idUsuario = idAutenticado(authentication);
        prorrogaService.solicitar(idPedido, idUsuario, request);
        return pedidoService.obtener(idPedido, idUsuario);
    }

    @PostMapping("/{idPedido}/prorrogas/{idProrroga}/aceptar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public PedidoDTO aceptar(
            @PathVariable Long idPedido,
            @PathVariable Long idProrroga,
            Authentication authentication) {
        Long idUsuario = idAutenticado(authentication);
        prorrogaService.aceptar(idPedido, idProrroga, idUsuario);
        return pedidoService.obtener(idPedido, idUsuario);
    }

    @PostMapping("/{idPedido}/prorrogas/{idProrroga}/rechazar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public PedidoDTO rechazar(
            @PathVariable Long idPedido,
            @PathVariable Long idProrroga,
            @RequestBody(required = false) RespuestaProrrogaRequest request,
            Authentication authentication) {
        Long idUsuario = idAutenticado(authentication);
        prorrogaService.rechazar(
            idPedido, idProrroga, idUsuario, request);
        return pedidoService.obtener(idPedido, idUsuario);
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }
}
