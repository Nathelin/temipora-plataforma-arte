package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.PresupuestoConfigDTO;
import com.temipora.backend_springboot.servicios.ConfiguracionPresupuestoService;

@RestController
@RequestMapping("/api/presupuestos")
public class PresupuestoConfigController {

    private final ConfiguracionPresupuestoService configuracionPresupuestoService;

    public PresupuestoConfigController(
            ConfiguracionPresupuestoService configuracionPresupuestoService) {
        this.configuracionPresupuestoService = configuracionPresupuestoService;
    }

    @GetMapping("/configuracion-vencimiento")
    @PreAuthorize("isAuthenticated()")
    public PresupuestoConfigDTO configuracionVencimiento() {
        return configuracionPresupuestoService.obtenerEstado();
    }
}
