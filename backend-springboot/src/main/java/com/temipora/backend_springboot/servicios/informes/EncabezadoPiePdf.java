package com.temipora.backend_springboot.servicios.informes;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Encabezado y pie que se dibujan en todas las páginas del documento, con la
 * numeración "Página X de Y". El total se completa al cerrar el documento,
 * cuando ya se conoce la cantidad de páginas.
 */
final class EncabezadoPiePdf extends PdfPageEventHelper {

    private static final float SEPARACION = 9f;
    private static final float RESERVA_TOTAL = 16f;
    private static final float ESPACIO_ETIQUETA = 4f;

    private final String tipo;
    private final String referencia;
    private final String pie;
    private final String detallePie;
    private final String emisor;
    private PdfTemplate totalPaginas;
    private BaseFont fuente;

    EncabezadoPiePdf(
            String tipo, String referencia, String pie, String detallePie,
            String emisor) {
        this.tipo = tipo;
        this.referencia = referencia;
        this.pie = pie;
        this.detallePie = detallePie;
        this.emisor = emisor;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document documento) {
        totalPaginas = writer.getDirectContent().createTemplate(30, 12);
        try {
            fuente = BaseFont.createFont(
                BaseFont.HELVETICA_BOLD, BaseFont.WINANSI,
                BaseFont.NOT_EMBEDDED);
        } catch (DocumentException | java.io.IOException e) {
            throw new IllegalStateException(
                "No se pudo preparar la numeración del documento.", e);
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document documento) {
        PdfContentByte lienzo = writer.getDirectContent();
        Rectangle pagina = documento.getPageSize();
        float izquierda = documento.leftMargin();
        float derecha = pagina.getWidth() - documento.rightMargin();
        float arriba = pagina.getHeight() - documento.topMargin() + 20;
        float abajo = documento.bottomMargin() - 32;

        lienzo.setColorStroke(EstiloPdf.LINEA);
        lienzo.setLineWidth(0.6f);

        ColumnText.showTextAligned(lienzo, Element.ALIGN_LEFT,
            new Phrase("Temipora", EstiloPdf.MARCA), izquierda, arriba, 0);
        ColumnText.showTextAligned(lienzo, Element.ALIGN_RIGHT,
            new Phrase(referencia(tipo, referencia), EstiloPdf.DOCUMENTO),
            derecha, arriba, 0);

        lienzo.moveTo(izquierda, arriba - SEPARACION);
        lienzo.lineTo(derecha, arriba - SEPARACION);
        lienzo.stroke();

        lienzo.moveTo(izquierda, abajo + 24);
        lienzo.lineTo(derecha, abajo + 24);
        lienzo.stroke();

        ColumnText.showTextAligned(lienzo, Element.ALIGN_LEFT,
            new Phrase(pie, EstiloPdf.PIE), izquierda, abajo + 12, 0);
        ColumnText.showTextAligned(lienzo, Element.ALIGN_LEFT,
            new Phrase(detallePie, EstiloPdf.PIE), izquierda, abajo, 0);
        if (emisor != null && !emisor.isBlank()) {
            ColumnText.showTextAligned(lienzo, Element.ALIGN_LEFT,
                new Phrase("Emitido por " + emisor, EstiloPdf.PIE),
                izquierda, abajo - 10, 0);
        }

        String etiqueta = "Página " + writer.getPageNumber() + " de";
        float anchoEtiqueta = fuente.getWidthPoint(etiqueta, 7.5f);
        ColumnText.showTextAligned(lienzo, Element.ALIGN_LEFT,
            new Phrase(etiqueta, EstiloPdf.PIE_DESTACADO),
            derecha - RESERVA_TOTAL - ESPACIO_ETIQUETA - anchoEtiqueta,
            abajo + 12, 0);
        lienzo.addTemplate(totalPaginas, derecha - RESERVA_TOTAL, abajo + 12);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document documento) {
        totalPaginas.beginText();
        totalPaginas.setFontAndSize(fuente, 7.5f);
        totalPaginas.setColorFill(EstiloPdf.TEXTO);
        totalPaginas.setTextMatrix(0, 0);
        totalPaginas.showText(String.valueOf(writer.getPageNumber() - 1));
        totalPaginas.endText();
    }

    private static String referencia(String tipo, String referencia) {
        return referencia == null || referencia.isBlank()
            ? tipo : tipo + "  ·  " + referencia;
    }
}
