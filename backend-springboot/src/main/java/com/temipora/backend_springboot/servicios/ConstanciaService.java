package com.temipora.backend_springboot.servicios;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.temipora.backend_springboot.modelo.EntregaArchivoDTO;
import com.temipora.backend_springboot.modelo.EntregaHitoDTO;
import com.temipora.backend_springboot.modelo.HitoPedidoDTO;
import com.temipora.backend_springboot.modelo.PagoDTO;
import com.temipora.backend_springboot.modelo.PedidoDTO;
import com.temipora.backend_springboot.modelo.UsuarioResumenDTO;

@Service
public class ConstanciaService {

    private static final Font TITULO = new Font(
        Font.HELVETICA, 16, Font.BOLD);
    private static final Font SUBTITULO = new Font(
        Font.HELVETICA, 11, Font.BOLD);
    private static final Font TEXTO = new Font(
        Font.HELVETICA, 9, Font.NORMAL);
    private static final Font TEXTO_BOLD = new Font(
        Font.HELVETICA, 9, Font.BOLD);
    private static final Font CHICO = new Font(
        Font.HELVETICA, 8, Font.NORMAL);
    private static final DateTimeFormatter FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generar(PedidoDTO pedido) {
        Document documento = new Document(
            PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            documento.add(new Paragraph(
                "Constancia del pedido #" + pedido.getIdPedido(), TITULO));
            documento.add(new Paragraph(
                "Generada el " + LocalDateTime.now().format(FECHA_HORA)
                    + ". Refleja el estado del pedido al momento de generarse.",
                CHICO));
            documento.add(new Paragraph(" "));

            datosGenerales(documento, pedido);
            planDePagos(documento, pedido);
            entregas(documento, pedido);
            pagos(documento, pedido);

            documento.add(new Paragraph(" "));
            documento.add(new Paragraph(
                "Los hashes SHA-256 permiten verificar la integridad de los "
                    + "archivos entregados. La comisión y el neto del artista "
                    + "solo se incluyen en la copia del artista y del admin.",
                CHICO));

            documento.close();
        } catch (DocumentException e) {
            throw new IllegalStateException(
                "No se pudo generar la constancia.", e);
        }

        return salida.toByteArray();
    }

    private void datosGenerales(Document documento, PedidoDTO pedido)
            throws DocumentException {
        documento.add(new Paragraph("Datos generales", SUBTITULO));
        documento.add(new Paragraph(
            "Cliente: @" + usuario(pedido.getCliente())
                + "   Artista: @" + usuario(pedido.getArtista()), TEXTO));
        documento.add(new Paragraph(
            "Servicio: " + texto(pedido.getNombreServicio())
                + "   Estado: " + texto(pedido.getEstado()), TEXTO));
        documento.add(new Paragraph(
            "Inicio: " + fecha(pedido.getFechaInicio())
                + "   Límite de entrega: " + fecha(pedido.getLimiteEntrega())
                + "   Finalizado: " + fechaHora(pedido.getFinalizadoEn()),
            TEXTO));
        documento.add(new Paragraph(
            "Total: " + monto(
                pedido.getPrecioFinalMostrado(),
                pedido.getMonedaMostrada()), TEXTO_BOLD));

        if (pedido.getComisionMonto() != null) {
            documento.add(new Paragraph(
                "Comisión de la plataforma ("
                    + numero(pedido.getComisionPorcentaje()) + "%): "
                    + monto(pedido.getComisionMontoMostrado(),
                        pedido.getMonedaMostrada())
                    + "   Neto del artista: "
                    + monto(pedido.getMontoNetoArtistaMostrado(),
                        pedido.getMonedaMostrada()),
                TEXTO));
        }

        documento.add(new Paragraph(
            "Términos de la plataforma: versión "
                + numero(pedido.getTerminosPlataformaVersion()),
            CHICO));
        documento.add(new Paragraph(" "));
    }

    private void planDePagos(Document documento, PedidoDTO pedido)
            throws DocumentException {
        documento.add(new Paragraph("Plan de pagos", SUBTITULO));

        PdfPTable tabla = new PdfPTable(
            new float[] { 0.5f, 1.6f, 3.0f, 1.0f, 1.4f, 1.2f, 1.4f });
        tabla.setWidthPercentage(100);
        encabezado(tabla, "#");
        encabezado(tabla, "Hito");
        encabezado(tabla, "Alcance");
        encabezado(tabla, "Tipo");
        encabezado(tabla, "Monto");
        encabezado(tabla, "Pago");
        encabezado(tabla, "Entrega");

        List<HitoPedidoDTO> hitos = pedido.getHitos() != null
            ? pedido.getHitos() : List.of();
        for (HitoPedidoDTO hito : hitos) {
            celda(tabla, String.valueOf(
                (hito.getOrden() == null ? 0 : hito.getOrden()) + 1));
            celda(tabla, texto(hito.getNombre()));
            celda(tabla, texto(hito.getDescripcion()));
            celda(tabla, texto(hito.getTipo()));
            celda(tabla, monto(
                hito.getMontoMostrado(), pedido.getMonedaMostrada()));
            celda(tabla, texto(hito.getEstadoPago()));
            celda(tabla, texto(hito.getEstadoEntrega()));
        }

        documento.add(tabla);
        documento.add(new Paragraph(" "));
    }

    private void entregas(Document documento, PedidoDTO pedido)
            throws DocumentException {
        documento.add(new Paragraph("Entregas", SUBTITULO));

        List<HitoPedidoDTO> hitos = pedido.getHitos() != null
            ? pedido.getHitos() : List.of();
        boolean hayEntregas = false;

        for (HitoPedidoDTO hito : hitos) {
            if (hito.getEntregas() == null || hito.getEntregas().isEmpty()) {
                continue;
            }
            hayEntregas = true;
            documento.add(new Paragraph(
                "Hito: " + texto(hito.getNombre()), TEXTO_BOLD));

            for (EntregaHitoDTO entrega : hito.getEntregas()) {
                String estado = entrega.isVigente()
                    ? "Vigente" : "Reemplazada";
                if (entrega.isAprobada()) {
                    estado += " · Aprobada";
                }
                documento.add(new Paragraph(
                    "Entrega v" + entrega.getNumero()
                        + " — " + fechaHora(entrega.getFechaEntrega())
                        + " — @" + usuario(entrega.getAutor())
                        + " — " + estado,
                    TEXTO));

                if (entrega.getDescripcion() != null) {
                    documento.add(new Paragraph(
                        "  " + entrega.getDescripcion(), TEXTO));
                }
                if (entrega.getRevisionMotivo() != null) {
                    documento.add(new Paragraph(
                        "  Revisión solicitada: "
                            + entrega.getRevisionMotivo()
                            + " (" + fechaHora(entrega.getRevisionFecha())
                            + ")", TEXTO));
                }

                for (EntregaArchivoDTO archivo : entrega.getArchivos()) {
                    String dibujo = archivo.getDibujo() != null
                        ? "Dibujo "
                            + ((archivo.getDibujo().getOrden() == null
                                ? 0 : archivo.getDibujo().getOrden()) + 1)
                            + " — "
                        : "";
                    documento.add(new Paragraph(
                        "  · " + dibujo + texto(archivo.getNombre())
                            + " | sha256=" + texto(archivo.getSha256()),
                        CHICO));
                }
            }
        }

        if (!hayEntregas) {
            documento.add(new Paragraph(
                "Todavía no hay entregas registradas.", TEXTO));
        }
        documento.add(new Paragraph(" "));
    }

    private void pagos(Document documento, PedidoDTO pedido)
            throws DocumentException {
        documento.add(new Paragraph("Pagos", SUBTITULO));

        List<HitoPedidoDTO> hitos = pedido.getHitos() != null
            ? pedido.getHitos() : List.of();
        boolean hayPagos = false;

        for (HitoPedidoDTO hito : hitos) {
            if (hito.getPagos() == null) {
                continue;
            }
            for (PagoDTO pago : hito.getPagos()) {
                hayPagos = true;
                documento.add(new Paragraph(
                    "Hito " + texto(hito.getNombre())
                        + " — Pago #" + pago.getIdPago()
                        + " — " + texto(pago.getEstado())
                        + " — " + monto(pago.getMonto(), pago.getMoneda())
                        + " — " + fechaHora(
                            pago.getFechaAcreditacion() != null
                                ? pago.getFechaAcreditacion()
                                : pago.getFechaCreacion()),
                    TEXTO));
            }
        }

        if (!hayPagos) {
            documento.add(new Paragraph(
                "Todavía no hay pagos registrados.", TEXTO));
        }
    }

    private void encabezado(PdfPTable tabla, String valor) {
        PdfPCell celda = new PdfPCell(new Phrase(valor, TEXTO_BOLD));
        celda.setBackgroundColor(new Color(238, 238, 238));
        celda.setPadding(4);
        tabla.addCell(celda);
    }

    private void celda(PdfPTable tabla, String valor) {
        PdfPCell celda = new PdfPCell(new Phrase(valor, CHICO));
        celda.setPadding(3);
        tabla.addCell(celda);
    }

    private String usuario(UsuarioResumenDTO usuario) {
        return usuario == null || usuario.getNombreUsuario() == null
            ? "—" : usuario.getNombreUsuario();
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    private String numero(Number valor) {
        if (valor == null) {
            return "—";
        }
        return new DecimalFormat("#,##0.##").format(valor);
    }

    private String monto(BigDecimal valor, String moneda) {
        if (valor == null) {
            return "—";
        }
        String formateado = new DecimalFormat("#,##0.00").format(valor);
        return moneda == null || moneda.isBlank()
            ? formateado
            : formateado + " " + moneda;
    }

    private String fecha(LocalDate valor) {
        return valor == null ? "—" : valor.format(FECHA);
    }

    private String fechaHora(LocalDateTime valor) {
        return valor == null ? "—" : valor.format(FECHA_HORA);
    }
}
