package com.temipora.backend_springboot.controladores;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ConfiguracionBajaCuentaPublicaDTO;
import com.temipora.backend_springboot.servicios.ConfiguracionBajaCuentaService;

@RestController
@RequestMapping("/api")
public class ConfiguracionBajaCuentaPublicoController {

    private final ConfiguracionBajaCuentaService configuracionService;

    public ConfiguracionBajaCuentaPublicoController(
            ConfiguracionBajaCuentaService configuracionService) {
        this.configuracionService = configuracionService;
    }

    @GetMapping("/configuracion-baja-cuenta")
    public ConfiguracionBajaCuentaPublicaDTO obtener() {
        return configuracionService.obtenerPublico();
    }
}
