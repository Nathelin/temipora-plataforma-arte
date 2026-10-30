package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

public record CuentaCobroDTO(
        boolean conectada,
        String estado,
        String idCuentaExterna,
        LocalDateTime expiraEn) {
}
