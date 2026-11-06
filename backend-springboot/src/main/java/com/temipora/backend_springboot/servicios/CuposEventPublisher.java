package com.temipora.backend_springboot.servicios;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.eventos.CuposServicioCambiadosEvent;

/**
 * Punto único para avisar que cambiaron los cupos ocupados de un servicio.
 * El evento se procesa después del commit, así que un fallo al reconciliar
 * el modo de recepción nunca revierte la operación de negocio.
 */
@Service
public class CuposEventPublisher {

    private final ApplicationEventPublisher publisher;

    public CuposEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publicar(Long idServicio) {
        if (idServicio != null) {
            publisher.publishEvent(new CuposServicioCambiadosEvent(idServicio));
        }
    }
}
