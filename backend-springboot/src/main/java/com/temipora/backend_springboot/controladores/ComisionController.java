package com.temipora.backend_springboot.controladores;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ComisionConfigDTO;
import com.temipora.backend_springboot.servicios.ComisionService;

/**
 * Comisión vigente visible para artistas y clientes. La plataforma la declara
 * públicamente y el artista necesita conocerla antes de presupuestar.
 */
@RestController
@RequestMapping("/api/comisiones")
public class ComisionController {

    private final ComisionService comisionService;

    public ComisionController(ComisionService comisionService) {
        this.comisionService = comisionService;
    }

    @GetMapping("/vigentes")
    public ComisionConfigDTO vigentes() {
        return comisionService.obtenerEstado();
    }
}
