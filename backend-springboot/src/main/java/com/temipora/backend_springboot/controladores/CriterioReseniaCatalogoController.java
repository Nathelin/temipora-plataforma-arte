package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.AmbitoResenia;
import com.temipora.backend_springboot.modelo.CriterioReseniaDTO;
import com.temipora.backend_springboot.servicios.CriterioReseniaService;

@RestController
@RequestMapping("/api/criterios-resenia")
@PreAuthorize("isAuthenticated()")
public class CriterioReseniaCatalogoController {

    private final CriterioReseniaService criterioReseniaService;

    public CriterioReseniaCatalogoController(
            CriterioReseniaService criterioReseniaService) {
        this.criterioReseniaService = criterioReseniaService;
    }

    @GetMapping
    public List<CriterioReseniaDTO> listar(
            @RequestParam AmbitoResenia ambito) {
        return criterioReseniaService.listarActivos(ambito);
    }
}
