package com.temipora.backend_springboot.servicios;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IndicadorEscrituraEnMemoria implements IndicadorEscritura {

    private static final Duration VENTANA = Duration.ofSeconds(5);
    private static final Duration MINIMO_ENTRE_LATIDOS = Duration.ofSeconds(1);

    private final Duration ventana;
    private final Map<Long, Map<Long, Instant>> latidos = new ConcurrentHashMap<>();

    public IndicadorEscrituraEnMemoria() {
        this(VENTANA);
    }

    IndicadorEscrituraEnMemoria(Duration ventana) {
        this.ventana = ventana;
    }

    @Override
    public void marcar(Long idConversacion, Long idUsuario) {
        if (idConversacion == null || idUsuario == null) {
            return;
        }
        Instant ahora = Instant.now();
        latidos.computeIfAbsent(idConversacion, clave -> new ConcurrentHashMap<>())
            .compute(idUsuario, (clave, ultimo) ->
                ultimo != null
                    && Duration.between(ultimo, ahora)
                        .compareTo(MINIMO_ENTRE_LATIDOS) < 0
                            ? ultimo
                            : ahora);
    }

    @Override
    public void limpiar(Long idConversacion, Long idUsuario) {
        if (idConversacion == null || idUsuario == null) {
            return;
        }
        Map<Long, Instant> porUsuario = latidos.get(idConversacion);
        if (porUsuario == null) {
            return;
        }
        porUsuario.remove(idUsuario);
        if (porUsuario.isEmpty()) {
            latidos.remove(idConversacion);
        }
    }

    @Override
    public boolean estaEscribiendo(Long idConversacion, Long idUsuario) {
        if (idConversacion == null || idUsuario == null) {
            return false;
        }
        Map<Long, Instant> porUsuario = latidos.get(idConversacion);
        if (porUsuario == null) {
            return false;
        }
        Instant ultimo = porUsuario.get(idUsuario);
        if (ultimo == null) {
            return false;
        }
        if (!vigente(ultimo)) {
            limpiar(idConversacion, idUsuario);
            return false;
        }
        return true;
    }

    @Scheduled(fixedDelay = 60_000)
    public void limpiarVencidos() {
        for (Map.Entry<Long, Map<Long, Instant>> entrada : latidos.entrySet()) {
            Map<Long, Instant> porUsuario = entrada.getValue();
            porUsuario.entrySet().removeIf(item -> !vigente(item.getValue()));
            if (porUsuario.isEmpty()) {
                latidos.remove(entrada.getKey());
            }
        }
    }

    private boolean vigente(Instant ultimo) {
        return Duration.between(ultimo, Instant.now()).compareTo(ventana) < 0;
    }
}
