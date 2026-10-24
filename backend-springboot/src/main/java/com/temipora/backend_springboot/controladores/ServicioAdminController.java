package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.servicios.ServicioAdminService;

@RestController
@RequestMapping("/api/admin/servicios")
@PreAuthorize("hasRole('ADMIN')")
public class ServicioAdminController {

    private final ServicioAdminService servicioAdminService;

    public ServicioAdminController(ServicioAdminService servicioAdminService) {
        this.servicioAdminService = servicioAdminService;
    }

    @GetMapping("/archivados")
    public List<ServicioDTO> listarArchivados() {
        return servicioAdminService.listarArchivados();
    }

    @PostMapping("/{id}/restaurar")
    public ServicioDTO restaurar(@PathVariable Long id) {
        return servicioAdminService.restaurar(id);
    }
}
