package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.temipora.backend_springboot.modelo.HistorialClienteDTO;
import com.temipora.backend_springboot.modelo.HistorialItemDTO;
import com.temipora.backend_springboot.modelo.HistorialResumenDTO;

class InformeHistorialPdfServiceTest {

    private final InformeHistorialPdfService servicio =
        new InformeHistorialPdfService();

    @Test
    void generaLaConstanciaConResumenYOperaciones() throws Exception {
        String texto = texto(servicio.generar(informe(items()), "cliente1"));

        assertThat(texto)
            .contains("Historial de pedidos y compras")
            .contains("cliente1")
            .contains("HIST-20261101-20261121")
            .contains("Operaciones")
            .contains("Servicio")
            .contains("Producto")
            .contains("Ilustración")
            .contains("Pieza")
            .contains("Total");
    }

    @Test
    void avisaCuandoNoHayOperaciones() throws Exception {
        String texto = texto(servicio.generar(informe(List.of()), "cliente1"));

        assertThat(texto)
            .contains("No hay operaciones para los filtros elegidos.");
    }

    private HistorialClienteDTO informe(List<HistorialItemDTO> items) {
        return new HistorialClienteDTO(
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 21),
            "01/11/2026 al 21/11/2026",
            "Servicios y productos · Completados",
            LocalDateTime.of(2026, 11, 21, 10, 0),
            new HistorialResumenDTO(
                items.size(), 1, 1, 2, 0, 0, 0,
                new BigDecimal("75000.00"),
                new BigDecimal("75000.00"),
                new BigDecimal("5000.00"),
                "ARS"),
            items,
            0,
            20,
            items.size(),
            items.isEmpty() ? 0 : 1,
            false);
    }

    private List<HistorialItemDTO> items() {
        return List.of(
            new HistorialItemDTO(
                "PEDIDO", "Servicio", 11L,
                LocalDateTime.of(2026, 11, 10, 12, 0),
                "Ilustración de personaje", "artista1",
                new BigDecimal("50000.00"), "ARS",
                "FINALIZADO", "Finalizado", "FINALIZADO",
                new BigDecimal("10.00"), new BigDecimal("5000.00"),
                1, 1),
            new HistorialItemDTO(
                "COMPRA", "Producto", 8L,
                LocalDateTime.of(2026, 11, 20, 12, 0),
                "Pieza de cerámica", "artista2",
                new BigDecimal("25000.00"), "ARS",
                "PAGA", "Pagada", "FINALIZADO",
                BigDecimal.ZERO, BigDecimal.ZERO,
                2, 1));
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
