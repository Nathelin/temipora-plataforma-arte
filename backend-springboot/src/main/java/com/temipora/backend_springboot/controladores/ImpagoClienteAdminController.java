package com.temipora.backend_springboot.controladores;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.servicios.MoraClienteService;
import com.temipora.backend_springboot.servicios.SancionMoraClienteService;

/**
 * Permite disparar a mano el ciclo de impagos del cliente desde el panel, para
 * verificar el comportamiento sin esperar a los jobs programados.
 */
@RestController
@RequestMapping("/api/admin/impagos-cliente")
@PreAuthorize("hasRole('ADMIN')")
public class ImpagoClienteAdminController {

    private final MoraClienteService moraClienteService;
    private final SancionMoraClienteService sancionMoraClienteService;

    public ImpagoClienteAdminController(
            MoraClienteService moraClienteService,
            SancionMoraClienteService sancionMoraClienteService) {
        this.moraClienteService = moraClienteService;
        this.sancionMoraClienteService = sancionMoraClienteService;
    }

    @PostMapping("/procesar")
    public Map<String, Object> procesar() {
        int impagos = moraClienteService.detectar();
        SancionMoraClienteService.Resumen resumen =
            sancionMoraClienteService.procesar();

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("impagosRegistrados", impagos);
        respuesta.put("sancionesCuradas", resumen.curados());
        respuesta.put("sancionesEscaladas", resumen.escalados());
        respuesta.put("nivel1Aplicados", resumen.nivel1Aplicados());
        return respuesta;
    }
}
