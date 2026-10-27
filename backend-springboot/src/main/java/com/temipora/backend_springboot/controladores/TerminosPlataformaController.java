package com.temipora.backend_springboot.controladores;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.TerminosPlataformaDTO;
import com.temipora.backend_springboot.servicios.TerminosPlataformaService;

@RestController
@RequestMapping("/api/terminos-plataforma")
public class TerminosPlataformaController {

    private final TerminosPlataformaService terminosPlataformaService;

    public TerminosPlataformaController(
            TerminosPlataformaService terminosPlataformaService) {
        this.terminosPlataformaService = terminosPlataformaService;
    }

    @GetMapping
    public TerminosPlataformaDTO vigente() {
        return terminosPlataformaService.obtenerVigente();
    }
}
