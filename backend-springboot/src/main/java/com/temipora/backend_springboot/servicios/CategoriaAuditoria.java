package com.temipora.backend_springboot.servicios;

public enum CategoriaAuditoria {

    COMERCIO("Comercio"),
    USUARIOS("Usuarios y seguridad"),
    MODERACION("Moderación"),
    CATALOGO("Catálogo"),
    CONFIGURACION("Configuración"),
    LEGAL("Legal"),
    SISTEMA("Sistema"),
    OTROS("Otros");

    private final String etiqueta;

    CategoriaAuditoria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
