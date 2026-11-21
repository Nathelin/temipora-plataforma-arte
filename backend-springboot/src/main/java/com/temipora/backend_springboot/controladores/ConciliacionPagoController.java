package com.temipora.backend_springboot.controladores;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ConciliacionResultadoDTO;
import com.temipora.backend_springboot.servicios.ConciliacionMercadoPagoService;

@RestController
@RequestMapping("/api/admin/pagos")
@PreAuthorize("hasRole('ADMIN')")
public class ConciliacionPagoController {

    private final ConciliacionMercadoPagoService conciliacionService;

    public ConciliacionPagoController(
            ConciliacionMercadoPagoService conciliacionService) {
        this.conciliacionService = conciliacionService;
    }

    @PostMapping("/conciliacion")
    public ConciliacionResultadoDTO conciliar() {
        return conciliacionService.conciliar();
    }
}
