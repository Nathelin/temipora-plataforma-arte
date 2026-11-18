package com.temipora.backend_springboot.controladores;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ReputacionClienteDTO;
import com.temipora.backend_springboot.modelo.ReseniaDTO;
import com.temipora.backend_springboot.modelo.ReseniaEstadoDTO;
import com.temipora.backend_springboot.modelo.ReseniaRequest;
import com.temipora.backend_springboot.servicios.ReseniaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ReseniaController {

    private final ReseniaService reseniaService;
    private final UsuarioService usuarioService;

    public ReseniaController(
            ReseniaService reseniaService,
            UsuarioService usuarioService) {
        this.reseniaService = reseniaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/pedidos/{idPedido}/resenia")
    @PreAuthorize("hasAnyRole('CLIENTE','ARTISTA')")
    public ReseniaEstadoDTO estado(
            @PathVariable Long idPedido,
            Authentication authentication) {
        return reseniaService.estado(idPedido, idAutenticado(authentication));
    }

    @PostMapping("/pedidos/{idPedido}/resenia")
    @PreAuthorize("hasAnyRole('CLIENTE','ARTISTA')")
    public ResponseEntity<ReseniaDTO> crear(
            @PathVariable Long idPedido,
            @Valid @RequestBody ReseniaRequest request,
            Authentication authentication) {
        ReseniaDTO dto = reseniaService.crear(
            idPedido, idAutenticado(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/pedidos/{idPedido}/resenia")
    @PreAuthorize("hasAnyRole('CLIENTE','ARTISTA')")
    public ReseniaDTO editar(
            @PathVariable Long idPedido,
            @Valid @RequestBody ReseniaRequest request,
            Authentication authentication) {
        return reseniaService.editar(
            idPedido, idAutenticado(authentication), request);
    }

    @DeleteMapping("/pedidos/{idPedido}/resenia")
    @PreAuthorize("hasAnyRole('CLIENTE','ARTISTA')")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long idPedido,
            Authentication authentication) {
        reseniaService.eliminar(idPedido, idAutenticado(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/productos/{idProducto}/resenia")
    @PreAuthorize("hasAnyRole('CLIENTE','ARTISTA')")
    public ReseniaEstadoDTO estadoProducto(
            @PathVariable Long idProducto,
            Authentication authentication) {
        return reseniaService.estadoProducto(
            idProducto, idAutenticado(authentication));
    }

    @PostMapping("/productos/{idProducto}/resenia")
    @PreAuthorize("hasAnyRole('CLIENTE','ARTISTA')")
    public ResponseEntity<ReseniaDTO> crearProducto(
            @PathVariable Long idProducto,
            @Valid @RequestBody ReseniaRequest request,
            Authentication authentication) {
        ReseniaDTO dto = reseniaService.crearProducto(
            idProducto, idAutenticado(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/productos/{idProducto}/resenia")
    @PreAuthorize("hasAnyRole('CLIENTE','ARTISTA')")
    public ReseniaDTO editarProducto(
            @PathVariable Long idProducto,
            @Valid @RequestBody ReseniaRequest request,
            Authentication authentication) {
        return reseniaService.editarProducto(
            idProducto, idAutenticado(authentication), request);
    }

    @DeleteMapping("/productos/{idProducto}/resenia")
    @PreAuthorize("hasAnyRole('CLIENTE','ARTISTA')")
    public ResponseEntity<Void> eliminarProducto(
            @PathVariable Long idProducto,
            Authentication authentication) {
        reseniaService.eliminarProducto(
            idProducto, idAutenticado(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/clientes/{idCliente}/reputacion")
    @PreAuthorize("hasRole('ARTISTA')")
    public ReputacionClienteDTO reputacion(
            @PathVariable Long idCliente,
            Authentication authentication) {
        return reseniaService.reputacionCliente(
            idCliente, idAutenticado(authentication));
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName())
            .getIdUsuario();
    }
}
