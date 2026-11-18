package com.temipora.backend_springboot.controladores;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.PublicacionReventaDTO;
import com.temipora.backend_springboot.servicios.ReventaService;

@RestController
@RequestMapping("/api/admin/reventas")
@PreAuthorize("hasRole('ADMIN')")
public class ReventaAdminController {

    private final ReventaService reventaService;

    public ReventaAdminController(ReventaService reventaService) {
        this.reventaService = reventaService;
    }

    @PatchMapping("/{idPublicacion}/suspender")
    public PublicacionReventaDTO suspender(
            @PathVariable Long idPublicacion,
            @RequestBody(required = false) Map<String, String> body) {
        return reventaService.suspender(idPublicacion,
            body != null ? body.get("motivo") : null);
    }
}
