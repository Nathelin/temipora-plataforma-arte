package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
            TIPO, desde + " al " + hasta, referencia(informe), PIE);
        documento.abrir();
        documento.cabecera("Informe de administración",
            "Ventas y comisiones",
            desde + " al " + hasta + " · " + informe.periodo().dias()
                + " días · Cifras en " + moneda);
        documento.metadatos(
            "Alcance", "Toda la plataforma",
            "Filtro", filtro(informe.tipo()),
            "Referencia", referencia(informe),
            "Generado", FormatoPdf.fechaHora(generado(informe.generadoEn())));

        resumen(documento, informe);
        comparacion(documento, informe);
        porTipo(documento, informe);
        porVendedor(documento, informe);
        porItem(documento, informe);

        return documento.cerrar();
    }

    private void resumen(DocumentoPdf documento, InformeAdminDTO informe) {
        InformeAdminTotalesDTO totales = informe.totales();
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
                FormatoPdf.monto(totales.comision()),
                "La plataforma cobró",
                tasaComision(totales) + " de las ventas", true));
        documento.resumen(
            new String[] { "Ingresó por ventas", "Comisión Temipora",
                "Neto para los artistas", "Costo de MercadoPago",
                "Pagado a los artistas" },
            new String[] { FormatoPdf.monto(totales.volumen()),
                FormatoPdf.resta(totales.comision()),
                FormatoPdf.monto(totales.netoArtistas()),
                FormatoPdf.resta(descuentoMedioPago(totales)),
                FormatoPdf.monto(totales.netoPercibido()) },
            4);
    }

    private static String tasaComision(InformeAdminTotalesDTO totales) {
        BigDecimal volumen = nulo(totales.volumen());
        if (volumen.signum() == 0) {
            return FormatoPdf.porcentaje(null);
        }
        BigDecimal tasa = nulo(totales.comision())
            .multiply(BigDecimal.valueOf(100))
            .divide(volumen, 2, RoundingMode.HALF_UP);
        return FormatoPdf.porcentaje(tasa);
    }

    private static BigDecimal descuentoMedioPago(
            InformeAdminTotalesDTO totales) {
        BigDecimal neto = totales.netoArtistas() != null
            ? totales.netoArtistas() : BigDecimal.ZERO;
        BigDecimal liquidado = totales.netoPercibido() != null
            ? totales.netoPercibido() : BigDecimal.ZERO;
        return neto.subtract(liquidado);
    }

    private void comparacion(
            DocumentoPdf documento, InformeAdminDTO informe) {
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
        tabla.fila("Volumen procesado",
            FormatoPdf.monto(anterior.volumen()),
            FormatoPdf.monto(informe.totales().volumen()),
            variacion(anterior.variacionVolumenPorcentaje()));
        tabla.fila("Ingresos de la plataforma",
            FormatoPdf.monto(anterior.comision()),
            FormatoPdf.monto(informe.totales().comision()),
            variacion(anterior.variacionComisionPorcentaje()));
        documento.tabla(tabla);
    }

    private void porTipo(DocumentoPdf documento, InformeAdminDTO informe) {
        documento.seccion("3", "Ventas por tipo de ítem");
        List<InformeAdminTipoDTO> tipos = informe.tipos() != null
            ? informe.tipos() : List.of();
        if (tipos.isEmpty()) {
            documento.parrafo("No hay ventas registradas en el período.");
            return;
        }

        TablaPdf tabla = new TablaPdf(
            new String[] { "Tipo", "Oper.", "Unid.", "Volumen", "Comisión",
                "Neto a artistas" },
            new float[] { 1.5f, 0.8f, 0.8f, 1.5f, 1.45f, 1.5f },
            new boolean[] { false, true, true, true, true, true });

        long operaciones = 0;
        long unidades = 0;
        BigDecimal volumen = BigDecimal.ZERO;
        BigDecimal comision = BigDecimal.ZERO;
        BigDecimal neto = BigDecimal.ZERO;

        for (InformeAdminTipoDTO tipo : tipos) {
            tabla.fila(tipo(tipo.tipo()),
                String.valueOf(tipo.operaciones()),
                String.valueOf(tipo.unidades()),
                FormatoPdf.monto(tipo.volumen()),
                FormatoPdf.monto(tipo.comision()),
                FormatoPdf.monto(tipo.netoArtistas()));
            operaciones += tipo.operaciones();
            unidades += tipo.unidades();
            volumen = volumen.add(nulo(tipo.volumen()));
            comision = comision.add(nulo(tipo.comision()));
            neto = neto.add(nulo(tipo.netoArtistas()));
        }
        tabla.total("Total", String.valueOf(operaciones),
            String.valueOf(unidades), FormatoPdf.monto(volumen),
            FormatoPdf.monto(comision), FormatoPdf.monto(neto));
        documento.tabla(tabla);
    }

    private void porVendedor(DocumentoPdf documento, InformeAdminDTO informe) {
        documento.seccion("4", "Ventas por vendedor y artista");
        List<InformeAdminVendedorDTO> vendedores = informe.vendedores() != null
            ? informe.vendedores() : List.of();
        if (vendedores.isEmpty()) {
            documento.parrafo(
                "No hay vendedores con ventas en el período.");
            return;
        }

        TablaPdf tabla = new TablaPdf(
            new String[] { "Vendedor / artista", "Oper.", "Unid.", "Volumen",
                "Comisión", "Neto a artistas" },
            new float[] { 2.5f, 0.75f, 0.75f, 1.4f, 1.35f, 1.45f },
            new boolean[] { false, true, true, true, true, true });

        long operaciones = 0;
        long unidades = 0;
        BigDecimal volumen = BigDecimal.ZERO;
        BigDecimal comision = BigDecimal.ZERO;
        BigDecimal neto = BigDecimal.ZERO;

        for (InformeAdminVendedorDTO vendedor : vendedores) {
            tabla.fila(FormatoPdf.texto(vendedor.nombre()),
                String.valueOf(vendedor.operaciones()),
                String.valueOf(vendedor.unidades()),
                FormatoPdf.monto(vendedor.volumen()),
                FormatoPdf.monto(vendedor.comision()),
                FormatoPdf.monto(vendedor.netoArtistas()));
            operaciones += vendedor.operaciones();
            unidades += vendedor.unidades();
            volumen = volumen.add(nulo(vendedor.volumen()));
            comision = comision.add(nulo(vendedor.comision()));
            neto = neto.add(nulo(vendedor.netoArtistas()));
        }
        tabla.total("Total", String.valueOf(operaciones),
            String.valueOf(unidades), FormatoPdf.monto(volumen),
            FormatoPdf.monto(comision), FormatoPdf.monto(neto));
        documento.tabla(tabla);
    }

    private void porItem(DocumentoPdf documento, InformeAdminDTO informe) {
        documento.seccion("5", "Ventas por ítem del catálogo");
        List<InformeAdminItemDTO> items = informe.items() != null
            ? informe.items() : List.of();
        if (items.isEmpty()) {
            documento.parrafo("No hay ítems vendidos en el período.");
            return;
        }

        TablaPdf tabla = new TablaPdf(
            new String[] { "Tipo", "Ítem", "Oper.", "Unid.", "Volumen",
                "Comisión", "Neto a artistas" },
            new float[] { 0.95f, 2.45f, 0.7f, 0.7f, 1.3f, 1.25f, 1.3f },
            new boolean[] { false, false, true, true, true, true, true });

        for (InformeAdminItemDTO item : items) {
            tabla.fila(tipo(item.tipo()),
                FormatoPdf.texto(item.nombre()),
                String.valueOf(item.operaciones()),
                String.valueOf(item.unidades()),
                FormatoPdf.monto(item.volumen()),
                FormatoPdf.monto(item.comision()),
                FormatoPdf.monto(item.netoArtistas()));
        }
        documento.tabla(tabla);
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
            return "sin ventas en el período anterior";
        }
        String signo = valor.signum() >= 0 ? "+" : "";
        return signo + FormatoPdf.numero(valor) + "%";
    }

    private static BigDecimal nulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
