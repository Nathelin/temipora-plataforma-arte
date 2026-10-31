package com.temipora.backend_springboot.servicios;

final class TextoMensajeria {

    private TextoMensajeria() {
    }

    static String obligatorio(String valor, int maximo, String etiqueta) {
        if (valor == null) {
            throw new IllegalArgumentException(etiqueta + " es obligatorio.");
        }
        String limpio = valor.replace("\u0000", "").trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException(etiqueta + " es obligatorio.");
        }
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException(
                etiqueta + " no puede superar los " + maximo + " caracteres.");
        }
        return limpio;
    }

    static String opcional(String valor, int maximo, String etiqueta) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.replace("\u0000", "").trim();
        if (limpio.isEmpty()) {
            return null;
        }
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException(
                etiqueta + " no puede superar los " + maximo + " caracteres.");
        }
        return limpio;
    }

    static String recortar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.replaceAll("\\s+", " ").trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo) + "…";
    }
}
