package com.temipora.backend_springboot.controladores;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.servicios.PedidoService;

/**
 * Permite al admin forzar el procesamiento de hitos (autoaprobaciones y
 * vencimientos) sin esperar al job programado. Útil para pruebas y soporte.
 */
@RestController
@RequestMapping("/api/admin/pedidos")
@PreAuthorize("hasRole('ADMIN')")
public class PedidoProcesoAdminController {

    private final PedidoService pedidoService;

    public PedidoProcesoAdminController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping("/procesar")
    public Map<String, Integer> procesar() {
        int aprobados = pedidoService.aprobarAutomaticamente();
        int expirados = pedidoService.expirarHitos();
        return Map.of(
            "aprobados", aprobados,
            "expirados", expirados);
    }
}
