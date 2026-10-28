package com.temipora.backend_springboot.controladores;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.PedidoDTO;
import com.temipora.backend_springboot.servicios.PedidoService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@RestController
@RequestMapping("/api/solicitudes-servicio")
public class PagoSolicitudController {

    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;

    public PagoSolicitudController(
            PedidoService pedidoService,
            UsuarioService usuarioService) {
        this.pedidoService = pedidoService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/{idSolicitud}/pagar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
    public ResponseEntity<PedidoDTO> pagar(
            @PathVariable Long idSolicitud,
            Authentication authentication) {
        Long idCliente = idAutenticado(authentication);
        PedidoDTO pedido = pedidoService.crearDesdePresupuesto(
            idSolicitud, idCliente);
        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }
}
