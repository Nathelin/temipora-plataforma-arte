package com.temipora.backend_springboot.servicios;

/**
 * Nombre con el que se muestra una pieza puesta en reventa. Los productos sin
 * variantes usan una presentación única llamada «Única»: mostrar ese nombre no
 * aporta información, así que se usa el título del producto.
 */
public final class NombrePiezaVisible {

    private NombrePiezaVisible() {
    }

    public static boolean esPresentacionUnica(String nombreVariante) {
        if (nombreVariante == null) {
            return true;
        }
        String nombre = nombreVariante.trim();
        return nombre.isEmpty()
            || nombre.equalsIgnoreCase("Única")
            || nombre.equalsIgnoreCase("Unica");
    }

    public static String nombreDeVariante(String nombreVariante) {
        return esPresentacionUnica(nombreVariante) ? null : nombreVariante.trim();
    }

    public static String deReventa(String nombreVariante, String tituloProducto) {
        String nombre = nombreDeVariante(nombreVariante);
        return nombre != null ? nombre : tituloProducto;
    }
}
