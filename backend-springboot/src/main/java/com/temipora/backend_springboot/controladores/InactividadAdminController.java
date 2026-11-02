package com.temipora.backend_springboot.controladores;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ConfiguracionInactividadDTO;
import com.temipora.backend_springboot.servicios.ConfiguracionInactividadService;
import com.temipora.backend_springboot.servicios.ReactivacionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class InactividadAdminController {

    private final ConfiguracionInactividadService configuracionService;
    private final ReactivacionService reactivacionService;

    public InactividadAdminController(
            ConfiguracionInactividadService configuracionService,
            ReactivacionService reactivacionService) {
        this.configuracionService = configuracionService;
        this.reactivacionService = reactivacionService;
    }

    @GetMapping("/configuracion-inactividad")
    public ConfiguracionInactividadDTO obtener() {
        return configuracionService.obtenerDto();
    }

    @PutMapping("/configuracion-inactividad")
    public ConfiguracionInactividadDTO actualizar(
            @Valid @RequestBody ConfiguracionInactividadDTO request) {
        return configuracionService.actualizar(request);
    }

    @PostMapping("/reactivacion/ejecutar")
    public Map<String, Object> ejecutar() {
        int campanas = reactivacionService.ejecutar();
        return Map.of("campanas", campanas);
    }
}
