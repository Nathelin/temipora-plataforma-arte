package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.temipora.backend_springboot.modelo.InformeComparacionDTO;
import com.temipora.backend_springboot.modelo.InformeItemDTO;
import com.temipora.backend_springboot.modelo.InformePeriodoDTO;
import com.temipora.backend_springboot.modelo.InformeSerieDTO;
import com.temipora.backend_springboot.modelo.InformeTotalesDTO;
import com.temipora.backend_springboot.modelo.InformeVentasDTO;

class InformePdfServiceTest {

    private final InformePdfService servicio = new InformePdfService();

    @Test
    void generaUnPdfConEncabezadoPieYSecciones() throws Exception {
        byte[] pdf = servicio.generar(informeConItems(), "artista");

        assertThat(texto(pdf))
            .contains("Temipora")
            .contains("Informe de ventas")
            .contains("Ventas del período")
            .contains("Referencia")
            .contains("VEN-20261101-20261130")
            .contains("Productos y servicios")
            .contains("Resumen")
            .contains("Comparación")
            .contains("Detalle por ítem")
            .contains("Notas")
            .contains("Neto percibido")
            .contains("01/11/2026")
            .containsPattern("Página\\s+1\\s+de\\s*1");
    }

    @Test
    void guardaElTituloEnLosMetadatosDelPdf() throws Exception {
        byte[] pdf = servicio.generar(informeConItems(), "artista");
        PdfReader lector = new PdfReader(pdf);

        try {
            assertThat(lector.getInfo().get("Title"))
                .isEqualTo("Informe de ventas");
            assertThat(lector.getInfo().get("Author")).isEqualTo("Temipora");
        } finally {
            lector.close();
        }
    }

    @Test
    void repiteElEncabezadoDeLaTablaYNumeraTodasLasPaginas()
            throws Exception {
        byte[] pdf = servicio.generar(informeConMuchosItems(), "artista");
        PdfReader lector = new PdfReader(pdf);
        int paginas = lector.getNumberOfPages();
        lector.close();

        assertThat(paginas).isGreaterThanOrEqualTo(2);
        assertThat(textoDePagina(pdf, 2))
            .contains("Ítem")
            .contains("Comisión");
        assertThat(texto(pdf))
            .containsPattern("Página\\s+1\\s+de\\s*" + paginas);
        assertThat(textoDePagina(pdf, paginas))
            .containsPattern("Página\\s+" + paginas + "\\s+de\\s*" + paginas);
    }

    @Test
    void declaraElFiltroAplicadoAlInforme() throws Exception {
        InformeVentasDTO base = informeConItems();
        InformeVentasDTO soloServicios = new InformeVentasDTO(
            base.periodo(), base.moneda(), "SERVICIOS", base.generadoEn(),
            base.totales(), base.comparacion(),
            base.operacionesSinNetoReal(), base.serie(), base.items());

        assertThat(texto(servicio.generar(soloServicios, "artista")))
            .contains("Solo servicios");
    }

    @Test
    void generaUnPdfSinVentas() throws Exception {
        InformeVentasDTO informe = new InformeVentasDTO(
            new InformePeriodoDTO(LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), 30, "DIA"),
            "ARS",
            "TODOS",
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

        assertThat(texto(servicio.generar(informe, "artista")))
            .contains("No hay ventas registradas en el período.")
            .contains("Sin ventas en el período anterior");
    }

    private InformeVentasDTO informeConItems() {
        return new InformeVentasDTO(
            new InformePeriodoDTO(LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), 30, "MES"),
            "ARS",
            "TODOS",
            LocalDateTime.of(2026, 11, 18, 17, 40),
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

    private InformeVentasDTO informeConMuchosItems() {
        List<InformeItemDTO> items = new ArrayList<>();
        for (int indice = 0; indice < 80; indice++) {
            items.add(new InformeItemDTO("PRODUCTO", (long) indice,
                "Obra " + indice, 1, 1,
                new BigDecimal("200.00"), new BigDecimal("20.00"),
                new BigDecimal("9.00"), new BigDecimal("171.00"),
                new BigDecimal("50.00")));
        }
        return new InformeVentasDTO(
            new InformePeriodoDTO(LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), 30, "MES"),
            "ARS",
            "TODOS",
            LocalDateTime.of(2026, 11, 18, 17, 40),
            new InformeTotalesDTO(
                new BigDecimal("16000.00"), new BigDecimal("13680.00"),
                new BigDecimal("2320.00"), new BigDecimal("1600.00"),
                new BigDecimal("720.00"), 80, 80,
                new BigDecimal("171.00")),
            null,
            0,
            List.of(),
            items);
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

    private String textoDePagina(byte[] pdf, int pagina) throws Exception {
        PdfReader lector = new PdfReader(pdf);
        try {
            return new PdfTextExtractor(lector).getTextFromPage(pagina);
        } finally {
            lector.close();
        }
    }
}
