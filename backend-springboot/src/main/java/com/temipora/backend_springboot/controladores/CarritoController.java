package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.CarritoDTO;
import com.temipora.backend_springboot.modelo.CarritoItemRequest;
import com.temipora.backend_springboot.modelo.CheckoutRequest;
import com.temipora.backend_springboot.modelo.CompraDTO;
import com.temipora.backend_springboot.servicios.CarritoService;
import com.temipora.backend_springboot.servicios.CompraService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/carrito")
@PreAuthorize("hasAnyRole('CLIENTE', 'ARTISTA')")
public class CarritoController {

    private final CarritoService carritoService;
    private final CompraService compraService;

    public CarritoController(
            CarritoService carritoService,
            CompraService compraService) {
        this.carritoService = carritoService;
        this.compraService = compraService;
    }

    @GetMapping
    public CarritoDTO obtener(Authentication authentication) {
        return carritoService.obtener(authentication.getName());
    }

    @PostMapping("/items")
    public CarritoDTO agregar(
            @Valid @RequestBody CarritoItemRequest request,
            Authentication authentication) {
        return carritoService.agregar(authentication.getName(), request);
    }

    @PatchMapping("/items/{idItem}")
    public CarritoDTO actualizar(
            @PathVariable Long idItem,
            @RequestBody CarritoItemRequest request,
            Authentication authentication) {
        return carritoService.actualizar(
            authentication.getName(), idItem, request.getCantidad());
    }

    @DeleteMapping("/items/{idItem}")
    public CarritoDTO quitar(
            @PathVariable Long idItem,
            Authentication authentication) {
        return carritoService.quitar(authentication.getName(), idItem);
    }

    @DeleteMapping
    public CarritoDTO vaciar(
            @RequestParam(required = false) Long artista,
            Authentication authentication) {
        if (artista != null) {
            return carritoService.vaciarArtista(
                authentication.getName(), artista);
        }
        return carritoService.vaciar(authentication.getName());
    }

    @PostMapping("/checkout")
    public CompraDTO checkout(
            @RequestBody(required = false) CheckoutRequest request,
            Authentication authentication) {
        return compraService.crearDesdeCarrito(
            authentication.getName(), request);
    }
}
