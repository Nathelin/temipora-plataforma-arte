package com.temipora.backend_springboot.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InformeSustitucionesHistorialDTO(
        LocalDate desde,
        LocalDate hasta,
        long dias,
        String filtro,
        LocalDateTime generadoEn,
        ResumenSustitucionesHistorialDTO resumen,
        List<SustitucionHistorialDTO> eventos,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas,
        boolean hayMas) {
}
