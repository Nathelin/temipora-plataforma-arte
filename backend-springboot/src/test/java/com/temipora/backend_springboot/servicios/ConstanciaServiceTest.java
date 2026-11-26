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
import com.temipora.backend_springboot.modelo.ProrrogaDTO;
import com.temipora.backend_springboot.modelo.UsuarioResumenDTO;
import com.temipora.backend_springboot.servicios.informes.EmisorPdf;

class ConstanciaServiceTest {

    private final ConstanciaService constanciaService =
        new ConstanciaService("https://temipora.test");

    @Test
    void generaUnaConstanciaConEncabezadoSeccionesYNotas() throws Exception {
        byte[] pdf = constanciaService.generar(
            pedidoConHitosYPagos(), emisor());

        assertThat(texto(pdf))
            .contains("Temipora")
            .contains("@emisor")
            .contains("Emisor Prueba")
            .contains("Constancia del pedido")
            .contains("Pedido #12")
            .contains("Documento contractual del pedido")
            .contains("Importes")
            .contains("Plan de pagos")
            .contains("Prórrogas del plazo")
            .contains("Entregas")
            .contains("Pagos")
            .contains("Cifras en ARS")
            .contains("Seña")
            .contains("El pedido no tiene prórrogas del plazo registradas.")
            .doesNotContain("Notas")
            .containsPattern("Página\\s+1\\s+de\\s*\\d+");
    }

    @Test
    void muestraLasProrrogasDelPlazoAceptadas() throws Exception {
        PedidoDTO pedido = PedidoDTO.builder()
            .idPedido(15L)
            .cliente(usuario(1L, "cliente"))
            .artista(usuario(2L, "artista"))
            .nombreServicio("Ilustración")
            .estado("EN_CURSO")
            .limiteEntrega(java.time.LocalDate.of(2026, 12, 12))
            .fechaEntregaPrevista(java.time.LocalDate.of(2026, 12, 19))
            .precioFinalMostrado(new BigDecimal("100.00"))
            .monedaMostrada("ARS")
            .hitos(List.of())
            .prorrogas(List.of(ProrrogaDTO.builder()
                .idProrroga(9L)
                .diasSolicitados(7)
                .motivo("Se retrasó la impresión de la pieza")
                .estado("ACEPTADA")
                .fechaSolicitud(LocalDateTime.of(2026, 12, 5, 9, 30))
                .fechaRespuesta(LocalDateTime.of(2026, 12, 5, 14, 0))
                .entregaPrevistaAntes(java.time.LocalDate.of(2026, 12, 12))
                .entregaPrevistaDespues(java.time.LocalDate.of(2026, 12, 19))
                .build()))
            .build();

        String texto = texto(constanciaService.generar(pedido, emisor()));

        assertThat(texto)
            .contains("ENTREGA PROMETIDA")
            .contains("ENTREGA VIGENTE")
            .contains("Prórrogas del plazo")
            .contains("impresión")
            .contains("Aceptada")
            .contains("Respuesta");
    }

    @Test
    void guardaElTituloEnLosMetadatosDelPdf() throws Exception {
        PdfReader lector = new PdfReader(
            constanciaService.generar(pedidoConHitosYPagos(), emisor()));

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
        String texto = texto(constanciaService.generar(pedidoConHitosYPagos(), emisor()));
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

        String texto = texto(constanciaService.generar(pedido, emisor()));

        assertThat(texto)
            .contains("Total a pagar")
            .doesNotContain("Tu neto")
            .doesNotContain("Comisión Temipora");
        assertThat(texto)
            .contains("El pedido no tiene hitos de pago registrados.")
            .contains("Todavía no hay entregas registradas.")
            .contains("Todavía no hay pagos registrados.");
    }

    @Test
    void muestraLosEstadosLegiblesSinCortarPalabras() throws Exception {
        PedidoDTO pedido = PedidoDTO.builder()
            .idPedido(14L)
            .cliente(usuario(1L, "cliente"))
            .artista(usuario(2L, "artista"))
            .nombreServicio("Retrato")
            .estado("PENDIENTE_PAGO")
            .precioFinalMostrado(new BigDecimal("1000.00"))
            .monedaMostrada("ARS")
            .hitos(List.of(
                HitoPedidoDTO.builder()
                    .idHitoPedido(1L)
                    .orden(0)
                    .nombre("Seña")
                    .tipo("SENA")
                    .montoMostrado(new BigDecimal("500.00"))
                    .estadoPago("PENDIENTE")
                    .estadoEntrega("NO_INICIADO")
                    .pagos(List.of())
                    .build(),
                HitoPedidoDTO.builder()
                    .idHitoPedido(2L)
                    .orden(1)
                    .nombre("Entrega final")
                    .tipo("CUOTA")
                    .montoMostrado(new BigDecimal("500.00"))
                    .estadoPago("REEMBOLSADO")
                    .estadoEntrega("APROBADO_AUTOMATICAMENTE")
                    .pagos(List.of())
                    .build()))
            .build();

        String texto = texto(constanciaService.generar(pedido, emisor()));

        assertThat(texto)
            .contains("No iniciado")
            .contains("Pendiente")
            .contains("Reembolsado")
            .contains("automáticamente")
            .contains("Cuota")
            .contains("Pendiente de pago");
    }

    @Test
    void muestraLosTerminosAceptadosConVersionYHuella() throws Exception {
        String texto = texto(constanciaService.generar(pedidoConHitosYPagos(), emisor()));

        assertThat(texto)
            .contains("Términos aceptados")
            .contains("Plataforma")
            .contains("v3")
            .contains("Del artista")
            .contains("v7")
            .contains("Generales del artista")
            .contains("faa13a754b615f98")
            .contains("b00994ee58432ff1")
            .contains(
                "https://temipora.test/terminos-aceptados/"
                    + "TA-Y-b00994ee58432ff1");
    }

    private EmisorPdf emisor() {
        return EmisorPdf.de("emisor", "Emisor Prueba", "ADMIN");
    }

    private PedidoDTO pedidoConHitosYPagos() {
        return PedidoDTO.builder()
            .idPedido(12L)
            .idSolicitud(34L)
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
            .terminosPlataformaAceptadosEn(
                LocalDateTime.of(2026, 11, 19, 23, 35))
            .terminosPlataformaHuella(
                "faa13a754b615f989a9e6600f4642592344820379bf3cb8232b40bd3"
                    + "0d6e08b5")
            .terminosArtistaVersion(7)
            .terminosArtistaAceptadosEn(
                LocalDateTime.of(2026, 11, 12, 10, 2))
            .terminosArtistaDelServicio(false)
            .terminosArtistaHuella(
                "b00994ee58432ff1a76c14fc395e51da35e9a4ebd68e0803e25a8351"
                    + "b87d4320")
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
