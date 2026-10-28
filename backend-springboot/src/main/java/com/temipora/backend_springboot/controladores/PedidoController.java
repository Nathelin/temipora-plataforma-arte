package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.EntregaHitoRequest;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.PedidoDTO;
import com.temipora.backend_springboot.modelo.PedidoResumenDTO;
import com.temipora.backend_springboot.modelo.SolicitudRevisionRequest;
import com.temipora.backend_springboot.servicios.PedidoService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;

    public PedidoController(
            PedidoService pedidoService,
            UsuarioService usuarioService) {
        this.pedidoService = pedidoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/{idPedido}")
    public PedidoDTO obtener(
            @PathVariable Long idPedido,
            Authentication authentication) {
        return pedidoService.obtener(idPedido, idAutenticado(authentication));
    }

    @GetMapping("/recibidos")
    @PreAuthorize("hasRole('ARTISTA')")
    public PaginaDTO<PedidoResumenDTO> recibidos(
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            Authentication authentication) {
        return pedidoService.listarRecibidos(
            idAutenticado(authentication), estado, pagina, tamanio);
    }

    @GetMapping("/enviados")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public PaginaDTO<PedidoResumenDTO> enviados(
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            Authentication authentication) {
        return pedidoService.listarEnviados(
            idAutenticado(authentication), estado, pagina, tamanio);
    }

    @PostMapping("/{idPedido}/hitos/{idHito}/pagar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public PedidoDTO pagarHito(
            @PathVariable Long idPedido,
            @PathVariable Long idHito,
            Authentication authentication) {
        return pedidoService.pagarHito(
            idPedido, idHito, idAutenticado(authentication));
    }

    @PostMapping("/{idPedido}/hitos/{idHito}/aprobar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public PedidoDTO aprobar(
            @PathVariable Long idPedido,
            @PathVariable Long idHito,
            Authentication authentication) {
        return pedidoService.aprobar(
            idPedido, idHito, idAutenticado(authentication));
    }

    @PostMapping("/{idPedido}/hitos/{idHito}/solicitar-revision")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public PedidoDTO solicitarRevision(
            @PathVariable Long idPedido,
            @PathVariable Long idHito,
            @RequestBody(required = false) SolicitudRevisionRequest request,
            Authentication authentication) {
        return pedidoService.solicitarRevision(
            idPedido, idHito, idAutenticado(authentication), request);
    }

    @PostMapping("/{idPedido}/hitos/{idHito}/entregar")
    @PreAuthorize("hasRole('ARTISTA')")
    public PedidoDTO entregar(
            @PathVariable Long idPedido,
            @PathVariable Long idHito,
            @Valid @RequestBody EntregaHitoRequest request,
            Authentication authentication) {
        return pedidoService.entregar(
            idPedido, idHito, idAutenticado(authentication), request);
    }

    @PostMapping("/{idPedido}/cancelar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public PedidoDTO cancelar(
            @PathVariable Long idPedido,
            Authentication authentication) {
        return pedidoService.cancelar(
            idPedido, idAutenticado(authentication));
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }
}
