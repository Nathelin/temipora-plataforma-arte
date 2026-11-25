package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.modelo.HistorialClienteDTO;
import com.temipora.backend_springboot.modelo.HistorialItemDTO;
import com.temipora.backend_springboot.modelo.HistorialResumenDTO;
import com.temipora.backend_springboot.servicios.informes.DocumentoPdf;
import com.temipora.backend_springboot.servicios.informes.FormatoPdf;
import com.temipora.backend_springboot.servicios.informes.TablaPdf;

@Service
public class InformeHistorialPdfService {

    private static final String TIPO = "Historial de pedidos y compras";
    private static final String PIE =
        "Documento personal generado automáticamente por Temipora · "
            + "Conservá esta constancia como comprobante de tu actividad";

    public byte[] generar(HistorialClienteDTO informe, String cuenta) {
        DocumentoPdf documento = DocumentoPdf.vertical(
            TIPO, informe.periodoTexto(), referencia(informe), PIE);
        documento.abrir();
        documento.cabecera("Tu actividad", "Historial de pedidos y compras",
            informe.periodoTexto() + " · " + informe.filtroTexto());
        documento.metadatos(
            "Cuenta", cuenta,
            "Período", informe.periodoTexto(),
            "Filtro", informe.filtroTexto(),
            "Moneda", "ARS",
            "Referencia", referencia(informe),
            "Generado", FormatoPdf.fechaHora(informe.generadoEn()));

        resumen(documento, informe.resumen());
        operaciones(documento, informe);

        return documento.cerrar();
    }

    private void resumen(
            DocumentoPdf documento, HistorialResumenDTO resumen) {
        documento.seccion("1", "Resumen");
        documento.indicadores(
            new DocumentoPdf.Metrica(
                String.valueOf(resumen.operaciones()), "Operaciones", null,
                true),
            new DocumentoPdf.Metrica(
                String.valueOf(resumen.pedidos()), "Servicios", null, false),
            new DocumentoPdf.Metrica(
                String.valueOf(resumen.compras()), "Productos", null, false),
            new DocumentoPdf.Metrica(
                FormatoPdf.monto(resumen.descuentos()),
                "Descuentos aplicados", "ARS", true));

        documento.resumen(
            new String[] { "Importe total", "Importe completado",
                "Completadas", "En curso", "Pendientes", "Canceladas" },
            new String[] { FormatoPdf.monto(resumen.importeTotal()),
                FormatoPdf.monto(resumen.importeCompletado()),
                String.valueOf(resumen.completadas()),
                String.valueOf(resumen.enCurso()),
                String.valueOf(resumen.pendientes()),
                String.valueOf(resumen.canceladas()) },
            -1);
    }

    private void operaciones(
            DocumentoPdf documento, HistorialClienteDTO informe) {
        documento.seccion("2", "Operaciones", informe.filtroTexto());
        List<HistorialItemDTO> items = informe.contenido() != null
            ? informe.contenido() : List.of();
        if (items.isEmpty()) {
            documento.parrafo(
                "No hay operaciones para los filtros elegidos.");
            return;
        }

        TablaPdf tabla = new TablaPdf(
            new String[] { "Fecha", "Tipo", "Detalle", "Artista",
                "Importe", "Estado", "Descuento" },
            new float[] { 0.8f, 0.6f, 2.0f, 1.1f, 0.9f, 1.1f, 0.8f },
            new boolean[] { false, false, false, false, true, false, false });

        BigDecimal totalImporte = BigDecimal.ZERO;
        BigDecimal totalDescuento = BigDecimal.ZERO;
        for (HistorialItemDTO item : items) {
            totalImporte = totalImporte.add(nulo(item.monto()));
            totalDescuento = totalDescuento.add(
                nulo(item.descuentoMonto()));
            tabla.fila(
                fecha(item),
                FormatoPdf.texto(item.tipoEtiqueta()),
                recortar(item.titulo(), 42),
                recortar(item.contraparte(), 20),
                FormatoPdf.monto(item.monto()),
                FormatoPdf.texto(item.estadoEtiqueta()),
                descuento(item));
        }
        tabla.total("Total", String.valueOf(items.size()), "", "", 
            FormatoPdf.monto(totalImporte), "",
            FormatoPdf.monto(totalDescuento));
        documento.tabla(tabla);

        if (informe.totalElementos() > items.size()) {
            documento.parrafoChico(
                "Se listan las primeras " + items.size() + " de "
                    + informe.totalElementos()
                    + " operaciones. Acotá el período o los filtros para "
                    + "ver el resto.");
        }
    }

    private static String fecha(HistorialItemDTO item) {
        return item.fecha() == null
            ? "—" : FormatoPdf.fecha(item.fecha().toLocalDate());
    }

    private static String descuento(HistorialItemDTO item) {
        BigDecimal monto = nulo(item.descuentoMonto());
        BigDecimal porcentaje = nulo(item.descuentoPorcentaje());
        if (monto.signum() <= 0 && porcentaje.signum() <= 0) {
            return "—";
        }
        String base = monto.signum() > 0
            ? FormatoPdf.monto(monto)
            : FormatoPdf.monto(BigDecimal.ZERO);
        if (porcentaje.signum() > 0) {
            return "-" + FormatoPdf.porcentaje(porcentaje) + " · " + base;
        }
        return base;
    }

    private static String recortar(String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return "—";
        }
        return valor.length() <= maximo
            ? valor
            : valor.substring(0, maximo - 1) + "…";
    }

    private static String referencia(HistorialClienteDTO informe) {
        if (informe.desde() == null && informe.hasta() == null) {
            return "HIST-COMPLETO";
        }
        String desde = informe.desde() != null
            ? FormatoPdf.fechaCompacta(informe.desde()) : "inicio";
        String hasta = informe.hasta() != null
            ? FormatoPdf.fechaCompacta(informe.hasta()) : "hoy";
        return "HIST-" + desde + "-" + hasta;
    }

    private static BigDecimal nulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
