package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.TerminosPlataformaDTO;
import com.temipora.backend_springboot.modelo.TerminosPlataformaRequest;
import com.temipora.backend_springboot.servicios.TerminosPlataformaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/terminos-plataforma")
@PreAuthorize("hasRole('ADMIN')")
public class TerminosPlataformaAdminController {

    private final TerminosPlataformaService terminosPlataformaService;

    public TerminosPlataformaAdminController(
            TerminosPlataformaService terminosPlataformaService) {
        this.terminosPlataformaService = terminosPlataformaService;
    }

    @GetMapping
    public List<TerminosPlataformaDTO> listar() {
        return terminosPlataformaService.listarVersiones();
    }

    @PostMapping
    public TerminosPlataformaDTO publicar(
            @Valid @RequestBody TerminosPlataformaRequest request) {
        return terminosPlataformaService.publicar(request);
    }
}
