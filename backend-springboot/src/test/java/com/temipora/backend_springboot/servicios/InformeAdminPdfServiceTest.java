package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.temipora.backend_springboot.modelo.InformeAdminComparacionDTO;
import com.temipora.backend_springboot.modelo.InformeAdminDTO;
import com.temipora.backend_springboot.modelo.InformeAdminItemDTO;
import com.temipora.backend_springboot.modelo.InformeAdminSerieDTO;
import com.temipora.backend_springboot.modelo.InformeAdminTipoDTO;
import com.temipora.backend_springboot.modelo.InformeAdminTotalesDTO;
import com.temipora.backend_springboot.modelo.InformeAdminVendedorDTO;
import com.temipora.backend_springboot.modelo.InformePeriodoDTO;
import com.temipora.backend_springboot.servicios.informes.EmisorPdf;

class InformeAdminPdfServiceTest {

    private final InformeAdminPdfService servicio = new InformeAdminPdfService();

    @Test
    void generaUnPdfVerticalConEncabezadoYSecciones() throws Exception {
        byte[] pdf = servicio.generar(informe(), emisor());
        PdfReader lector = new PdfReader(pdf);
        int paginas = lector.getNumberOfPages();
        Rectangle pagina = lector.getPageSizeWithRotation(1);
        lector.close();

        assertThat(texto(pdf))
            .contains("Temipora")
            .contains("Informe de ventas y comisiones")
            .contains("Ventas y comisiones")
            .contains("ADM-20261101-20261130")
            .contains("Volumen procesado")
            .contains("Ingresó por ventas")
            .contains("La plataforma cobró")
            .contains("Neto para los artistas")
            .contains("Costo de MercadoPago")
            .contains("Pagado a los artistas")
            .contains("Ventas por tipo de ítem")
            .contains("Ventas por vendedor y artista")
            .contains("Ventas por ítem del catálogo")
            .contains("Cifras en ARS")
            .contains("Total")
            .contains("@admin")
            .contains("Administración")
            .doesNotContain("Notas")
            .containsPattern("Página\\s+1\\s+de\\s*" + paginas);

        assertThat(paginas).isEqualTo(1);
        assertThat(pagina.getWidth()).isLessThan(pagina.getHeight());
    }

    @Test
    void guardaElTituloEnLosMetadatosDelPdf() throws Exception {
        PdfReader lector = new PdfReader(servicio.generar(informe(), emisor()));

        try {
            assertThat(lector.getInfo().get("Title"))
                .isEqualTo("Informe de ventas y comisiones");
        } finally {
            lector.close();
        }
    }

    @Test
    void declaraElFiltroAplicadoAlInforme() throws Exception {
        InformeAdminDTO base = informe();
        InformeAdminDTO soloProductos = new InformeAdminDTO(
            base.periodo(), base.moneda(), "PRODUCTOS", base.generadoEn(),
            base.totales(), base.comparacion(),
            base.operacionesSinNetoReal(), base.serie(), base.tipos(),
            base.vendedores(), base.items());

        assertThat(texto(servicio.generar(soloProductos, emisor())))
            .contains("Solo productos");
    }

    @Test
    void generaUnPdfSinVentas() throws Exception {
        InformeAdminDTO informe = new InformeAdminDTO(
            new InformePeriodoDTO(LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), 30, "DIA"),
            "ARS",
            "TODOS",
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

        assertThat(texto(servicio.generar(informe, emisor())))
            .contains("No hay ventas registradas en el período.")
            .contains("No hay ítems vendidos en el período.")
            .contains("No hay un período anterior registrado para comparar.");
    }

    private EmisorPdf emisor() {
        return EmisorPdf.de("admin", "Ana Admin", "ADMIN");
    }

    private InformeAdminDTO informe() {
        return new InformeAdminDTO(
            new InformePeriodoDTO(LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), 30, "DIA"),
            "ARS",
            "TODOS",
            LocalDateTime.of(2026, 11, 18, 17, 40),
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
