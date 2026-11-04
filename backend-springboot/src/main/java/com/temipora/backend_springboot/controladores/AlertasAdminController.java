package com.temipora.backend_springboot.controladores;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.AlertaAdminDTO;
import com.temipora.backend_springboot.servicios.AlertasAdminService;

@RestController
@RequestMapping("/api/admin/resumen")
@PreAuthorize("hasRole('ADMIN')")
public class AlertasAdminController {

    private final AlertasAdminService alertasService;

    public AlertasAdminController(AlertasAdminService alertasService) {
        this.alertasService = alertasService;
    }

    @GetMapping("/alertas")
    public List<AlertaAdminDTO> alertas() {
        return alertasService.listar();
    }
}
