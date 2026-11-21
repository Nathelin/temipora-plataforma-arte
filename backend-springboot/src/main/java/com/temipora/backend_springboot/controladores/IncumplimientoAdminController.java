package com.temipora.backend_springboot.controladores;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.servicios.MoraService;
import com.temipora.backend_springboot.servicios.SancionIncumplimientoService;

/**
 * Permite disparar a mano el ciclo de incumplimientos desde el panel, para
 * verificar el comportamiento sin esperar a los jobs programados.
 */
@RestController
@RequestMapping("/api/admin/incumplimientos")
@PreAuthorize("hasRole('ADMIN')")
public class IncumplimientoAdminController {

    private final MoraService moraService;
    private final SancionIncumplimientoService incumplimientoService;

    public IncumplimientoAdminController(
            MoraService moraService,
            SancionIncumplimientoService incumplimientoService) {
        this.moraService = moraService;
        this.incumplimientoService = incumplimientoService;
    }

    @PostMapping("/procesar")
    public Map<String, Object> procesar() {
        int moras = moraService.detectar();
        SancionIncumplimientoService.Resumen resumen =
            incumplimientoService.procesar();

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("morasRegistradas", moras);
        respuesta.put("sancionesCuradas", resumen.curados());
        respuesta.put("sancionesEscaladas", resumen.escalados());
        respuesta.put("nivel1Aplicados", resumen.nivel1Aplicados());
        respuesta.put("pedidosCerrados", resumen.pedidosCerrados());
        return respuesta;
    }
}
