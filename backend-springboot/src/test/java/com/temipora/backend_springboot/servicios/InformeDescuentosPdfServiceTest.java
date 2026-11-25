package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.temipora.backend_springboot.modelo.DescuentoPedidoDTO;
import com.temipora.backend_springboot.modelo.InformeDescuentosDTO;
import com.temipora.backend_springboot.modelo.OfertasReactivacionResumenDTO;
import com.temipora.backend_springboot.modelo.ResumenDescuentosDTO;

class InformeDescuentosPdfServiceTest {

    private final InformeDescuentosPdfService servicio =
        new InformeDescuentosPdfService();

    @Test
    void generaElInformeConPedidosYOfertas() throws Exception {
        String texto = texto(servicio.generar(
            informe(pedidos(), resumen(2), ofertas(12))));

        assertThat(texto)
            .contains("Descuentos aplicados en pedidos")
            .contains("PROM-DTO-20261101-20261121")
            .contains("Pedidos con descuento")
            .contains("Descuento financiado")
            .contains("Ofertas de reactivación generadas en el período")
            .contains("#101")
            .contains("cliente1")
            .contains("La comisión congelada al emitir el presupuesto fue 0%")
            .contains("Total");
    }

    @Test
    void informaCuandoNoHayPedidos() throws Exception {
        String texto = texto(servicio.generar(
            informe(List.of(), resumen(0), ofertas(0))));

        assertThat(texto)
            .contains("No hay pedidos con descuento para los filtros elegidos.")
            .contains("No se generaron ofertas de reactivación en el período.");
    }

    private InformeDescuentosDTO informe(
            List<DescuentoPedidoDTO> pedidos,
            ResumenDescuentosDTO resumen,
            OfertasReactivacionResumenDTO ofertas) {
        return new InformeDescuentosDTO(
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 21),
            21,
            "ARS",
            "Todos los pedidos con descuento",
            LocalDateTime.of(2026, 11, 21, 10, 0),
            resumen,
            ofertas,
            pedidos,
            0,
            20,
            pedidos.size(),
            pedidos.isEmpty() ? 0 : 1,
            false);
    }

    private ResumenDescuentosDTO resumen(int pedidos) {
        return new ResumenDescuentosDTO(
            pedidos,
            pedidos,
            pedidos,
            new BigDecimal("20.00"),
            new BigDecimal("3000.00"),
            new BigDecimal("1500.00"),
            new BigDecimal("27000.00"),
            new BigDecimal("30000.00"),
            new BigDecimal("2700.00"),
            new BigDecimal("5700.00"),
            new BigDecimal("13500.00"));
    }

    private OfertasReactivacionResumenDTO ofertas(int total) {
        return new OfertasReactivacionResumenDTO(
            3, total, 5, 4, 3, 2, 0, new BigDecimal("25.00"));
    }

    private List<DescuentoPedidoDTO> pedidos() {
        return List.of(
            pedido(101L, "cliente1", "10.00", false),
            pedido(102L, "cliente2", "0.00", true));
    }

    private DescuentoPedidoDTO pedido(
            Long idPedido, String cliente, String comision,
            boolean comisionCero) {
        return new DescuentoPedidoDTO(
            idPedido,
            LocalDateTime.of(2026, 11, 20, 12, 0),
            "EN_CURSO",
            1L,
            cliente,
            "Cliente de prueba",
            2L,
            "artista1",
            7L,
            "Ilustración",
            5L,
            LocalDateTime.of(2026, 11, 18, 9, 0),
            33L,
            new BigDecimal("10.00"),
            "Mismo estilo",
            "UTILIZADA",
            LocalDateTime.of(2026, 11, 18, 10, 0),
            LocalDateTime.of(2026, 11, 19, 9, 0),
            new BigDecimal("10.00"),
            new BigDecimal("3000.00"),
            new BigDecimal("30000.00"),
            new BigDecimal("27000.00"),
            new BigDecimal(comision),
            new BigDecimal("2700.00"),
            new BigDecimal("5700.00"),
            "ARS",
            comisionCero);
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
