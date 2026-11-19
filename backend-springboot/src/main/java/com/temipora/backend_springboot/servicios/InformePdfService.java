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
            TIPO, desde + " al " + hasta, PIE);
        documento.abrir();
        documento.cabecera("Ventas del período",
            "Actividad de " + artista + " entre el " + desde + " y el "
                + hasta + " (" + informe.periodo().dias() + " días).");
        documento.metadatos(
            "Emisor", artista,
            "Referencia", referencia(informe),
            "Período", desde + " al " + hasta,
            "Generado", FormatoPdf.fechaHora(generado(informe.generadoEn())),
            "Moneda", moneda,
            "Filtro", filtro(informe.tipo()));

        resumen(documento, informe, moneda);
        comparacion(documento, informe, moneda);
        detalle(documento, informe, moneda);

        return documento.cerrar();
    }

    private void resumen(
            DocumentoPdf documento, InformeVentasDTO informe, String moneda) {
        InformeTotalesDTO totales = informe.totales();
        documento.seccion("1", "Resumen del período");
        documento.parrafo("Este informe resume tus ventas cobradas en el "
            + "período y cómo se compone el importe que recibís.");
        documento.indicadores(
            new String[] { "Operaciones", "Unidades", "Ticket promedio" },
            new String[] { String.valueOf(totales.operaciones()),
                String.valueOf(totales.unidades()),
                FormatoPdf.monto(totales.ticketPromedio()) });
        BigDecimal bruto = totales.bruto() != null
            ? totales.bruto() : BigDecimal.ZERO;
        BigDecimal comision = totales.comision() != null
            ? totales.comision() : BigDecimal.ZERO;
        BigDecimal netoArtista = bruto.subtract(comision);
        BigDecimal liquidado = totales.netoPercibido() != null
            ? totales.netoPercibido() : BigDecimal.ZERO;

        documento.resumen(
            new String[] { "Ingresó por ventas",
                "Temipora cobró (comisión)", "Neto para vos",
                "MercadoPago descontó", "Se te pagó" },
            new String[] { FormatoPdf.monto(bruto),
                FormatoPdf.resta(comision),
                FormatoPdf.monto(netoArtista),
                FormatoPdf.resta(netoArtista.subtract(liquidado)),
                FormatoPdf.monto(liquidado) },
            4, "Concepto", "Importe (" + moneda + ")");
    }

    private void comparacion(
            DocumentoPdf documento, InformeVentasDTO informe, String moneda) {
        documento.seccion("2", "Comparación con el período anterior");
        var anterior = informe.comparacion();
        if (anterior == null) {
            documento.parrafo(
                "No hay un período anterior registrado para comparar.");
            return;
        }
        documento.caption("Período anterior: "
            + FormatoPdf.fecha(anterior.desde()) + " al "
            + FormatoPdf.fecha(anterior.hasta()) + ".");
        TablaPdf tabla = new TablaPdf(
            new String[] { "Concepto", "Período anterior (" + moneda + ")",
                "Variación" },
            new float[] { 2.4f, 1.3f, 1.3f },
            new boolean[] { false, true, true });
        tabla.fila("Neto percibido",
            FormatoPdf.monto(anterior.netoPercibido()),
            variacion(anterior.variacionNetoPorcentaje()));
        documento.tabla(tabla);
    }

    private void detalle(
            DocumentoPdf documento, InformeVentasDTO informe, String moneda) {
        documento.seccion("3", "Ventas por ítem", "importes en " + moneda);

        List<InformeItemDTO> items = informe.items() != null
            ? informe.items() : List.of();
        if (items.isEmpty()) {
            documento.parrafo("No hay ventas registradas en el período.");
            return;
        }

        TablaPdf tabla = new TablaPdf(
            new String[] { "Tipo", "Ítem", "Unid.", "Oper.", "Bruto",
                "Comisión", "Arancel MercadoPago", "Neto percibido",
                "Part. del neto" },
            new float[] { 0.95f, 2.2f, 0.7f, 0.7f, 1.2f, 1.2f, 1.25f, 1.2f,
                0.9f },
            new boolean[] { false, false, true, true, true, true, true, true,
                true });

        for (InformeItemDTO item : items) {
            tabla.fila(
                "PRODUCTO".equals(item.tipo()) ? "Producto" : "Servicio",
                FormatoPdf.texto(item.nombre()),
                String.valueOf(item.unidades()),
                String.valueOf(item.operaciones()),
                FormatoPdf.monto(item.bruto()),
                FormatoPdf.monto(item.comision()),
                FormatoPdf.monto(item.arancel()),
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
            return "Sin ventas en el período anterior";
        }
        String signo = valor.signum() >= 0 ? "+" : "";
        return signo + FormatoPdf.numero(valor) + "% vs. período anterior";
    }
}
