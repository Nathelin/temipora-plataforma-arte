package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ConfiguracionReseniaDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionReseniaRequest;
import com.temipora.backend_springboot.servicios.ConfiguracionReseniaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/configuracion-resenia")
@PreAuthorize("hasRole('ADMIN')")
public class ConfiguracionReseniaAdminController {

    private final ConfiguracionReseniaService configuracionReseniaService;

    public ConfiguracionReseniaAdminController(
            ConfiguracionReseniaService configuracionReseniaService) {
        this.configuracionReseniaService = configuracionReseniaService;
    }

    @GetMapping
    public ConfiguracionReseniaDTO obtener() {
        return configuracionReseniaService.obtenerEstado();
    }

    @PutMapping
    public ConfiguracionReseniaDTO actualizar(
            @Valid @RequestBody ConfiguracionReseniaRequest request) {
        return configuracionReseniaService.actualizar(request);
    }
}
