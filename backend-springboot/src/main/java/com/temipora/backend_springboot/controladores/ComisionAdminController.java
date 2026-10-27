package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ComisionConfigDTO;
import com.temipora.backend_springboot.modelo.ComisionConfigRequest;
import com.temipora.backend_springboot.servicios.ComisionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/configuracion-comision")
@PreAuthorize("hasRole('ADMIN')")
public class ComisionAdminController {

    private final ComisionService comisionService;

    public ComisionAdminController(ComisionService comisionService) {
        this.comisionService = comisionService;
    }

    @GetMapping
    public ComisionConfigDTO obtener() {
        return comisionService.obtenerEstado();
    }

    @PutMapping
    public ComisionConfigDTO actualizar(
            @Valid @RequestBody ComisionConfigRequest request) {
        return comisionService.actualizar(request);
    }
}
