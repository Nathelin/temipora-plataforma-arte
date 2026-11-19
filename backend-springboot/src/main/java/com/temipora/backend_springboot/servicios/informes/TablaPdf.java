package com.temipora.backend_springboot.servicios.informes;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

/**
 * Tabla de datos de un informe: encabezado que se repite en cada página, filas
 * alternadas, alineación por columna y fila de totales.
 */
public final class TablaPdf {

    private static final float BORDE = 0.4f;

    private final PdfPTable tabla;
    private final boolean[] numericas;
    private int filas;

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
            PdfPCell celda = new PdfPCell(
                new Phrase(encabezados[indice], EstiloPdf.TABLA_FUERTE));
            celda.setBackgroundColor(EstiloPdf.ROSA);
            celda.setBorderColor(EstiloPdf.ROSA);
            celda.setBorderWidth(BORDE);
            celda.setPadding(3.5f);
            celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
            celda.setHorizontalAlignment(alineacion(indice));
            tabla.addCell(celda);
        }
    }

    public TablaPdf fila(String... valores) {
        for (int indice = 0; indice < valores.length; indice++) {
            PdfPCell celda = new PdfPCell(
                new Phrase(valores[indice], EstiloPdf.TABLA));
            celda.setBorderColor(EstiloPdf.ROSA);
            celda.setBorderWidth(BORDE);
            celda.setPadding(3.5f);
            celda.setHorizontalAlignment(alineacion(indice));
            if (filas % 2 == 1) {
                celda.setBackgroundColor(EstiloPdf.SUPERFICIE_ALT);
            }
            tabla.addCell(celda);
        }
        filas++;
        return this;
    }

    public TablaPdf total(String... valores) {
        for (int indice = 0; indice < valores.length; indice++) {
            PdfPCell celda = new PdfPCell(
                new Phrase(valores[indice], EstiloPdf.TABLA_FUERTE));
            celda.setBackgroundColor(EstiloPdf.ROSA);
            celda.setBorderColor(EstiloPdf.ROSA);
            celda.setBorderWidth(BORDE);
            celda.setPadding(3.5f);
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
