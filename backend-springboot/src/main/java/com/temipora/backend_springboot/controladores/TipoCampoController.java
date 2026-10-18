package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.TipoCampoDTO;
import com.temipora.backend_springboot.repositorios.TipoCampoRepository;

@RestController
@RequestMapping("/api/tipos-campo")
public class TipoCampoController {

    private final TipoCampoRepository tipoCampoRepository;

    public TipoCampoController(TipoCampoRepository tipoCampoRepository) {
        this.tipoCampoRepository = tipoCampoRepository;
    }

    @GetMapping
    public List<TipoCampoDTO> listar() {
        return tipoCampoRepository.findByActivoTrueOrderByNombreAsc().stream()
            .map(TipoCampoDTO::desdeTipoCampo)
            .toList();
    }
}
