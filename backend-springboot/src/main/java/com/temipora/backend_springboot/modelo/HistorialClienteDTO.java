package com.temipora.backend_springboot.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HistorialClienteDTO(
        LocalDate desde,
        LocalDate hasta,
        String periodoTexto,
        String filtroTexto,
        LocalDateTime generadoEn,
        HistorialResumenDTO resumen,
        List<HistorialItemDTO> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas,
        boolean hayMas) {
}
