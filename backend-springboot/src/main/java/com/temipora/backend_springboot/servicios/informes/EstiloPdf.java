package com.temipora.backend_springboot.servicios.informes;

import java.awt.Color;

import com.lowagie.text.Font;

/**
 * Paleta y tipografía compartidas por los documentos PDF. La identidad es
 * neutra: texto casi negro, secundarios grises, líneas muy claras y un único
 * acento ciruela para la marca y los remates.
 */
public final class EstiloPdf {

    public static final Color TEXTO = new Color(0x1f1a1c);
    public static final Color SECUNDARIO = new Color(0x6b6468);
    public static final Color ACENTO = new Color(0x7a2e4d);
    public static final Color LINEA = new Color(0xe6e2e4);

    public static final Font MARCA = new Font(Font.HELVETICA, 13.5f, Font.BOLD, ACENTO);
    public static final Font DOCUMENTO = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, SECUNDARIO);
    public static final Font EYEBROW = new Font(Font.HELVETICA, 8.5f, Font.BOLD, ACENTO);
    public static final Font TITULO = new Font(Font.HELVETICA, 20, Font.BOLD, TEXTO);
    public static final Font SUBTITULO = new Font(Font.HELVETICA, 10, Font.NORMAL, SECUNDARIO);
    public static final Font SECCION = new Font(Font.HELVETICA, 12.5f, Font.BOLD, TEXTO);
    public static final Font SECCION_NUMERO = new Font(Font.HELVETICA, 12.5f, Font.BOLD, ACENTO);
    public static final Font ETIQUETA = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, SECUNDARIO);
    public static final Font VALOR = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXTO);
    public static final Font VALOR_TOTAL = new Font(Font.HELVETICA, 9.5f, Font.BOLD, TEXTO);
    public static final Font METRICA = new Font(Font.HELVETICA, 22, Font.BOLD, TEXTO);
    public static final Font METRICA_ACENTO = new Font(Font.HELVETICA, 22, Font.BOLD, ACENTO);
    public static final Font METRICA_ETIQUETA = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, SECUNDARIO);
    public static final Font METRICA_DETALLE = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, SECUNDARIO);
    public static final Font TABLA_ENCABEZADO = new Font(Font.HELVETICA, 7.5f, Font.BOLD, SECUNDARIO);
    public static final Font TABLA = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, TEXTO);
    public static final Font TABLA_TOTAL = new Font(Font.HELVETICA, 8.5f, Font.BOLD, TEXTO);
    public static final Font PIE = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, SECUNDARIO);
    public static final Font PIE_DESTACADO = new Font(Font.HELVETICA, 7.5f, Font.BOLD, TEXTO);

    public static final float MARGEN_LATERAL = 42f;
    public static final float MARGEN_SUPERIOR = 60f;
    public static final float MARGEN_INFERIOR = 54f;

    private EstiloPdf() {
    }
}
