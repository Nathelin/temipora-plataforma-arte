package com.temipora.backend_springboot.controladores;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ArancelEstimadoDTO;
import com.temipora.backend_springboot.servicios.pagos.ArancelEstimadoService;

@RestController
@RequestMapping("/api/pagos")
public class ArancelEstimadoController {

    private final ArancelEstimadoService arancelEstimadoService;

    public ArancelEstimadoController(
            ArancelEstimadoService arancelEstimadoService) {
        this.arancelEstimadoService = arancelEstimadoService;
    }

    @GetMapping("/arancel-estimado")
    public ArancelEstimadoDTO estimado() {
        return arancelEstimadoService.estimar();
    }
}
