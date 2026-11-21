package com.temipora.backend_springboot.servicios;

import java.util.Optional;

/**
 * Código público de una aceptación de términos: el id de la solicitud en base36
 * y los primeros caracteres de la huella del texto aceptado. No requiere
 * columna ni backfill: es determinista y la huella impide adivinarlo.
 */
public final class CodigoTerminos {

    private static final String PREFIJO = "TA-";
    private static final int LARGO_HUELLA = 16;

    private CodigoTerminos() {
    }

    public record Datos(Long idSolicitud, String huella) {
    }

    public static String codificar(Long idSolicitud, String huella) {
        if (idSolicitud == null || huella == null
                || huella.length() < LARGO_HUELLA) {
            throw new IllegalArgumentException(
                "La aceptación no tiene solicitud o huella suficiente.");
        }
        return PREFIJO + Long.toString(idSolicitud, 36).toUpperCase()
            + "-" + huella.substring(0, LARGO_HUELLA).toLowerCase();
    }

    public static Optional<Datos> decodificar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        String limpio = codigo.trim();
        if (!limpio.regionMatches(true, 0, PREFIJO, 0, PREFIJO.length())) {
            return Optional.empty();
        }
        String cuerpo = limpio.substring(PREFIJO.length());
        int guion = cuerpo.lastIndexOf('-');
        if (guion <= 0 || guion == cuerpo.length() - 1) {
            return Optional.empty();
        }
        String huella = cuerpo.substring(guion + 1).toLowerCase();
        if (huella.length() != LARGO_HUELLA
                || !huella.matches("[0-9a-f]+")) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Datos(
                Long.parseLong(cuerpo.substring(0, guion), 36), huella));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
