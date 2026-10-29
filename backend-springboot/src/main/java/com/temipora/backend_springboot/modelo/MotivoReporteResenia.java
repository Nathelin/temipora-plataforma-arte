package com.temipora.backend_springboot.modelo;

import lombok.Getter;

@Getter
public enum MotivoReporteResenia {
    OFENSIVA("Contenido ofensivo"),
    SPAM("Spam o publicidad"),
    DATOS_PERSONALES("Datos personales"),
    FALSA("Información falsa"),
    OTRO("Otro motivo");

    private final String etiqueta;

    MotivoReporteResenia(String etiqueta) {
        this.etiqueta = etiqueta;
    }
}
