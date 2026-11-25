package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.temipora.backend_springboot.modelo.InformeSustitucionesHistorialDTO;
import com.temipora.backend_springboot.modelo.ResumenSustitucionesHistorialDTO;
import com.temipora.backend_springboot.modelo.SustitucionConteoDTO;
import com.temipora.backend_springboot.modelo.SustitucionHistorialDTO;

class InformeSustitucionesPdfServiceTest {

    private final InformeSustitucionesPdfService servicio =
        new InformeSustitucionesPdfService();

    @Test
    void generaElPdfConResumenMotivosYEventos() throws Exception {
        String texto = texto(servicio.generar(informe(eventos(), resumen(2))));

        assertThat(texto)
            .contains("Historial de eventos")
            .contains("SUST-20261101-20261121")
            .contains("Resumen del período")
            .contains("Motivos de la búsqueda")
            .contains("Sin cupos")
            .contains("Eventos")
            .contains("cliente1")
            .contains("Alternativa: Ilustración")
            .contains("Total");
    }

    @Test
    void informaCuandoNoHayEventos() throws Exception {
        String texto = texto(servicio.generar(
            informe(List.of(), resumen(0))));

        assertThat(texto)
            .contains("No hay eventos de sustitución en el período.")
            .contains("No hay eventos de sustitución para los filtros elegidos.");
    }

    private InformeSustitucionesHistorialDTO informe(
            List<SustitucionHistorialDTO> eventos,
            ResumenSustitucionesHistorialDTO resumen) {
        return new InformeSustitucionesHistorialDTO(
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 21),
            21,
            "Todos los eventos",
            LocalDateTime.of(2026, 11, 21, 10, 0),
            resumen,
            eventos,
            0,
            20,
            eventos.size(),
            eventos.isEmpty() ? 0 : 1,
            false);
    }

    private ResumenSustitucionesHistorialDTO resumen(int total) {
        List<SustitucionConteoDTO> motivos = total == 0
            ? List.of()
            : List.of(new SustitucionConteoDTO(
                "SIN_CUPOS", "Sin cupos", total));
        return new ResumenSustitucionesHistorialDTO(
            total,
            total,
            0,
            0,
            0,
            0,
            0,
            0,
            new BigDecimal("100.00"),
            new BigDecimal("4.50"),
            motivos);
    }

    private List<SustitucionHistorialDTO> eventos() {
        return List.of(evento(11L, "cliente1", "Alternativa: Ilustración"));
    }

    private SustitucionHistorialDTO evento(
            Long id, String cliente, String resultado) {
        return new SustitucionHistorialDTO(
            id,
            LocalDateTime.of(2026, 11, 20, 12, 0),
            LocalDateTime.of(2026, 11, 20, 12, 30),
            "RESUELTA_ALTERNATIVA",
            "Resuelta con alternativa",
            "SIN_CUPOS",
            "Sin cupos",
            "DETALLE",
            "Desde el detalle",
            1L,
            cliente,
            "Cliente de prueba",
            7L,
            "Retrato",
            2L,
            "artista1",
            4,
            9L,
            "Ilustración",
            15L,
            null,
            resultado);
    }

    private String texto(byte[] pdf) throws Exception {
        PdfReader lector = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(lector);
            StringBuilder salida = new StringBuilder();
            for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
                salida.append(extractor.getTextFromPage(pagina)).append('\n');
            }
            return salida.toString();
        } finally {
            lector.close();
        }
    }
}
