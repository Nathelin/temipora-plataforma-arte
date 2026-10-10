package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.MaterialDTO;
import com.temipora.backend_springboot.servicios.MaterialService;

@RestController
@RequestMapping("/api/materiales")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public List<MaterialDTO> selector(@RequestParam(required = false) String idMedio) {
        return materialService.selector(parseIdMedio(idMedio));
    }

    private Long parseIdMedio(String idMedio) {
        if (idMedio == null || idMedio.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(idMedio.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Medio no válido: " + idMedio);
        }
    }
}
