package com.temipora.backend_springboot.servicios.informes;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Formateo común de fechas e importes para los PDF. Las fechas se muestran
 * siempre como dd/MM/aaaa y los importes con dos decimales.
 */
public final class FormatoPdf {

    private static final DateTimeFormatter FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA_ARCHIVO =
        DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FECHA_COMPACTA =
        DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String VACIO = "—";

    private FormatoPdf() {
    }

    public static String fecha(LocalDate valor) {
        return valor == null ? VACIO : valor.format(FECHA);
    }

    public static String fechaHora(LocalDateTime valor) {
        return valor == null ? VACIO : valor.format(FECHA_HORA);
    }

    public static String fechaArchivo(LocalDate valor) {
        return valor == null ? "sin-fecha" : valor.format(FECHA_ARCHIVO);
    }

    public static String fechaCompacta(LocalDate valor) {
        return valor == null ? "sinfecha" : valor.format(FECHA_COMPACTA);
    }

    public static String monto(BigDecimal valor) {
        return valor == null ? VACIO
            : new DecimalFormat("#,##0.00").format(valor);
    }

    public static String numero(Number valor) {
        return valor == null ? VACIO
            : new DecimalFormat("#,##0.##").format(valor);
    }

    public static String texto(String valor) {
        return valor == null || valor.isBlank() ? VACIO : valor;
    }

    public static String moneda(String valor) {
        return valor == null || valor.isBlank() ? VACIO : valor;
    }
}
