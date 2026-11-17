package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.temipora.backend_springboot.modelo.InformeComparacionDTO;
import com.temipora.backend_springboot.modelo.InformeItemDTO;
import com.temipora.backend_springboot.modelo.InformePeriodoDTO;
import com.temipora.backend_springboot.modelo.InformeSerieDTO;
import com.temipora.backend_springboot.modelo.InformeTotalesDTO;
import com.temipora.backend_springboot.modelo.InformeVentasDTO;

class InformePdfServiceTest {

    private final InformePdfService servicio = new InformePdfService();

    @Test
    void generaUnPdfConTotalesComparacionYDetalle() {
        byte[] pdf = servicio.generar(informeConItems(), "artista");

        assertThat(pdf).isNotEmpty();
        assertThat(texto(pdf)).startsWith("%PDF");
    }

    @Test
    void generaUnPdfSinVentasYAvisaDeImportesEstimados() {
        InformeVentasDTO informe = new InformeVentasDTO(
            new InformePeriodoDTO(LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), 30, "DIA"),
            "ARS",
            LocalDateTime.now(),
            new InformeTotalesDTO(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, BigDecimal.ZERO),
            new InformeComparacionDTO(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                BigDecimal.ZERO, BigDecimal.ZERO, null),
            0,
            List.of(),
            List.of());

        byte[] pdf = servicio.generar(informe, "artista");

        assertThat(pdf).isNotEmpty();
        assertThat(texto(pdf)).startsWith("%PDF");
    }

    private InformeVentasDTO informeConItems() {
        return new InformeVentasDTO(
            new InformePeriodoDTO(LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), 30, "MES"),
            "ARS",
            LocalDateTime.now(),
            new InformeTotalesDTO(
                new BigDecimal("350.00"), new BigDecimal("299.25"),
                new BigDecimal("50.75"), new BigDecimal("35.00"),
                new BigDecimal("15.75"), 2, 4, new BigDecimal("149.63")),
            new InformeComparacionDTO(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                new BigDecimal("100.00"), new BigDecimal("90.00"),
                new BigDecimal("232.50")),
            1,
            List.of(new InformeSerieDTO(
                "2026-11", new BigDecimal("350.00"),
                new BigDecimal("299.25"), 3)),
            List.of(
                new InformeItemDTO("SERVICIO", 7L, "Ilustración", 2, 1,
                    new BigDecimal("150.00"), new BigDecimal("15.00"),
                    new BigDecimal("6.75"), new BigDecimal("128.25"),
                    new BigDecimal("42.86")),
                new InformeItemDTO("PRODUCTO", 30L, "Obra A", 1, 1,
                    new BigDecimal("200.00"), new BigDecimal("20.00"),
                    new BigDecimal("9.00"), new BigDecimal("171.00"),
                    new BigDecimal("57.14"))));
    }

    private String texto(byte[] pdf) {
        return new String(pdf, 0, Math.min(pdf.length, 8),
            StandardCharsets.ISO_8859_1);
    }
}
