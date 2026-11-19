package com.temipora.backend_springboot.servicios.informes;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.Locale;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Documento PDF de la plataforma: cabecera y pie en todas las páginas,
 * identificación del documento, secciones numeradas, indicadores, tablas y
 * notas. Los servicios de informes sólo describen su contenido.
 */
public final class DocumentoPdf {

    private final Document documento;
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
    private final String tipo;
    private final String referencia;
    private final String pie;

    private DocumentoPdf(
            Rectangle pagina, String tipo, String referencia, String pie) {
        this.tipo = tipo;
        this.referencia = referencia;
        this.pie = pie;
        this.documento = new Document(pagina,
            EstiloPdf.MARGEN_LATERAL, EstiloPdf.MARGEN_LATERAL,
            EstiloPdf.MARGEN_SUPERIOR, EstiloPdf.MARGEN_INFERIOR);
    }

    public static DocumentoPdf vertical(
            String tipo, String referencia, String pie) {
        return new DocumentoPdf(PageSize.A4, tipo, referencia, pie);
    }

    public static DocumentoPdf horizontal(
            String tipo, String referencia, String pie) {
        return new DocumentoPdf(PageSize.A4.rotate(), tipo, referencia, pie);
    }

    public void abrir() {
        try {
            PdfWriter escritor = PdfWriter.getInstance(documento, salida);
            escritor.setPageEvent(new EncabezadoPiePdf(tipo, referencia, pie));
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

    public DocumentoPdf cabecera(String titulo, String bajada) {
        Paragraph parrafo = new Paragraph(titulo, EstiloPdf.TITULO);
        parrafo.setSpacingAfter(3);
        agregar(parrafo);
        if (bajada != null && !bajada.isBlank()) {
            Paragraph detalle = new Paragraph(bajada, EstiloPdf.BAJADA);
            detalle.setLeading(12);
            detalle.setSpacingAfter(8);
            agregar(detalle);
        }
        return this;
    }

    public DocumentoPdf metadatos(String... pares) {
        PdfPTable tabla = new PdfPTable(4);
        try {
            tabla.setWidths(new float[] { 1.0f, 1.6f, 1.0f, 1.6f });
        } catch (DocumentException e) {
            throw new IllegalStateException(
                "No se pudo definir el panel de datos.", e);
        }
        tabla.setWidthPercentage(100);
        for (int indice = 0; indice < pares.length; indice += 2) {
            String valor = indice + 1 < pares.length ? pares[indice + 1] : "";
            tabla.addCell(celdaDato(pares[indice], EstiloPdf.ETIQUETA,
                EstiloPdf.CREMA));
            tabla.addCell(celdaDato(valor, EstiloPdf.VALOR_FUERTE,
                EstiloPdf.BLANCO));
        }
        int faltantes = (4 - pares.length % 4) % 4;
        for (int indice = 0; indice < faltantes; indice++) {
            tabla.addCell(celdaDato("", EstiloPdf.VALOR, EstiloPdf.BLANCO));
        }
        tabla.setSpacingAfter(10);
        agregar(tabla);
        return this;
    }

    public DocumentoPdf seccion(String numero, String titulo) {
        Phrase frase = new Phrase();
        frase.add(new Chunk(numero + "   ", EstiloPdf.SECCION_NUMERO));
        frase.add(new Chunk(titulo, EstiloPdf.SECCION));

        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        PdfPCell celda = new PdfPCell(frase);
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(EstiloPdf.ROSA);
        celda.setBorderWidth(0.8f);
        celda.setPadding(0);
        celda.setPaddingBottom(4);
        tabla.addCell(celda);
        tabla.setSpacingBefore(7);
        tabla.setSpacingAfter(6);
        agregar(tabla);
        return this;
    }

    public DocumentoPdf caption(String texto) {
        Paragraph parrafo = new Paragraph(texto, EstiloPdf.CHICO);
        parrafo.setSpacingAfter(7);
        agregar(parrafo);
        return this;
    }

    public DocumentoPdf parrafo(String texto) {
        Paragraph parrafo = new Paragraph(texto, EstiloPdf.VALOR);
        parrafo.setLeading(12);
        parrafo.setSpacingAfter(7);
        agregar(parrafo);
        return this;
    }

    public DocumentoPdf parrafoChico(String texto) {
        Paragraph parrafo = new Paragraph(texto, EstiloPdf.CHICO);
        parrafo.setLeading(10);
        parrafo.setSpacingAfter(4);
        agregar(parrafo);
        return this;
    }

    public DocumentoPdf subtitulo(String texto) {
        Paragraph parrafo = new Paragraph(texto, EstiloPdf.VALOR_FUERTE);
        parrafo.setSpacingBefore(6);
        parrafo.setSpacingAfter(3);
        agregar(parrafo);
        return this;
    }

    public DocumentoPdf indicadores(String[] etiquetas, String[] valores) {
        PdfPTable tabla = new PdfPTable(etiquetas.length);
        tabla.setWidthPercentage(100);

        for (int indice = 0; indice < etiquetas.length; indice++) {
            Paragraph titulo = new Paragraph(
                etiquetas[indice].toUpperCase(new Locale("es")),
                EstiloPdf.INDICADOR_ETIQUETA);
            titulo.setSpacingAfter(2);
            Paragraph valor = new Paragraph(
                valores[indice], EstiloPdf.INDICADOR_VALOR);

            PdfPCell celda = new PdfPCell();
            celda.setBackgroundColor(EstiloPdf.CREMA);
            celda.setBorderColor(EstiloPdf.ROSA);
            celda.setBorderWidth(0.5f);
            celda.setPadding(6);
            celda.addElement(titulo);
            celda.addElement(valor);
            tabla.addCell(celda);
        }
        tabla.setSpacingAfter(8);
        agregar(tabla);
        return this;
    }

    public DocumentoPdf resumen(
            String[] conceptos,
            String[] valores,
            int destacada,
            String encabezadoConcepto,
            String encabezadoImporte) {
        PdfPTable tabla = new PdfPTable(2);
        try {
            tabla.setWidths(new float[] { 3f, 1f });
        } catch (DocumentException e) {
            throw new IllegalStateException(
                "No se pudo definir el resumen.", e);
        }
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        tabla.addCell(celdaEncabezado(encabezadoConcepto, false));
        tabla.addCell(celdaEncabezado(encabezadoImporte, true));

        for (int indice = 0; indice < conceptos.length; indice++) {
            boolean resaltada = indice == destacada;
            tabla.addCell(celdaResumen(conceptos[indice], resaltada, false));
            tabla.addCell(celdaResumen(valores[indice], resaltada, true));
        }
        tabla.setSpacingAfter(8);
        agregar(tabla);
        return this;
    }

    public DocumentoPdf tabla(TablaPdf tabla) {
        PdfPTable pdf = tabla.pdf();
        pdf.setSpacingBefore(2);
        pdf.setSpacingAfter(7);
        agregar(pdf);
        return this;
    }

    public DocumentoPdf nota(String numero, String texto) {
        Paragraph parrafo = new Paragraph();
        parrafo.setLeading(11);
        parrafo.setIndentationLeft(12);
        parrafo.setFirstLineIndent(-12);
        parrafo.setSpacingAfter(4);
        parrafo.add(new Chunk(numero + ".   ", EstiloPdf.VALOR_FUERTE));
        parrafo.add(new Chunk(texto, EstiloPdf.CHICO));
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

    private static PdfPCell celdaDato(
            String texto, Font fuente, Color fondo) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setBackgroundColor(fondo);
        celda.setBorderColor(EstiloPdf.ROSA);
        celda.setBorderWidth(0.5f);
        celda.setPadding(4);
        return celda;
    }

    private static PdfPCell celdaEncabezado(String texto, boolean derecha) {
        PdfPCell celda = new PdfPCell(
            new Phrase(texto, EstiloPdf.TABLA_FUERTE));
        celda.setBackgroundColor(EstiloPdf.ROSA);
        celda.setBorderColor(EstiloPdf.ROSA);
        celda.setBorderWidth(0.5f);
        celda.setPadding(5);
        celda.setHorizontalAlignment(
            derecha ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        return celda;
    }

    private static PdfPCell celdaResumen(
            String texto, boolean resaltada, boolean derecha) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, resaltada
            ? EstiloPdf.TABLA_DESTACADA : EstiloPdf.VALOR));
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(EstiloPdf.ROSA);
        celda.setBorderWidth(0.5f);
        celda.setPadding(4);
        celda.setHorizontalAlignment(
            derecha ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        if (resaltada) {
            celda.setBackgroundColor(EstiloPdf.GRANATE);
        }
        return celda;
    }
}
