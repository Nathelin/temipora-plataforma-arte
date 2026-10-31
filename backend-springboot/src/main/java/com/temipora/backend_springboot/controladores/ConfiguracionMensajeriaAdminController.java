package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ConfiguracionMensajeriaDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionMensajeriaRequest;
import com.temipora.backend_springboot.servicios.ConfiguracionMensajeriaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/configuracion-mensajeria")
@PreAuthorize("hasRole('ADMIN')")
public class ConfiguracionMensajeriaAdminController {

    private final ConfiguracionMensajeriaService configuracionService;

    public ConfiguracionMensajeriaAdminController(
            ConfiguracionMensajeriaService configuracionService) {
        this.configuracionService = configuracionService;
    }

    @GetMapping
    public ConfiguracionMensajeriaDTO obtener() {
        return configuracionService.obtenerEstado();
    }

    @PutMapping
    public ConfiguracionMensajeriaDTO actualizar(
            @Valid @RequestBody ConfiguracionMensajeriaRequest request) {
        return configuracionService.actualizar(request);
    }
}
