package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.PedidoConfigDTO;
import com.temipora.backend_springboot.modelo.PedidoConfigRequest;
import com.temipora.backend_springboot.servicios.ConfiguracionPedidoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/configuracion-pedido")
@PreAuthorize("hasRole('ADMIN')")
public class PedidoAdminController {

    private final ConfiguracionPedidoService configuracionPedidoService;

    public PedidoAdminController(
            ConfiguracionPedidoService configuracionPedidoService) {
        this.configuracionPedidoService = configuracionPedidoService;
    }

    @GetMapping
    public PedidoConfigDTO obtener() {
        return configuracionPedidoService.obtenerEstado();
    }

    @PutMapping
    public PedidoConfigDTO actualizar(
            @Valid @RequestBody PedidoConfigRequest request) {
        return configuracionPedidoService.actualizar(request);
    }
}
