package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BajaCuentaResultadoDTO {

    private String message;
    private LocalDateTime reactivarHasta;
}
