package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.MedioDTO;
import com.temipora.backend_springboot.servicios.MedioService;

@RestController
@RequestMapping("/api/medios")
public class MedioController {

    private final MedioService medioService;

    public MedioController(MedioService medioService) {
        this.medioService = medioService;
    }

    @GetMapping
    public List<MedioDTO> listar() {
        return medioService.listarActivos();
    }
}
