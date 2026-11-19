package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.modelo.InformeItemDTO;
import com.temipora.backend_springboot.modelo.InformeTotalesDTO;
import com.temipora.backend_springboot.modelo.InformeVentasDTO;
import com.temipora.backend_springboot.servicios.informes.DocumentoPdf;
import com.temipora.backend_springboot.servicios.informes.FormatoPdf;
import com.temipora.backend_springboot.servicios.informes.TablaPdf;

@Service
public class InformePdfService {

    private static final String TIPO = "Informe de ventas";
    private static final String PIE =
        "Documento interno del artista · generado automáticamente por Temipora";

    public byte[] generar(InformeVentasDTO informe, String nombreArtista) {
        String desde = FormatoPdf.fecha(informe.periodo().desde());
        String hasta = FormatoPdf.fecha(informe.periodo().hasta());
        String moneda = FormatoPdf.moneda(informe.moneda());
        String artista = "@" + FormatoPdf.texto(nombreArtista);

        DocumentoPdf documento = DocumentoPdf.vertical(
            TIPO, desde + " al " + hasta, referencia(informe), PIE);
        documento.abrir();
        documento.cabecera("Informe del artista", "Ventas del período",
            desde + " al " + hasta + " · " + informe.periodo().dias()
                + " días · Cifras en " + moneda);
        documento.metadatos(
            "Emisor", artista,
            "Filtro", filtro(informe.tipo()),
            "Referencia", referencia(informe),
            "Generado", FormatoPdf.fechaHora(generado(informe.generadoEn())));

        resumen(documento, informe);
        comparacion(documento, informe);
        detalle(documento, informe);

        return documento.cerrar();
    }

    private void resumen(DocumentoPdf documento, InformeVentasDTO informe) {
        InformeTotalesDTO totales = informe.totales();
        documento.seccion("1", "Resumen del período");
        documento.indicadores(
            new DocumentoPdf.Metrica(String.valueOf(totales.operaciones()),
                "Operaciones", null, false),
            new DocumentoPdf.Metrica(String.valueOf(totales.unidades()),
                "Unidades", null, false),
            new DocumentoPdf.Metrica(
                FormatoPdf.monto(totales.ticketPromedio()),
                "Ticket promedio", null, false),
            new DocumentoPdf.Metrica(
                FormatoPdf.monto(totales.netoPercibido()),
                "Te pagaron", null, true));

        BigDecimal bruto = totales.bruto() != null
            ? totales.bruto() : BigDecimal.ZERO;
        BigDecimal comision = totales.comision() != null
            ? totales.comision() : BigDecimal.ZERO;
        BigDecimal netoArtista = bruto.subtract(comision);
        BigDecimal liquidado = totales.netoPercibido() != null
            ? totales.netoPercibido() : BigDecimal.ZERO;

        documento.resumen(
            new String[] { "Ingresó por ventas", "Comisión Temipora",
                "Neto para vos", "Costo de MercadoPago", "Se te pagó" },
            new String[] { FormatoPdf.monto(bruto),
                FormatoPdf.resta(comision),
                FormatoPdf.monto(netoArtista),
                FormatoPdf.resta(netoArtista.subtract(liquidado)),
                FormatoPdf.monto(liquidado) },
            4);
    }

    private void comparacion(
            DocumentoPdf documento, InformeVentasDTO informe) {
        var anterior = informe.comparacion();
        if (anterior == null) {
            documento.seccion("2", "Comparación con el período anterior");
            documento.parrafo(
                "No hay un período anterior registrado para comparar.");
            return;
        }
        documento.seccion("2", "Comparación con el período anterior",
            FormatoPdf.fecha(anterior.desde()) + " al "
                + FormatoPdf.fecha(anterior.hasta()));

        TablaPdf tabla = new TablaPdf(
            new String[] { "Concepto", "Anterior", "Actual", "Variación" },
            new float[] { 2.1f, 1.2f, 1.2f, 1.8f },
            new boolean[] { false, true, true, true });
        tabla.fila("Neto percibido",
            FormatoPdf.monto(anterior.netoPercibido()),
            FormatoPdf.monto(informe.totales().netoPercibido()),
            variacion(anterior.variacionNetoPorcentaje()));
        documento.tabla(tabla);
    }

    private void detalle(DocumentoPdf documento, InformeVentasDTO informe) {
        documento.seccion("3", "Ventas por ítem");

        List<InformeItemDTO> items = informe.items() != null
            ? informe.items() : List.of();
        if (items.isEmpty()) {
            documento.parrafo("No hay ventas registradas en el período.");
            return;
        }

        TablaPdf tabla = new TablaPdf(
            new String[] { "Tipo", "Ítem", "Unid.", "Oper.", "Bruto",
                "Comisión", "Neto percibido", "Part. del neto" },
            new float[] { 0.95f, 2.4f, 0.7f, 0.7f, 1.25f, 1.25f, 1.3f,
                0.95f },
            new boolean[] { false, false, true, true, true, true, true,
                true });

        for (InformeItemDTO item : items) {
            tabla.fila(
                "PRODUCTO".equals(item.tipo()) ? "Producto" : "Servicio",
                FormatoPdf.texto(item.nombre()),
                String.valueOf(item.unidades()),
                String.valueOf(item.operaciones()),
                FormatoPdf.monto(item.bruto()),
                FormatoPdf.monto(item.comision()),
                FormatoPdf.monto(item.netoPercibido()),
                FormatoPdf.numero(item.participacion()) + "%");
        }
        documento.tabla(tabla);
    }

    private String referencia(InformeVentasDTO informe) {
        return "VEN-" + FormatoPdf.fechaCompacta(informe.periodo().desde())
            + "-" + FormatoPdf.fechaCompacta(informe.periodo().hasta());
    }

    private LocalDateTime generado(LocalDateTime valor) {
        return valor != null ? valor : LocalDateTime.now();
    }

    private static String filtro(String tipo) {
        if (tipo == null) {
            return "Productos y servicios";
        }
        return switch (tipo) {
            case "PRODUCTOS" -> "Solo productos";
            case "SERVICIOS" -> "Solo servicios";
            default -> "Productos y servicios";
        };
    }

    private static String variacion(BigDecimal valor) {
        if (valor == null) {
            return "sin ventas en el período anterior";
        }
        String signo = valor.signum() >= 0 ? "+" : "";
        return signo + FormatoPdf.numero(valor) + "%";
    }
}
