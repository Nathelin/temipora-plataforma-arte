package com.temipora.backend_springboot.tareas;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.servicios.ReservaStockService;

@Component
public class CompraVencimientoJob {

    private final ReservaStockService reservaStockService;

    public CompraVencimientoJob(ReservaStockService reservaStockService) {
        this.reservaStockService = reservaStockService;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT30S")
    public void ejecutar() {
        reservaStockService.expirarVencidas();
    }
}
