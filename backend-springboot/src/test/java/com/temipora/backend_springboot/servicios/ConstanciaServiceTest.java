package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.temipora.backend_springboot.modelo.HitoPedidoDTO;
import com.temipora.backend_springboot.modelo.PagoDTO;
import com.temipora.backend_springboot.modelo.PedidoDTO;
import com.temipora.backend_springboot.modelo.UsuarioResumenDTO;

class ConstanciaServiceTest {

    private final ConstanciaService constanciaService = new ConstanciaService();

    @Test
    void generaUnaConstanciaConEncabezadoSeccionesYNotas() throws Exception {
        byte[] pdf = constanciaService.generar(pedidoConHitosYPagos());

        assertThat(texto(pdf))
            .contains("Temipora")
            .contains("Constancia del pedido")
            .contains("Pedido #12")
            .contains("Documento contractual del pedido")
            .contains("Importes")
            .contains("Plan de pagos")
            .contains("Entregas")
            .contains("Pagos")
            .contains("importes en ARS")
            .contains("Seña")
            .doesNotContain("Notas")
            .containsPattern("Página\\s+1\\s+de\\s*1");
    }

    @Test
    void guardaElTituloEnLosMetadatosDelPdf() throws Exception {
        PdfReader lector = new PdfReader(
            constanciaService.generar(pedidoConHitosYPagos()));

        try {
            assertThat(lector.getInfo().get("Title"))
                .isEqualTo("Constancia del pedido");
            assertThat(lector.getInfo().get("Subject"))
                .contains("Pedido #12");
        } finally {
            lector.close();
        }
    }

    @Test
    void muestraElNetoDelArtistaCuandoLaCopiaIncluyeComision()
            throws Exception {
        String texto = texto(constanciaService.generar(pedidoConHitosYPagos()));
        String neto = new java.text.DecimalFormat("#,##0.00")
            .format(new BigDecimal("95.00"));

        assertThat(texto)
            .contains("Precio del pedido")
            .contains("Comisión Temipora (5%)")
            .contains("Tu neto")
            .contains(neto);
    }

    @Test
    void omiteLaComisionEnLaCopiaDelCliente() throws Exception {
        PedidoDTO pedido = PedidoDTO.builder()
            .idPedido(13L)
            .cliente(usuario(1L, "cliente"))
            .artista(usuario(2L, "artista"))
            .nombreServicio("Retrato")
            .estado("FINALIZADO")
            .precioFinalMostrado(new BigDecimal("200.00"))
            .precioFinalClienteMostrado(new BigDecimal("180.00"))
            .monedaMostrada("ARS")
            .hitos(List.of())
            .build();

        String texto = texto(constanciaService.generar(pedido));

        assertThat(texto)
            .contains("Total a pagar")
            .doesNotContain("Tu neto")
            .doesNotContain("Comisión Temipora");
        assertThat(texto)
            .contains("El pedido no tiene hitos de pago registrados.")
            .contains("Todavía no hay entregas registradas.")
            .contains("Todavía no hay pagos registrados.");
    }

    private PedidoDTO pedidoConHitosYPagos() {
        return PedidoDTO.builder()
            .idPedido(12L)
            .cliente(usuario(1L, "cliente"))
            .artista(usuario(2L, "artista"))
            .nombreServicio("Ilustración")
            .estado("EN_CURSO")
            .precioFinalMostrado(new BigDecimal("100.00"))
            .precioFinalClienteMostrado(new BigDecimal("100.00"))
            .monedaMostrada("ARS")
            .comisionPorcentaje(new BigDecimal("5"))
            .comisionMonto(new BigDecimal("5.00"))
            .comisionMontoMostrado(new BigDecimal("5.00"))
            .montoNetoArtistaMostrado(new BigDecimal("95.00"))
            .terminosPlataformaVersion(3)
            .hitos(List.of(HitoPedidoDTO.builder()
                .idHitoPedido(1L)
                .orden(0)
                .nombre("Seña")
                .descripcion("Bocetos de los 2 dibujos")
                .tipo("SENA")
                .montoMostrado(new BigDecimal("50.00"))
                .estadoPago("PAGADO")
                .estadoEntrega("APROBADO")
                .pagos(List.of(PagoDTO.builder()
                    .idPago(1L)
                    .estado("APROBADO")
                    .monto(new BigDecimal("50.00"))
                    .moneda("ARS")
                    .fechaCreacion(LocalDateTime.now())
                    .build()))
                .build()))
            .build();
    }

    private UsuarioResumenDTO usuario(Long id, String nombre) {
        return UsuarioResumenDTO.builder()
            .idUsuario(id)
            .nombreUsuario(nombre)
            .nombreDisplay(nombre)
            .build();
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
