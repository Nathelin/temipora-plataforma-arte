package com.temipora.backend_springboot.servicios.informes;

import java.util.Locale;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

/**
 * Tabla de datos de un informe: encabezado en mayúsculas sobre una línea fina,
 * filas separadas por líneas claras, sin fondos ni filas alternadas, y fila de
 * totales con un remate superior.
 */
public final class TablaPdf {

    private static final float LINEA_FINA = 0.5f;

    private final PdfPTable tabla;
    private final boolean[] numericas;

    public TablaPdf(String[] encabezados, float[] anchos, boolean[] numericas) {
        if (encabezados.length != anchos.length
                || encabezados.length != numericas.length) {
            throw new IllegalArgumentException(
                "La tabla necesita la misma cantidad de columnas, anchos y "
                    + "alineaciones.");
        }
        this.numericas = numericas.clone();
        this.tabla = new PdfPTable(anchos.length);
        try {
            this.tabla.setWidths(anchos);
        } catch (DocumentException e) {
            throw new IllegalStateException(
                "No se pudieron definir los anchos de la tabla.", e);
        }
        this.tabla.setWidthPercentage(100);
        this.tabla.setHeaderRows(1);
        this.tabla.setSplitLate(true);
        for (int indice = 0; indice < encabezados.length; indice++) {
            PdfPCell celda = new PdfPCell(new Phrase(
                encabezados[indice].toUpperCase(new Locale("es")),
                EstiloPdf.TABLA_ENCABEZADO));
            celda.setBorder(Rectangle.BOTTOM);
            celda.setBorderColor(EstiloPdf.LINEA);
            celda.setBorderWidth(LINEA_FINA);
            celda.setPadding(4);
            celda.setPaddingTop(2);
            celda.setPaddingBottom(5);
            celda.setVerticalAlignment(Element.ALIGN_BOTTOM);
            celda.setHorizontalAlignment(alineacion(indice));
            tabla.addCell(celda);
        }
    }

    public TablaPdf fila(String... valores) {
        for (int indice = 0; indice < valores.length; indice++) {
            PdfPCell celda = new PdfPCell(new Phrase(
                valores[indice], EstiloPdf.TABLA));
            celda.setBorder(Rectangle.BOTTOM);
            celda.setBorderColor(EstiloPdf.LINEA);
            celda.setBorderWidth(LINEA_FINA);
            celda.setPadding(4);
            celda.setPaddingTop(4);
            celda.setPaddingBottom(4);
            celda.setHorizontalAlignment(alineacion(indice));
            tabla.addCell(celda);
        }
        return this;
    }

    public TablaPdf total(String... valores) {
        for (int indice = 0; indice < valores.length; indice++) {
            PdfPCell celda = new PdfPCell(new Phrase(
                valores[indice], EstiloPdf.TABLA_TOTAL));
            celda.setBorder(Rectangle.TOP);
            celda.setBorderColor(EstiloPdf.ACENTO);
            celda.setBorderWidth(0.8f);
            celda.setPadding(4);
            celda.setPaddingTop(5);
            celda.setPaddingBottom(4);
            celda.setHorizontalAlignment(alineacion(indice));
            tabla.addCell(celda);
        }
        return this;
    }

    PdfPTable pdf() {
        return tabla;
    }

    private int alineacion(int columna) {
        return numericas[columna]
            ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT;
    }
}
