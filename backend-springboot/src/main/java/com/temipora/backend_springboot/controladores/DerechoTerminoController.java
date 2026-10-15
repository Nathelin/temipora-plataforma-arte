package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.DerechoTerminoDTO;
import com.temipora.backend_springboot.repositorios.DerechoTerminoRepository;

@RestController
@RequestMapping("/api/derechos-termino")
public class DerechoTerminoController {

    private final DerechoTerminoRepository derechoTerminoRepository;

    public DerechoTerminoController(DerechoTerminoRepository derechoTerminoRepository) {
        this.derechoTerminoRepository = derechoTerminoRepository;
    }

    @GetMapping
    public List<DerechoTerminoDTO> listar() {
        return derechoTerminoRepository.findByActivoTrueOrderByOrdenAsc().stream()
            .map(DerechoTerminoDTO::desdeDerecho)
            .toList();
    }
}
