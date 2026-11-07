package com.temipora.backend_springboot.controladores;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ConfiguracionSustitucionDTO;
import com.temipora.backend_springboot.modelo.SustitucionResumenDTO;
import com.temipora.backend_springboot.servicios.CierreEmailService;
import com.temipora.backend_springboot.servicios.ConfiguracionSustitucionService;
import com.temipora.backend_springboot.servicios.SustitucionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class SustitucionAdminController {

    private final ConfiguracionSustitucionService configuracionService;
    private final SustitucionService sustitucionService;
    private final CierreEmailService cierreEmailService;

    public SustitucionAdminController(
            ConfiguracionSustitucionService configuracionService,
            SustitucionService sustitucionService,
            CierreEmailService cierreEmailService) {
        this.configuracionService = configuracionService;
        this.sustitucionService = sustitucionService;
        this.cierreEmailService = cierreEmailService;
    }

    @GetMapping("/configuracion-sustitucion")
    public ConfiguracionSustitucionDTO obtener() {
        return configuracionService.obtenerDto();
    }

    @PutMapping("/configuracion-sustitucion")
    public ConfiguracionSustitucionDTO actualizar(
            @Valid @RequestBody ConfiguracionSustitucionDTO request) {
        return configuracionService.actualizar(request);
    }

    @GetMapping("/sustituciones/resumen")
    public SustitucionResumenDTO resumen() {
        SustitucionResumenDTO resumen = sustitucionService.resumen();
        resumen.setEmailsCierrePendientes(cierreEmailService.pendientes());
        return resumen;
    }

    @PostMapping("/cierres-email/procesar")
    public Map<String, Object> procesarCierres() {
        int enviados = cierreEmailService.procesarPendientes(true);
        return Map.of(
            "enviados", enviados,
            "pendientes", cierreEmailService.pendientes());
    }
}
