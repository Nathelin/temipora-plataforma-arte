package com.temipora.backend_springboot.servicios.informes;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Locale;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Documento PDF de la plataforma: cabecera y pie en todas las páginas,
 * identificación del documento, métricas, secciones numeradas, tablas y
 * resumen de importes. Los servicios de informes sólo describen su contenido.
 */
public final class DocumentoPdf {

    private static final int COLUMNAS_METADATOS = 5;

    private final Document documento;
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
    private final String tipo;
    private final String referencia;
    private final String pie;
    private final String detallePie;
    private final EmisorPdf emisor;

    private DocumentoPdf(
            Rectangle pagina, String tipo, String referencia, String codigo,
            String pie, EmisorPdf emisor) {
        this.tipo = tipo;
        this.referencia = referencia;
        this.pie = pie;
        this.emisor = emisor;
        this.detallePie = "Ref. " + (codigo == null ? "—" : codigo)
            + "  ·  Zona horaria: Argentina (UTC-3)"
            + "  ·  Datos al "
            + FormatoPdf.fechaHora(LocalDateTime.now());
        this.documento = new Document(pagina,
            EstiloPdf.MARGEN_LATERAL, EstiloPdf.MARGEN_LATERAL,
            EstiloPdf.MARGEN_SUPERIOR, EstiloPdf.MARGEN_INFERIOR);
    }

    public static DocumentoPdf vertical(
            String tipo, String referencia, String pie) {
        return vertical(tipo, referencia, referencia, pie, null);
    }

    public static DocumentoPdf vertical(
            String tipo, String referencia, String pie, EmisorPdf emisor) {
        return vertical(tipo, referencia, referencia, pie, emisor);
    }

    public static DocumentoPdf vertical(
            String tipo, String referencia, String codigo, String pie) {
        return vertical(tipo, referencia, codigo, pie, null);
    }

    public static DocumentoPdf vertical(
            String tipo, String referencia, String codigo, String pie,
            EmisorPdf emisor) {
        return new DocumentoPdf(
            PageSize.A4, tipo, referencia, codigo, pie, emisor);
    }

    public void abrir() {
        try {
            PdfWriter escritor = PdfWriter.getInstance(documento, salida);
            escritor.setPageEvent(new EncabezadoPiePdf(
                tipo, referencia, pie, detallePie,
                emisor == null ? null : emisor.descripcion()));
            documento.addTitle(tipo);
            documento.addAuthor("Temipora");
            documento.addCreator("Temipora");
            documento.addSubject(referencia == null || referencia.isBlank()
                ? tipo : tipo + " · " + referencia);
            documento.open();
        } catch (DocumentException e) {
            throw new IllegalStateException(
                "No se pudo abrir el documento PDF.", e);
        }
    }

    public byte[] cerrar() {
        documento.close();
        return salida.toByteArray();
    }

    public DocumentoPdf cabecera(
            String eyebrow, String titulo, String bajada) {
        if (eyebrow != null && !eyebrow.isBlank()) {
            Paragraph sobre = new Paragraph(
                eyebrow.toUpperCase(new Locale("es")), EstiloPdf.EYEBROW);
            sobre.setSpacingAfter(8);
            agregar(sobre);
        }
        Paragraph parrafo = new Paragraph(titulo, EstiloPdf.TITULO);
        parrafo.setSpacingAfter(4);
        agregar(parrafo);
        if (bajada != null && !bajada.isBlank()) {
            Paragraph detalle = new Paragraph(bajada, EstiloPdf.SUBTITULO);
            detalle.setSpacingAfter(4);
            agregar(detalle);
        }
        return this;
    }

    public DocumentoPdf metadatos(String... pares) {
        PdfPTable tabla = new PdfPTable(COLUMNAS_METADATOS);
        tabla.setWidthPercentage(100);
        int celdas = 0;
        for (int indice = 0; indice < pares.length; indice += 2) {
            String valor = indice + 1 < pares.length ? pares[indice + 1] : "";
            tabla.addCell(celdaDato(pares[indice], valor));
            celdas++;
        }
        if (emisor != null) {
            tabla.addCell(celdaDato("Emitido por", emisor.descripcion()));
            celdas++;
        }
        while (celdas % COLUMNAS_METADATOS != 0) {
            tabla.addCell(celdaDato("", ""));
            celdas++;
        }
        tabla.setSpacingBefore(12);
        tabla.setSpacingAfter(8);
        agregar(tabla);
        return this;
    }

    public DocumentoPdf indicadores(Metrica... metricas) {
        PdfPTable tabla = new PdfPTable(metricas.length);
        tabla.setWidthPercentage(100);

        for (Metrica metrica : metricas) {
            Paragraph valor = new Paragraph(metrica.valor(),
                metrica.acento() ? EstiloPdf.METRICA_ACENTO
                    : EstiloPdf.METRICA);
            valor.setLeading(24);
            valor.setSpacingAfter(2);
            Paragraph etiqueta = new Paragraph(
                metrica.etiqueta(), EstiloPdf.METRICA_ETIQUETA);

            PdfPCell celda = new PdfPCell();
            celda.setBorder(Rectangle.NO_BORDER);
            celda.setPadding(0);
            celda.setPaddingRight(10);
            celda.addElement(valor);
            celda.addElement(etiqueta);
            if (metrica.detalle() != null && !metrica.detalle().isBlank()) {
                celda.addElement(new Paragraph(
                    metrica.detalle(), EstiloPdf.METRICA_DETALLE));
            }
            tabla.addCell(celda);
        }
        tabla.setSpacingBefore(12);
        tabla.setSpacingAfter(4);
        agregar(tabla);
        return this;
    }

    public record Metrica(
            String valor, String etiqueta, String detalle, boolean acento) {
    }

    public DocumentoPdf seccion(String numero, String titulo) {
        return seccion(numero, titulo, null);
    }

    public DocumentoPdf seccion(
            String numero, String titulo, String detalle) {
        Paragraph parrafo = new Paragraph();
        parrafo.setSpacingBefore(12);
        parrafo.setSpacingAfter(6);
        parrafo.add(new Chunk(numero + "   ", EstiloPdf.SECCION_NUMERO));
        parrafo.add(new Chunk(titulo, EstiloPdf.SECCION));
        if (detalle != null && !detalle.isBlank()) {
            parrafo.add(new Chunk("   " + detalle, EstiloPdf.SUBTITULO));
        }
        agregar(parrafo);
        return this;
    }

    public DocumentoPdf resumen(
            String[] conceptos, String[] valores, int destacada) {
        PdfPTable tabla = new PdfPTable(2);
        try {
            tabla.setWidths(new float[] { 4f, 1f });
        } catch (DocumentException e) {
            throw new IllegalStateException(
                "No se pudo definir el resumen.", e);
        }
        tabla.setWidthPercentage(100);

        for (int indice = 0; indice < conceptos.length; indice++) {
            boolean total = indice == destacada;
            tabla.addCell(celdaResumen(conceptos[indice], total, false));
            tabla.addCell(celdaResumen(valores[indice], total, true));
        }
        tabla.setSpacingBefore(2);
        tabla.setSpacingAfter(12);
        agregar(tabla);
        return this;
    }

    public DocumentoPdf tabla(TablaPdf tabla) {
        PdfPTable pdf = tabla.pdf();
        pdf.setSpacingBefore(2);
        pdf.setSpacingAfter(10);
        agregar(pdf);
        return this;
    }

    public DocumentoPdf parrafo(String texto) {
        Paragraph parrafo = new Paragraph(texto, EstiloPdf.VALOR);
        parrafo.setLeading(12);
        parrafo.setSpacingAfter(8);
        agregar(parrafo);
        return this;
    }

    public DocumentoPdf parrafoChico(String texto) {
        Paragraph parrafo = new Paragraph(texto, EstiloPdf.PIE);
        parrafo.setLeading(10);
        parrafo.setSpacingAfter(4);
        agregar(parrafo);
        return this;
    }

    public DocumentoPdf subtitulo(String texto) {
        Paragraph parrafo = new Paragraph(texto, EstiloPdf.VALOR_TOTAL);
        parrafo.setSpacingBefore(6);
        parrafo.setSpacingAfter(3);
        agregar(parrafo);
        return this;
    }

    private void agregar(Element elemento) {
        try {
            documento.add(elemento);
        } catch (DocumentException e) {
            throw new IllegalStateException(
                "No se pudo componer el documento PDF.", e);
        }
    }

    private static PdfPCell celdaDato(String etiqueta, String valor) {
        Paragraph titulo = new Paragraph(
            etiqueta.toUpperCase(new Locale("es")), EstiloPdf.ETIQUETA);
        titulo.setSpacingAfter(3);

        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(EstiloPdf.LINEA);
        celda.setBorderWidth(0.5f);
        celda.setPadding(5);
        celda.setPaddingLeft(0);
        celda.setPaddingRight(10);
        celda.addElement(titulo);
        celda.addElement(new Paragraph(valor, EstiloPdf.VALOR));
        return celda;
    }

    private static PdfPCell celdaResumen(
            String texto, boolean total, boolean derecha) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, total
            ? EstiloPdf.VALOR_TOTAL : EstiloPdf.VALOR));
        celda.setPadding(4.5f);
        celda.setPaddingLeft(0);
        celda.setPaddingRight(0);
        if (total) {
            celda.setBorder(Rectangle.TOP);
            celda.setBorderColor(EstiloPdf.ACENTO);
            celda.setBorderWidth(0.8f);
            celda.setPaddingTop(5);
        } else {
            celda.setBorder(Rectangle.BOTTOM);
            celda.setBorderColor(EstiloPdf.LINEA);
            celda.setBorderWidth(0.5f);
        }
        celda.setHorizontalAlignment(
            derecha ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        return celda;
    }
}
