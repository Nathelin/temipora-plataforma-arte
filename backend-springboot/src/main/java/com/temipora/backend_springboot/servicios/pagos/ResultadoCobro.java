package com.temipora.backend_springboot.servicios.pagos;

import java.time.LocalDateTime;

public record ResultadoCobro(
        String idExterno,
        String instruccion,
        LocalDateTime expiraEn) {

    public ResultadoCobro(String idExterno, String instruccion) {
        this(idExterno, instruccion, null);
    }
}
