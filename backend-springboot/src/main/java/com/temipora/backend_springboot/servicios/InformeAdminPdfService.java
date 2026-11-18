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
import com.temipora.backend_springboot.modelo.InformeAdminDTO;
import com.temipora.backend_springboot.modelo.InformeAdminItemDTO;
import com.temipora.backend_springboot.modelo.InformeAdminTipoDTO;
import com.temipora.backend_springboot.modelo.InformeAdminTotalesDTO;
import com.temipora.backend_springboot.modelo.InformeAdminVendedorDTO;

@Service
public class InformeAdminPdfService {

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

    public byte[] generar(InformeAdminDTO informe) {
        Document documento = new Document(
            PageSize.A4.rotate(), 36, 36, 36, 36);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            documento.add(new Paragraph(
                "Informe de ventas y comisiones", TITULO));
            documento.add(new Paragraph(
                "Período: " + fecha(informe.periodo().desde())
                    + " al " + fecha(informe.periodo().hasta())
                    + " (" + informe.periodo().dias() + " días)",
                TEXTO));
            documento.add(new Paragraph(
                "Generado el " + LocalDateTime.now().format(FECHA_HORA)
                    + ". Importes en " + informe.moneda() + ".",
                CHICO));
            documento.add(new Paragraph(" "));

            resumen(documento, informe);
            comparacion(documento, informe);
            porTipo(documento, informe);
            porVendedor(documento, informe);
            porItem(documento, informe);

            documento.add(new Paragraph(" "));
            documento.add(new Paragraph(
                "La comisión es el ingreso de Temipora por el uso de la "
                    + "plataforma. El arancel es el informado por el medio "
                    + "de pago.",
                CHICO));

            documento.close();
        } catch (DocumentException e) {
            throw new IllegalStateException(
                "No se pudo generar el informe de ventas y comisiones.", e);
        }

        return salida.toByteArray();
    }

    private void resumen(Document documento, InformeAdminDTO informe)
            throws DocumentException {
        InformeAdminTotalesDTO totales = informe.totales();
        documento.add(new Paragraph("Resumen del período", SUBTITULO));

        PdfPTable tabla = new PdfPTable(new float[] { 2.6f, 1.6f });
        tabla.setWidthPercentage(55);
        tabla.setHorizontalAlignment(PdfPTable.ALIGN_LEFT);
        filaResumen(tabla, "Volumen procesado",
            monto(totales.volumen(), informe.moneda()));
        filaResumen(tabla, "Ingresos de la plataforma (comisión)",
            monto(totales.comision(), informe.moneda()));
        filaResumen(tabla, "Neto a artistas",
            monto(totales.netoArtistas(), informe.moneda()));
        filaResumen(tabla, "Arancel MercadoPago",
            monto(totales.arancel(), informe.moneda()));
        filaResumen(tabla, "Neto liquidado a artistas",
            monto(totales.netoPercibido(), informe.moneda()));
        filaResumen(tabla, "Operaciones",
            String.valueOf(totales.operaciones()));
        filaResumen(tabla, "Unidades", String.valueOf(totales.unidades()));
        filaResumen(tabla, "Ticket promedio",
            monto(totales.ticketPromedio(), informe.moneda()));
        documento.add(tabla);
        documento.add(new Paragraph(" "));
    }

    private void comparacion(Document documento, InformeAdminDTO informe)
            throws DocumentException {
        var anterior = informe.comparacion();
        if (anterior == null) {
            return;
        }
        documento.add(new Paragraph("Comparación", SUBTITULO));
        documento.add(new Paragraph(
            "Período anterior (" + fecha(anterior.desde())
                + " al " + fecha(anterior.hasta()) + "): volumen "
                + monto(anterior.volumen(), informe.moneda())
                + variacion(anterior.variacionVolumenPorcentaje())
                + " · comisión "
                + monto(anterior.comision(), informe.moneda())
                + variacion(anterior.variacionComisionPorcentaje()),
            TEXTO));
        documento.add(new Paragraph(" "));
    }

    private void porTipo(Document documento, InformeAdminDTO informe)
            throws DocumentException {
        documento.add(new Paragraph("Por tipo", SUBTITULO));
        List<InformeAdminTipoDTO> tipos = informe.tipos() != null
            ? informe.tipos() : List.of();
        if (tipos.isEmpty()) {
            documento.add(new Paragraph(
                "No hay ventas registradas en el período.", TEXTO));
            documento.add(new Paragraph(" "));
            return;
        }

        PdfPTable tabla = new PdfPTable(
            new float[] { 1.2f, 0.8f, 0.8f, 1.4f, 1.4f, 1.4f, 1.4f });
        tabla.setWidthPercentage(100);
        encabezado(tabla, "Tipo");
        encabezado(tabla, "Oper.");
        encabezado(tabla, "Unid.");
        encabezado(tabla, "Volumen");
        encabezado(tabla, "Comisión");
        encabezado(tabla, "Neto artistas");
        encabezado(tabla, "Arancel");

        for (InformeAdminTipoDTO tipo : tipos) {
            celda(tabla, tipo(tipo.tipo()));
            celda(tabla, String.valueOf(tipo.operaciones()));
            celda(tabla, String.valueOf(tipo.unidades()));
            celda(tabla, monto(tipo.volumen(), informe.moneda()));
            celda(tabla, monto(tipo.comision(), informe.moneda()));
            celda(tabla, monto(tipo.netoArtistas(), informe.moneda()));
            celda(tabla, monto(tipo.arancel(), informe.moneda()));
        }
        documento.add(tabla);
        documento.add(new Paragraph(" "));
    }

    private void porVendedor(Document documento, InformeAdminDTO informe)
            throws DocumentException {
        documento.add(new Paragraph("Por vendedor y artista", SUBTITULO));
        List<InformeAdminVendedorDTO> vendedores = informe.vendedores() != null
            ? informe.vendedores() : List.of();
        if (vendedores.isEmpty()) {
            documento.add(new Paragraph(
                "No hay vendedores con ventas en el período.", TEXTO));
            documento.add(new Paragraph(" "));
            return;
        }

        PdfPTable tabla = new PdfPTable(
            new float[] { 2.4f, 0.8f, 0.8f, 1.4f, 1.4f, 1.4f, 1.4f });
        tabla.setWidthPercentage(100);
        encabezado(tabla, "Vendedor / artista");
        encabezado(tabla, "Oper.");
        encabezado(tabla, "Unid.");
        encabezado(tabla, "Volumen");
        encabezado(tabla, "Comisión");
        encabezado(tabla, "Neto artistas");
        encabezado(tabla, "Arancel");

        for (InformeAdminVendedorDTO vendedor : vendedores) {
            celda(tabla, texto(vendedor.nombre()));
            celda(tabla, String.valueOf(vendedor.operaciones()));
            celda(tabla, String.valueOf(vendedor.unidades()));
            celda(tabla, monto(vendedor.volumen(), informe.moneda()));
            celda(tabla, monto(vendedor.comision(), informe.moneda()));
            celda(tabla, monto(vendedor.netoArtistas(), informe.moneda()));
            celda(tabla, monto(vendedor.arancel(), informe.moneda()));
        }
        documento.add(tabla);
        documento.add(new Paragraph(" "));
    }

    private void porItem(Document documento, InformeAdminDTO informe)
            throws DocumentException {
        documento.add(new Paragraph("Por ítem del catálogo", SUBTITULO));
        List<InformeAdminItemDTO> items = informe.items() != null
            ? informe.items() : List.of();
        if (items.isEmpty()) {
            documento.add(new Paragraph(
                "No hay ítems vendidos en el período.", TEXTO));
            return;
        }

        PdfPTable tabla = new PdfPTable(
            new float[] { 0.9f, 2.8f, 0.7f, 0.7f, 1.3f, 1.3f, 1.3f, 1.2f });
        tabla.setWidthPercentage(100);
        encabezado(tabla, "Tipo");
        encabezado(tabla, "Ítem");
        encabezado(tabla, "Oper.");
        encabezado(tabla, "Unid.");
        encabezado(tabla, "Volumen");
        encabezado(tabla, "Comisión");
        encabezado(tabla, "Neto artistas");
        encabezado(tabla, "Arancel");

        for (InformeAdminItemDTO item : items) {
            celda(tabla, tipo(item.tipo()));
            celda(tabla, texto(item.nombre()));
            celda(tabla, String.valueOf(item.operaciones()));
            celda(tabla, String.valueOf(item.unidades()));
            celda(tabla, monto(item.volumen(), informe.moneda()));
            celda(tabla, monto(item.comision(), informe.moneda()));
            celda(tabla, monto(item.netoArtistas(), informe.moneda()));
            celda(tabla, monto(item.arancel(), informe.moneda()));
        }
        documento.add(tabla);
    }

    private void filaResumen(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, TEXTO));
        celdaEtiqueta.setBorderColor(new Color(214, 214, 214));
        celdaEtiqueta.setPadding(4);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor, TEXTO_BOLD));
        celdaValor.setBorderColor(new Color(214, 214, 214));
        celdaValor.setPadding(4);
        celdaValor.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
        tabla.addCell(celdaValor);
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

    private String variacion(BigDecimal valor) {
        if (valor == null) {
            return " (sin ventas en el período anterior)";
        }
        String signo = valor.signum() >= 0 ? "+" : "";
        return " (" + signo + numero(valor) + "% respecto del anterior)";
    }

    private String tipo(String valor) {
        return "PRODUCTO".equals(valor) ? "Producto" : "Servicio";
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
}
