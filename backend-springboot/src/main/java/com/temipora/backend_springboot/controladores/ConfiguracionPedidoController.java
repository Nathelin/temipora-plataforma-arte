package com.temipora.backend_springboot.controladores;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.PedidoConfigDTO;
import com.temipora.backend_springboot.servicios.ConfiguracionPedidoService;

/**
 * Límites del plan de hitos visibles para el artista antes de presupuestar.
 */
@RestController
@RequestMapping("/api/configuracion-pedido")
public class ConfiguracionPedidoController {

    private final ConfiguracionPedidoService configuracionPedidoService;

    public ConfiguracionPedidoController(
            ConfiguracionPedidoService configuracionPedidoService) {
        this.configuracionPedidoService = configuracionPedidoService;
    }

    @GetMapping
    public PedidoConfigDTO vigente() {
        return configuracionPedidoService.obtenerEstado();
    }
}
