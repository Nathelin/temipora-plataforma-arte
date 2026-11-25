package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.modelo.DescuentoPedidoDTO;
import com.temipora.backend_springboot.modelo.InformeDescuentosDTO;
import com.temipora.backend_springboot.modelo.OfertasReactivacionResumenDTO;
import com.temipora.backend_springboot.modelo.ResumenDescuentosDTO;
import com.temipora.backend_springboot.servicios.informes.DocumentoPdf;
import com.temipora.backend_springboot.servicios.informes.EmisorPdf;
import com.temipora.backend_springboot.servicios.informes.FormatoPdf;
import com.temipora.backend_springboot.servicios.informes.TablaPdf;

@Service
public class InformeDescuentosPdfService {

    private static final String TIPO = "Descuentos de promociones";
    private static final String PIE =
        "Documento interno de administración · generado automáticamente por "
            + "Temipora";

    public byte[] generar(InformeDescuentosDTO informe, EmisorPdf emisor) {
        String desde = FormatoPdf.fecha(informe.desde());
        String hasta = FormatoPdf.fecha(informe.hasta());

        DocumentoPdf documento = DocumentoPdf.vertical(
            TIPO, desde + " al " + hasta, referencia(informe), PIE, emisor);
        documento.abrir();
        documento.cabecera("Promociones por inactividad",
            "Descuentos aplicados en pedidos",
            desde + " al " + hasta + " · " + informe.dias()
                + " días · Cifras en " + FormatoPdf.moneda(informe.moneda()));
        documento.metadatos(
            "Alcance", "Pedidos con descuento",
            "Filtro", informe.filtro(),
            "Referencia", referencia(informe),
            "Generado", FormatoPdf.fechaHora(informe.generadoEn()));

        resumen(documento, informe.resumen());
        ofertas(documento, informe.ofertas());
        pedidos(documento, informe);

        return documento.cerrar();
    }

    private void resumen(
            DocumentoPdf documento, ResumenDescuentosDTO resumen) {
        documento.seccion("1", "Resumen del período");
        documento.indicadores(
            new DocumentoPdf.Metrica(
                String.valueOf(resumen.pedidos()), "Pedidos con descuento",
                resumen.totalPedidosPeriodo() > 0
                    ? FormatoPdf.porcentaje(resumen.porcentajePedidos())
                        + " de los pedidos del período"
                    : null,
                true),
            new DocumentoPdf.Metrica(
                FormatoPdf.monto(resumen.descuentoTotal()),
                "Descuento financiado", "Cifras en ARS", true),
            new DocumentoPdf.Metrica(
                FormatoPdf.monto(resumen.ticketPromedio()),
                "Ticket promedio", null, false));
            /*new DocumentoPdf.Metrica(
                String.valueOf(resumen.pedidosConComisionCero()),
                "Pedidos con comisión 0",
                FormatoPdf.monto(resumen.descuentoConComisionCero())
                    + " de descuento",
                false));*/

        documento.resumen(
            new String[] { "Volumen con descuento", "Volumen sin descuento",
                "Comisión nominal", "Comisión efectiva" },
            new String[] { FormatoPdf.monto(resumen.volumenCliente()),
                FormatoPdf.monto(resumen.volumenSinDescuento()),
                FormatoPdf.monto(resumen.comisionNominal()),
                FormatoPdf.monto(resumen.comisionEfectiva()) },
            -1);
    }

    private void ofertas(
            DocumentoPdf documento, OfertasReactivacionResumenDTO ofertas) {
        documento.seccion("2",
            "Ofertas de reactivación generadas en el período");
        documento.indicadores(
            new DocumentoPdf.Metrica(
                String.valueOf(ofertas.campanas()), "Campañas", null, false),
            new DocumentoPdf.Metrica(
                String.valueOf(ofertas.ofertas()), "Ofertas", null, false),
            new DocumentoPdf.Metrica(
                String.valueOf(ofertas.aceptadas()), "Aceptadas",
                "Solicitud iniciada; incluye las usadas", false),
            new DocumentoPdf.Metrica(
                String.valueOf(ofertas.utilizadas()), "Usadas",
                FormatoPdf.porcentaje(ofertas.tasaUtilizacion())
                    + " de las ofertas",
                true));

        if (ofertas.ofertas() == 0) {
            documento.parrafo(
                "No se generaron ofertas de reactivación en el período.");
            return;
        }
        documento.parrafoChico(
            "Estados: " + ofertas.ofrecidas() + " ofrecidas · "
                + ofertas.aceptadas() + " aceptadas · "
                + ofertas.utilizadas() + " utilizadas · "
                + ofertas.vencidas() + " vencidas · "
                + ofertas.canceladas() + " canceladas.");
        documento.parrafoChico(
            "Cada pedido se cuenta una sola vez. Las ofertas se resumen "
                + "aparte para no duplicar la misma promoción en varias filas.");
    }

    private void pedidos(
            DocumentoPdf documento, InformeDescuentosDTO informe) {
        documento.seccion("3", "Pedidos con descuento", "Detalle");
        List<DescuentoPedidoDTO> filas = informe.pedidos() != null
            ? informe.pedidos() : List.of();
        if (filas.isEmpty()) {
            documento.parrafo(
                "No hay pedidos con descuento para los filtros elegidos.");
            return;
        }

        TablaPdf tabla = new TablaPdf(
            new String[] { "Fecha", "Pedido", "Cliente", "Artista",
                "Servicio", "Dto. %", "Dto. $", "Comisión", "Estado" },
            new float[] { 0.85f, 0.55f, 1.15f, 1.0f, 1.35f, 0.55f, 0.9f,
                0.85f, 0.9f },
            new boolean[] { false, true, false, false, false, true, true,
                true, false });

        BigDecimal descuento = BigDecimal.ZERO;
        boolean hayComisionCero = false;
        for (DescuentoPedidoDTO fila : filas) {
            hayComisionCero = hayComisionCero || fila.comisionCero();
            tabla.fila(
                fecha(fila.fechaPedido()),
                "#" + fila.idPedido(),
                FormatoPdf.texto(fila.clienteUsuario()),
                FormatoPdf.texto(fila.artistaUsuario()),
                recortar(fila.servicioNombre()),
                FormatoPdf.porcentaje(fila.descuentoPorcentaje()),
                FormatoPdf.monto(fila.descuentoMontoCobro()),
                FormatoPdf.porcentaje(fila.comisionPorcentaje())
                    + (fila.comisionCero() ? " *" : ""),
                estado(fila.estado()));
            descuento = descuento.add(nulo(fila.descuentoMontoCobro()));
        }

        tabla.total("Total", String.valueOf(filas.size()), "", "", "", "",
            FormatoPdf.monto(descuento), "", "");
        documento.tabla(tabla);

        if (hayComisionCero) {
            documento.parrafoChico(
                "* La comisión congelada al emitir el presupuesto fue 0%.");
        }
        if (informe.totalElementos() > filas.size()) {
            documento.parrafoChico(
                "Se listan los primeros " + filas.size() + " de "
                    + informe.totalElementos()
                    + " pedidos. Acotá el período o los filtros para ver "
                    + "el resto.");
        }
    }

    private static String fecha(LocalDateTime valor) {
        return valor == null ? "—" : FormatoPdf.fecha(valor.toLocalDate());
    }

    private static String recortar(String valor) {
        if (valor == null || valor.isBlank()) {
            return "—";
        }
        return valor.length() <= 38
            ? valor
            : valor.substring(0, 37) + "…";
    }

    private static String estado(String valor) {
        if (valor == null) {
            return "—";
        }
        return switch (valor) {
            case "PENDIENTE_PAGO" -> "Pendiente de pago";
            case "EN_CURSO" -> "En curso";
            case "FINALIZADO" -> "Finalizado";
            case "CANCELADO" -> "Cancelado";
            case "IMPAGO" -> "Impago";
            case "INCUMPLIMIENTO" -> "Incumplimiento";
            default -> valor;
        };
    }

    private static BigDecimal nulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static String referencia(InformeDescuentosDTO informe) {
        return "PROM-DTO-"
            + FormatoPdf.fechaCompacta(informe.desde()) + "-"
            + FormatoPdf.fechaCompacta(informe.hasta());
    }
}
