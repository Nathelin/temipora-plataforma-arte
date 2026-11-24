package com.temipora.backend_springboot.controladores;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.servicios.AccionesAdmin;
import com.temipora.backend_springboot.servicios.AuditoriaService;
import com.temipora.backend_springboot.servicios.PedidoService;
import com.temipora.backend_springboot.servicios.ProrrogaService;

/**
 * Permite al admin forzar el procesamiento de hitos (autoaprobaciones y
 * vencimientos) sin esperar al job programado. Útil para pruebas y soporte.
 */
@RestController
@RequestMapping("/api/admin/pedidos")
@PreAuthorize("hasRole('ADMIN')")
public class PedidoProcesoAdminController {

    private final PedidoService pedidoService;
    private final ProrrogaService prorrogaService;
    private final AuditoriaService auditoria;

    public PedidoProcesoAdminController(
            PedidoService pedidoService,
            ProrrogaService prorrogaService,
            AuditoriaService auditoria) {
        this.pedidoService = pedidoService;
        this.prorrogaService = prorrogaService;
        this.auditoria = auditoria;
    }

    @PostMapping("/procesar")
    public Map<String, Integer> procesar() {
        int aprobados = pedidoService.aprobarAutomaticamente();
        int expirados = pedidoService.expirarHitos();
        int prorrogasVencidas = prorrogaService.expirarVencidas();
        auditoria.registrarSistema(
            AccionesAdmin.PEDIDO_PROCESAR, "Pedido", null,
            "Procesó hitos: " + aprobados + " aprobado(s), " + expirados
                + " expirado(s); " + prorrogasVencidas
                + " prórroga(s) vencida(s).");
        return Map.of(
            "aprobados", aprobados,
            "expirados", expirados,
            "prorrogasVencidas", prorrogasVencidas);
    }
}
