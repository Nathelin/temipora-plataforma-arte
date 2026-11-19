package com.temipora.backend_springboot.servicios.informes;

import java.awt.Color;

import com.lowagie.text.Font;

/**
 * Paleta y tipografía compartidas por los documentos PDF. Los colores son los
 * tokens de identidad de Temipora documentados en el manual de usuario (7.1).
 */
public final class EstiloPdf {

    public static final Color GRANATE = new Color(0x4a1d2e);
    public static final Color CAFE = new Color(0x6e3b4d);
    public static final Color CORAL = new Color(0xc2185b);
    public static final Color ROJIZO = new Color(0x9a1047);
    public static final Color ROSA = new Color(0xffc2d1);
    public static final Color CREMA = new Color(0xfff6f1);
    public static final Color SUPERFICIE_ALT = new Color(0xfff0e9);
    public static final Color BLANCO = Color.WHITE;

    public static final Font MARCA = new Font(Font.HELVETICA, 11, Font.BOLD, GRANATE);
    public static final Font TITULO = new Font(Font.HELVETICA, 18, Font.BOLD, GRANATE);
    public static final Font BAJADA = new Font(Font.HELVETICA, 9, Font.NORMAL, CAFE);
    public static final Font SECCION = new Font(Font.HELVETICA, 11, Font.BOLD, GRANATE);
    public static final Font SECCION_NUMERO = new Font(Font.HELVETICA, 11, Font.BOLD, CORAL);
    public static final Font ETIQUETA = new Font(Font.HELVETICA, 7.5f, Font.BOLD, CAFE);
    public static final Font VALOR = new Font(Font.HELVETICA, 9, Font.NORMAL, GRANATE);
    public static final Font VALOR_FUERTE = new Font(Font.HELVETICA, 9, Font.BOLD, GRANATE);
    public static final Font CHICO = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, CAFE);
    public static final Font TABLA = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, GRANATE);
    public static final Font TABLA_FUERTE = new Font(Font.HELVETICA, 7.5f, Font.BOLD, GRANATE);
    public static final Font TABLA_DESTACADA = new Font(Font.HELVETICA, 9, Font.BOLD, BLANCO);
    public static final Font INDICADOR_ETIQUETA = new Font(Font.HELVETICA, 6.8f, Font.BOLD, CAFE);
    public static final Font INDICADOR_VALOR = new Font(Font.HELVETICA, 12, Font.BOLD, GRANATE);

    public static final float MARGEN_LATERAL = 40f;
    public static final float MARGEN_SUPERIOR = 64f;
    public static final float MARGEN_INFERIOR = 60f;

    private EstiloPdf() {
    }
}
