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

    private static final float SEPARACION = 7f;
    private static final float RESERVA_TOTAL = 16f;

    private final String tipo;
    private final String referencia;
    private final String pie;
    private PdfTemplate totalPaginas;
    private BaseFont fuente;

    EncabezadoPiePdf(String tipo, String referencia, String pie) {
        this.tipo = tipo;
        this.referencia = referencia;
        this.pie = pie;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document documento) {
        totalPaginas = writer.getDirectContent().createTemplate(30, 12);
        try {
            fuente = BaseFont.createFont(
                BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
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
        float arriba = pagina.getHeight() - documento.topMargin() + 22;
        float abajo = documento.bottomMargin() - 30;

        lienzo.setColorStroke(EstiloPdf.ROSA);
        lienzo.setLineWidth(0.8f);

        ColumnText.showTextAligned(lienzo, Element.ALIGN_LEFT,
            new Phrase("Temipora", EstiloPdf.MARCA), izquierda, arriba, 0);
        ColumnText.showTextAligned(lienzo, Element.ALIGN_RIGHT,
            new Phrase(referencia(tipo, referencia), EstiloPdf.CHICO),
            derecha, arriba, 0);

        lienzo.moveTo(izquierda, arriba - SEPARACION);
        lienzo.lineTo(derecha, arriba - SEPARACION);
        lienzo.stroke();

        lienzo.moveTo(izquierda, abajo + 14);
        lienzo.lineTo(derecha, abajo + 14);
        lienzo.stroke();

        ColumnText.showTextAligned(lienzo, Element.ALIGN_LEFT,
            new Phrase(pie, EstiloPdf.CHICO), izquierda, abajo, 0);

        String etiqueta = "Página " + writer.getPageNumber() + " de ";
        float anchoEtiqueta = fuente.getWidthPoint(etiqueta, 7.5f);
        ColumnText.showTextAligned(lienzo, Element.ALIGN_LEFT,
            new Phrase(etiqueta, EstiloPdf.CHICO),
            derecha - RESERVA_TOTAL - anchoEtiqueta, abajo, 0);
        lienzo.addTemplate(totalPaginas, derecha - RESERVA_TOTAL, abajo);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document documento) {
        totalPaginas.beginText();
        totalPaginas.setFontAndSize(fuente, 7.5f);
        totalPaginas.setColorFill(EstiloPdf.CAFE);
        totalPaginas.setTextMatrix(0, 0);
        totalPaginas.showText(String.valueOf(writer.getPageNumber() - 1));
        totalPaginas.endText();
    }

    private static String referencia(String tipo, String referencia) {
        return referencia == null || referencia.isBlank()
            ? tipo : tipo + "  ·  " + referencia;
    }
}
