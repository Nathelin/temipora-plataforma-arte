package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.PresupuestoConfigDTO;
import com.temipora.backend_springboot.modelo.PresupuestoConfigRequest;
import com.temipora.backend_springboot.servicios.ConfiguracionPresupuestoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/configuracion-presupuesto")
@PreAuthorize("hasRole('ADMIN')")
public class PresupuestoAdminController {

    private final ConfiguracionPresupuestoService configuracionPresupuestoService;

    public PresupuestoAdminController(
            ConfiguracionPresupuestoService configuracionPresupuestoService) {
        this.configuracionPresupuestoService = configuracionPresupuestoService;
    }

    @GetMapping
    public PresupuestoConfigDTO obtener() {
        return configuracionPresupuestoService.obtenerEstado();
    }

    @PutMapping
    public PresupuestoConfigDTO actualizar(
            @Valid @RequestBody PresupuestoConfigRequest request) {
        return configuracionPresupuestoService.actualizar(request);
    }
}
