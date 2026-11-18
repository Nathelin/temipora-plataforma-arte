package com.temipora.backend_springboot.modelo;

import java.time.LocalDate;

public record InformePeriodoDTO(
        LocalDate desde,
        LocalDate hasta,
        long dias,
        String agrupacion) {
}
