package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.modelo.InformeAdminDTO;
import com.temipora.backend_springboot.modelo.InformeAdminItemDTO;
import com.temipora.backend_springboot.modelo.InformeAdminTipoDTO;
import com.temipora.backend_springboot.modelo.InformeAdminTotalesDTO;
import com.temipora.backend_springboot.modelo.InformeAdminVendedorDTO;
import com.temipora.backend_springboot.servicios.informes.DocumentoPdf;
import com.temipora.backend_springboot.servicios.informes.FormatoPdf;
import com.temipora.backend_springboot.servicios.informes.TablaPdf;

@Service
public class InformeAdminPdfService {

    private static final String TIPO = "Informe de ventas y comisiones";
    private static final String PIE =
        "Documento interno de administración · generado automáticamente por "
            + "Temipora";

    public byte[] generar(InformeAdminDTO informe) {
        String desde = FormatoPdf.fecha(informe.periodo().desde());
        String hasta = FormatoPdf.fecha(informe.periodo().hasta());
        String moneda = FormatoPdf.moneda(informe.moneda());

        DocumentoPdf documento = DocumentoPdf.vertical(
            TIPO, desde + " al " + hasta, PIE);
        documento.abrir();
        documento.cabecera("Resumen de la plataforma",
            "Ventas y comisiones de Temipora entre el " + desde + " y el "
                + hasta + " (" + informe.periodo().dias() + " días).");
        documento.metadatos(
            "Alcance", "Toda la plataforma",
            "Referencia", referencia(informe),
            "Período", desde + " al " + hasta,
            "Generado", FormatoPdf.fechaHora(generado(informe.generadoEn())),
            "Moneda", moneda,
            "Filtro", filtro(informe.tipo()));

        resumen(documento, informe, moneda);
        comparacion(documento, informe, moneda);
        porTipo(documento, informe);
        porVendedor(documento, informe);
        porItem(documento, informe);
        notas(documento);

        return documento.cerrar();
    }

    private void resumen(
            DocumentoPdf documento, InformeAdminDTO informe, String moneda) {
        InformeAdminTotalesDTO totales = informe.totales();
        documento.seccion("1", "Resumen");
        documento.indicadores(
            new String[] { "Operaciones", "Unidades", "Ticket promedio" },
            new String[] { String.valueOf(totales.operaciones()),
                String.valueOf(totales.unidades()),
                FormatoPdf.monto(totales.ticketPromedio()) });
        documento.resumen(
            new String[] { "Volumen procesado",
                "Ingresos de la plataforma (comisión)", "Neto a artistas",
                "Arancel MercadoPago", "Neto liquidado a artistas" },
            new String[] { FormatoPdf.monto(totales.volumen()),
                FormatoPdf.monto(totales.comision()),
                FormatoPdf.monto(totales.netoArtistas()),
                FormatoPdf.monto(totales.arancel()),
                FormatoPdf.monto(totales.netoPercibido()) },
            4, "Concepto", "Importe (" + moneda + ")");
    }

    private void comparacion(
            DocumentoPdf documento, InformeAdminDTO informe, String moneda) {
        documento.seccion("2", "Comparación");
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
        tabla.fila("Volumen procesado", FormatoPdf.monto(anterior.volumen()),
            variacion(anterior.variacionVolumenPorcentaje()));
        tabla.fila("Ingresos de la plataforma (comisión)",
            FormatoPdf.monto(anterior.comision()),
            variacion(anterior.variacionComisionPorcentaje()));
        documento.tabla(tabla);
    }

    private void porTipo(
            DocumentoPdf documento, InformeAdminDTO informe) {
        documento.seccion("3", "Por tipo");
        List<InformeAdminTipoDTO> tipos = informe.tipos() != null
            ? informe.tipos() : List.of();
        if (tipos.isEmpty()) {
            documento.parrafo("No hay ventas registradas en el período.");
            return;
        }

        TablaPdf tabla = new TablaPdf(
            new String[] { "Tipo", "Oper.", "Unid.", "Volumen", "Comisión",
                "Neto artistas", "Arancel" },
            new float[] { 1.5f, 0.8f, 0.8f, 1.5f, 1.45f, 1.5f, 1.3f },
            new boolean[] { false, true, true, true, true, true, true });

        long operaciones = 0;
        long unidades = 0;
        BigDecimal volumen = BigDecimal.ZERO;
        BigDecimal comision = BigDecimal.ZERO;
        BigDecimal neto = BigDecimal.ZERO;
        BigDecimal arancel = BigDecimal.ZERO;

        for (InformeAdminTipoDTO tipo : tipos) {
            tabla.fila(tipo(tipo.tipo()),
                String.valueOf(tipo.operaciones()),
                String.valueOf(tipo.unidades()),
                FormatoPdf.monto(tipo.volumen()),
                FormatoPdf.monto(tipo.comision()),
                FormatoPdf.monto(tipo.netoArtistas()),
                FormatoPdf.monto(tipo.arancel()));
            operaciones += tipo.operaciones();
            unidades += tipo.unidades();
            volumen = volumen.add(nulo(tipo.volumen()));
            comision = comision.add(nulo(tipo.comision()));
            neto = neto.add(nulo(tipo.netoArtistas()));
            arancel = arancel.add(nulo(tipo.arancel()));
        }
        tabla.total("Total", String.valueOf(operaciones),
            String.valueOf(unidades), FormatoPdf.monto(volumen),
            FormatoPdf.monto(comision), FormatoPdf.monto(neto),
            FormatoPdf.monto(arancel));
        documento.tabla(tabla);
    }

    private void porVendedor(
            DocumentoPdf documento, InformeAdminDTO informe) {
        documento.seccion("4", "Por vendedor y artista");
        List<InformeAdminVendedorDTO> vendedores = informe.vendedores() != null
            ? informe.vendedores() : List.of();
        if (vendedores.isEmpty()) {
            documento.parrafo(
                "No hay vendedores con ventas en el período.");
            return;
        }

        TablaPdf tabla = new TablaPdf(
            new String[] { "Vendedor / artista", "Oper.", "Unid.", "Volumen",
                "Comisión", "Neto artistas", "Arancel" },
            new float[] { 2.5f, 0.75f, 0.75f, 1.4f, 1.35f, 1.45f, 1.25f },
            new boolean[] { false, true, true, true, true, true, true });

        long operaciones = 0;
        long unidades = 0;
        BigDecimal volumen = BigDecimal.ZERO;
        BigDecimal comision = BigDecimal.ZERO;
        BigDecimal neto = BigDecimal.ZERO;
        BigDecimal arancel = BigDecimal.ZERO;

        for (InformeAdminVendedorDTO vendedor : vendedores) {
            tabla.fila(FormatoPdf.texto(vendedor.nombre()),
                String.valueOf(vendedor.operaciones()),
                String.valueOf(vendedor.unidades()),
                FormatoPdf.monto(vendedor.volumen()),
                FormatoPdf.monto(vendedor.comision()),
                FormatoPdf.monto(vendedor.netoArtistas()),
                FormatoPdf.monto(vendedor.arancel()));
            operaciones += vendedor.operaciones();
            unidades += vendedor.unidades();
            volumen = volumen.add(nulo(vendedor.volumen()));
            comision = comision.add(nulo(vendedor.comision()));
            neto = neto.add(nulo(vendedor.netoArtistas()));
            arancel = arancel.add(nulo(vendedor.arancel()));
        }
        tabla.total("Total", String.valueOf(operaciones),
            String.valueOf(unidades), FormatoPdf.monto(volumen),
            FormatoPdf.monto(comision), FormatoPdf.monto(neto),
            FormatoPdf.monto(arancel));
        documento.tabla(tabla);
    }

    private void porItem(
            DocumentoPdf documento, InformeAdminDTO informe) {
        documento.seccion("5", "Por ítem del catálogo");
        List<InformeAdminItemDTO> items = informe.items() != null
            ? informe.items() : List.of();
        if (items.isEmpty()) {
            documento.parrafo("No hay ítems vendidos en el período.");
            return;
        }

        TablaPdf tabla = new TablaPdf(
            new String[] { "Tipo", "Ítem", "Oper.", "Unid.", "Volumen",
                "Comisión", "Neto artistas", "Arancel" },
            new float[] { 0.95f, 2.45f, 0.7f, 0.7f, 1.3f, 1.25f, 1.3f,
                1.1f },
            new boolean[] { false, false, true, true, true, true, true, true });

        for (InformeAdminItemDTO item : items) {
            tabla.fila(tipo(item.tipo()),
                FormatoPdf.texto(item.nombre()),
                String.valueOf(item.operaciones()),
                String.valueOf(item.unidades()),
                FormatoPdf.monto(item.volumen()),
                FormatoPdf.monto(item.comision()),
                FormatoPdf.monto(item.netoArtistas()),
                FormatoPdf.monto(item.arancel()));
        }
        documento.tabla(tabla);
    }

    private void notas(DocumentoPdf documento) {
        documento.seccion("6", "Notas");
        documento.nota("1", "La comisión es el ingreso de Temipora por el uso "
            + "de la plataforma.");
        documento.nota("2", "El arancel es el que informa el medio de pago.");
    }

    private String referencia(InformeAdminDTO informe) {
        return "ADM-" + FormatoPdf.fechaCompacta(informe.periodo().desde())
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

    private static String tipo(String valor) {
        return "PRODUCTO".equals(valor) ? "Producto" : "Servicio";
    }

    private static String variacion(BigDecimal valor) {
        if (valor == null) {
            return "Sin ventas en el período anterior";
        }
        String signo = valor.signum() >= 0 ? "+" : "";
        return signo + FormatoPdf.numero(valor) + "%";
    }

    private static BigDecimal nulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
