package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.CotizacionConfigDTO;
import com.temipora.backend_springboot.modelo.CotizacionConfigRequest;
import com.temipora.backend_springboot.servicios.CotizacionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/cotizacion")
@PreAuthorize("hasRole('ADMIN')")
public class CotizacionAdminController {

    private final CotizacionService cotizacionService;

    public CotizacionAdminController(CotizacionService cotizacionService) {
        this.cotizacionService = cotizacionService;
    }

    @GetMapping
    public CotizacionConfigDTO obtener() {
        return cotizacionService.obtenerEstado();
    }

    @PutMapping
    public CotizacionConfigDTO actualizar(@Valid @RequestBody CotizacionConfigRequest request) {
        return cotizacionService.actualizar(request);
    }

    @PostMapping("/refrescar")
    public CotizacionConfigDTO refrescar() {
        return cotizacionService.refrescarDesdeApi();
    }
}
