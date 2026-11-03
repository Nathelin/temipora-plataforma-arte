package com.temipora.backend_springboot.servicios;

public record CampoAuditado(String campo, String etiqueta) {

    public static CampoAuditado de(String campo, String etiqueta) {
        return new CampoAuditado(campo, etiqueta);
    }
}
