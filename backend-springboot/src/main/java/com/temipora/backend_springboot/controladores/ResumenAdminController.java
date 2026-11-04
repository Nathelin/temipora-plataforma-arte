package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ResumenComunidadDTO;
import com.temipora.backend_springboot.modelo.ResumenNegocioDTO;
import com.temipora.backend_springboot.modelo.ResumenOperacionDTO;
import com.temipora.backend_springboot.servicios.ResumenAdminService;

@RestController
@RequestMapping("/api/admin/resumen")
@PreAuthorize("hasRole('ADMIN')")
public class ResumenAdminController {

    private final ResumenAdminService resumenService;

    public ResumenAdminController(ResumenAdminService resumenService) {
        this.resumenService = resumenService;
    }

    @GetMapping("/negocio")
    public ResumenNegocioDTO negocio() {
        return resumenService.negocio();
    }

    @GetMapping("/operacion")
    public ResumenOperacionDTO operacion() {
        return resumenService.operacion();
    }

    @GetMapping("/comunidad")
    public ResumenComunidadDTO comunidad() {
        return resumenService.comunidad();
    }
}
