package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.temipora.backend_springboot.modelo.InformeAdminComparacionDTO;
import com.temipora.backend_springboot.modelo.InformeAdminDTO;
import com.temipora.backend_springboot.modelo.InformeAdminItemDTO;
import com.temipora.backend_springboot.modelo.InformeAdminSerieDTO;
import com.temipora.backend_springboot.modelo.InformeAdminTipoDTO;
import com.temipora.backend_springboot.modelo.InformeAdminTotalesDTO;
import com.temipora.backend_springboot.modelo.InformeAdminVendedorDTO;
import com.temipora.backend_springboot.modelo.InformePeriodoDTO;

class InformeAdminPdfServiceTest {

    private final InformeAdminPdfService servicio = new InformeAdminPdfService();

    @Test
    void generaUnPdfConResumenYDesgloses() {
        InformeAdminDTO informe = new InformeAdminDTO(
            new InformePeriodoDTO(LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), 30, "DIA"),
            "ARS",
            LocalDateTime.now(),
            new InformeAdminTotalesDTO(
                new BigDecimal("350.00"), new BigDecimal("35.00"),
                new BigDecimal("315.00"), new BigDecimal("15.75"),
                new BigDecimal("299.25"), 2, 4, new BigDecimal("175.00")),
            new InformeAdminComparacionDTO(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                new BigDecimal("100.00"), new BigDecimal("10.00"),
                new BigDecimal("250.00"), new BigDecimal("250.00")),
            1,
            List.of(new InformeAdminSerieDTO(
                "2026-11", new BigDecimal("350.00"),
                new BigDecimal("35.00"), 3)),
            List.of(
                new InformeAdminTipoDTO("PRODUCTO", 1, 2,
                    new BigDecimal("200.00"), new BigDecimal("20.00"),
                    new BigDecimal("180.00"), new BigDecimal("9.00")),
                new InformeAdminTipoDTO("SERVICIO", 1, 2,
                    new BigDecimal("150.00"), new BigDecimal("15.00"),
                    new BigDecimal("135.00"), new BigDecimal("6.75"))),
            List.of(new InformeAdminVendedorDTO(
                2L, "artista1", 2, 4, new BigDecimal("350.00"),
                new BigDecimal("35.00"), new BigDecimal("315.00"),
                new BigDecimal("15.75"))),
            List.of(new InformeAdminItemDTO(
                "SERVICIO", 7L, "Ilustración", 1, 2,
                new BigDecimal("150.00"), new BigDecimal("15.00"),
                new BigDecimal("135.00"), new BigDecimal("6.75"))));

        byte[] pdf = servicio.generar(informe);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1))
            .isEqualTo("%PDF");
    }

    @Test
    void generaUnPdfSinVentas() {
        InformeAdminDTO informe = new InformeAdminDTO(
            new InformePeriodoDTO(LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), 30, "DIA"),
            "ARS",
            LocalDateTime.now(),
            new InformeAdminTotalesDTO(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, BigDecimal.ZERO),
            null,
            0,
            List.of(),
            List.of(),
            List.of(),
            List.of());

        byte[] pdf = servicio.generar(informe);

        assertThat(pdf).isNotEmpty();
    }
}
