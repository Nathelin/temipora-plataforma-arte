package com.temipora.backend_springboot.servicios.informes;

/**
 * Usuario autenticado que solicitó la emisión de un documento PDF. Se imprime
 * en los metadatos y en el pie de todas las páginas para dejar constancia de
 * quién generó cada informe o constancia.
 */
public record EmisorPdf(String usuario, String nombre, String rol) {

    private static final String SIN_IDENTIFICAR = "Usuario no identificado";

    public static EmisorPdf de(String usuario, String nombre, String rol) {
        return new EmisorPdf(limpiar(usuario), limpiar(nombre), limpiar(rol));
    }

    /** Nombre para mostrar: el visible si existe, o el nombre de usuario. */
    public String nombreVisible() {
        if (nombre != null) {
            return nombre;
        }
        if (usuario != null) {
            return usuario;
        }
        return SIN_IDENTIFICAR;
    }

    /** Descripción completa, por ejemplo "@ana · Ana Pérez (Artista)". */
    public String descripcion() {
        StringBuilder texto = new StringBuilder();
        if (usuario != null) {
            texto.append('@').append(usuario);
            if (nombre != null && !nombre.equalsIgnoreCase(usuario)) {
                texto.append(" · ").append(nombre);
            }
        } else if (nombre != null) {
            texto.append(nombre);
        } else {
            return SIN_IDENTIFICAR;
        }
        if (rol != null) {
            texto.append(" (").append(rolLegible(rol)).append(')');
        }
        return texto.toString();
    }

    private static String rolLegible(String rol) {
        return switch (rol) {
            case "ADMIN" -> "Administración";
            case "ARTISTA" -> "Artista";
            case "CLIENTE" -> "Cliente";
            default -> rol;
        };
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
