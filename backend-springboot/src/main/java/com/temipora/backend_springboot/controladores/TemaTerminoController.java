package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.TemaTerminoDTO;
import com.temipora.backend_springboot.repositorios.TemaTerminoRepository;

@RestController
@RequestMapping("/api/temas-termino")
public class TemaTerminoController {

    private final TemaTerminoRepository temaTerminoRepository;

    public TemaTerminoController(TemaTerminoRepository temaTerminoRepository) {
        this.temaTerminoRepository = temaTerminoRepository;
    }

    @GetMapping
    public List<TemaTerminoDTO> listar() {
        return temaTerminoRepository.findByActivoTrueOrderByOrdenAsc().stream()
            .map(TemaTerminoDTO::desdeTema)
            .toList();
    }
}
