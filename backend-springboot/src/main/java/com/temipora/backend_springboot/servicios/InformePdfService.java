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
import com.temipora.backend_springboot.modelo.InformeItemDTO;
import com.temipora.backend_springboot.modelo.InformeSerieDTO;
import com.temipora.backend_springboot.modelo.InformeTotalesDTO;
import com.temipora.backend_springboot.modelo.InformeVentasDTO;

@Service
public class InformePdfService {

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

    public byte[] generar(InformeVentasDTO informe, String nombreArtista) {
        Document documento = new Document(
            PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            documento.add(new Paragraph("Informe de ventas", TITULO));
            documento.add(new Paragraph(
                "Artista: @" + texto(nombreArtista), TEXTO));
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
            detalle(documento, informe);

            documento.add(new Paragraph(" "));
            documento.add(new Paragraph(
                "El neto percibido es el importe que el medio de pago liquidó "
                    + "realmente. Cuando una operación no tiene ese dato, se "
                    + "muestra el neto del artista como estimación.",
                CHICO));

            documento.close();
        } catch (DocumentException e) {
            throw new IllegalStateException(
                "No se pudo generar el informe de ventas.", e);
        }

        return salida.toByteArray();
    }

    private void resumen(Document documento, InformeVentasDTO informe)
            throws DocumentException {
        InformeTotalesDTO totales = informe.totales();
        documento.add(new Paragraph("Resumen del período", SUBTITULO));

        PdfPTable tabla = new PdfPTable(new float[] { 2.4f, 1.6f });
        tabla.setWidthPercentage(70);
        tabla.setHorizontalAlignment(PdfPTable.ALIGN_LEFT);
        filaResumen(tabla, "Ventas brutas", monto(totales.bruto(),
            informe.moneda()));
        filaResumen(tabla, "Neto percibido", monto(
            totales.netoPercibido(), informe.moneda()));
        filaResumen(tabla, "Costo de cobro", monto(
            totales.costoCobro(), informe.moneda()));
        filaResumen(tabla, "Comisión Temipora", monto(
            totales.comision(), informe.moneda()));
        filaResumen(tabla, "Arancel MercadoPago", monto(
            totales.arancel(), informe.moneda()));
        filaResumen(tabla, "Operaciones", String.valueOf(
            totales.operaciones()));
        filaResumen(tabla, "Unidades", String.valueOf(totales.unidades()));
        filaResumen(tabla, "Ticket promedio", monto(
            totales.ticketPromedio(), informe.moneda()));
        documento.add(tabla);
        documento.add(new Paragraph(" "));

        /*if (informe.operacionesSinNetoReal() > 0) {
            documento.add(new Paragraph(
                informe.operacionesSinNetoReal() + " de "
                    + totales.operaciones()
                    + " operaciones no tienen importe liquidado por el medio "
                    + "de pago; en esos casos el neto es estimado.",
                CHICO));
            documento.add(new Paragraph(" "));
        }*/
    }

    private void comparacion(Document documento, InformeVentasDTO informe)
            throws DocumentException {
        var anterior = informe.comparacion();
        if (anterior == null) {
            return;
        }
        documento.add(new Paragraph("Comparación", SUBTITULO));
        documento.add(new Paragraph(
            "Período anterior (" + fecha(anterior.desde())
                + " al " + fecha(anterior.hasta()) + "): neto "
                + monto(anterior.netoPercibido(), informe.moneda())
                + variacion(anterior.variacionNetoPorcentaje()),
            TEXTO));
        documento.add(new Paragraph(" "));
    }

    private void detalle(Document documento, InformeVentasDTO informe)
            throws DocumentException {
        documento.add(new Paragraph("Detalle por producto y servicio", SUBTITULO));

        List<InformeItemDTO> items = informe.items() != null
            ? informe.items() : List.of();
        if (items.isEmpty()) {
            documento.add(new Paragraph(
                "No hay ventas registradas en el período.", TEXTO));
            return;
        }

        PdfPTable tabla = new PdfPTable(
            new float[] { 0.9f, 2.6f, 0.8f, 0.9f, 1.2f, 1.2f, 1.2f, 1.3f, 0.9f });
        tabla.setWidthPercentage(100);
        encabezado(tabla, "Tipo");
        encabezado(tabla, "Ítem");
        encabezado(tabla, "Unid.");
        encabezado(tabla, "Oper.");
        encabezado(tabla, "Bruto");
        encabezado(tabla, "Comisión");
        encabezado(tabla, "Arancel");
        encabezado(tabla, "Neto");
        encabezado(tabla, "Part.");

        for (InformeItemDTO item : items) {
            celda(tabla, "PRODUCTO".equals(item.tipo())
                ? "Producto" : "Servicio");
            celda(tabla, texto(item.nombre()));
            celda(tabla, String.valueOf(item.unidades()));
            celda(tabla, String.valueOf(item.operaciones()));
            celda(tabla, monto(item.bruto(), informe.moneda()));
            celda(tabla, monto(item.comision(), informe.moneda()));
            celda(tabla, monto(item.arancel(), informe.moneda()));
            celda(tabla, monto(item.netoPercibido(), informe.moneda()));
            celda(tabla, numero(item.participacion()) + "%");
        }
        documento.add(tabla);

        List<InformeSerieDTO> serie = informe.serie() != null
            ? informe.serie() : List.of();
        long cobros = serie.stream().mapToLong(InformeSerieDTO::cobros).sum();
        documento.add(new Paragraph(" "));
        documento.add(new Paragraph(
            "Cobros registrados en la serie: " + cobros + ".", CHICO));
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
