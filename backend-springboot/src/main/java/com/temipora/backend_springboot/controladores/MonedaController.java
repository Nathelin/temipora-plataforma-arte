package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.MonedaDTO;
import com.temipora.backend_springboot.servicios.MonedaService;

@RestController
@RequestMapping("/api/monedas")
public class MonedaController {

    private final MonedaService monedaService;

    public MonedaController(MonedaService monedaService) {
        this.monedaService = monedaService;
    }

    @GetMapping
    public List<MonedaDTO> listar() {
        return monedaService.listarActivas();
    }
}
