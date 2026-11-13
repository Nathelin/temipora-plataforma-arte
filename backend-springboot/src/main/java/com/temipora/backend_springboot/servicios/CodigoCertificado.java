package com.temipora.backend_springboot.servicios;

import java.util.Optional;

/**
 * Codigo publico y opaco de un certificado de autenticidad. Se deriva del id de
 * la variante (pieza unica) en base36, con prefijo "TP-". No requiere columna
 * ni backfill: es determinista y reversible.
 */
public final class CodigoCertificado {

    private static final String PREFIJO = "TP-";

    private CodigoCertificado() {
    }

    public static String codificar(Long idVariante) {
        if (idVariante == null) {
            throw new IllegalArgumentException("La variante no tiene identificador.");
        }
        return PREFIJO + Long.toString(idVariante, 36).toUpperCase();
    }

    public static Optional<Long> decodificar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        String limpio = codigo.trim();
        if (!limpio.regionMatches(true, 0, PREFIJO, 0, PREFIJO.length())) {
            return Optional.empty();
        }
        String cuerpo = limpio.substring(PREFIJO.length());
        if (cuerpo.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(cuerpo, 36));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
