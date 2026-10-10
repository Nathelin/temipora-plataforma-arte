package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.TipoArteDTO;
import com.temipora.backend_springboot.servicios.TipoArteService;

@RestController
@RequestMapping("/api/tipos-arte")
public class TipoArteController {

    private final TipoArteService tipoArteService;

    public TipoArteController(TipoArteService tipoArteService) {
        this.tipoArteService = tipoArteService;
    }

    @GetMapping
    public List<TipoArteDTO> listarActivos() {
        return tipoArteService.listarActivos();
    }
}
