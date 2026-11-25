package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.modelo.InformeSustitucionesHistorialDTO;
import com.temipora.backend_springboot.modelo.ResumenSustitucionesHistorialDTO;
import com.temipora.backend_springboot.modelo.SustitucionConteoDTO;
import com.temipora.backend_springboot.modelo.SustitucionHistorialDTO;
import com.temipora.backend_springboot.servicios.informes.DocumentoPdf;
import com.temipora.backend_springboot.servicios.informes.FormatoPdf;
import com.temipora.backend_springboot.servicios.informes.TablaPdf;

@Service
public class InformeSustitucionesPdfService {

    private static final String TIPO = "Historial de sustituciones";
    private static final String PIE =
        "Documento interno de administración · generado automáticamente por "
            + "Temipora";
    private static final String VISITANTE = "Visitante";

    public byte[] generar(InformeSustitucionesHistorialDTO informe) {
        String desde = FormatoPdf.fecha(informe.desde());
        String hasta = FormatoPdf.fecha(informe.hasta());

        DocumentoPdf documento = DocumentoPdf.vertical(
            TIPO, desde + " al " + hasta, referencia(informe), PIE);
        documento.abrir();
        documento.cabecera("Sustituciones de artistas",
            "Historial de eventos",
            desde + " al " + hasta + " · " + informe.dias() + " días");
        documento.metadatos(
            "Alcance", "Búsquedas de alternativas",
            "Filtro", informe.filtro(),
            "Referencia", referencia(informe),
            "Generado", FormatoPdf.fechaHora(informe.generadoEn()));

        resumen(documento, informe.resumen());
        motivos(documento, informe.resumen());
        eventos(documento, informe);

        return documento.cerrar();
    }

    private void resumen(
            DocumentoPdf documento, ResumenSustitucionesHistorialDTO resumen) {
        documento.seccion("1", "Resumen del período");
        documento.indicadores(
            new DocumentoPdf.Metrica(
                String.valueOf(resumen.total()), "Eventos", null, true),
            new DocumentoPdf.Metrica(
                String.valueOf(resumen.resueltasConAlternativa()),
                "Resueltas con otra artista",
                FormatoPdf.porcentaje(resumen.tasaSustitucion())
                    + " de las resueltas",
                true),
            new DocumentoPdf.Metrica(
                String.valueOf(resumen.resueltasConCola()),
                "Quedaron en lista de espera", null, false),
            new DocumentoPdf.Metrica(
                FormatoPdf.numero(resumen.opcionesPromedio()),
                "Opciones por evento", null, false));

        documento.resumen(
            new String[] { "Descartadas", "Abiertas sin resolver", "Expiradas",
                "Eventos anónimos" },
            new String[] { String.valueOf(resumen.descartadas()),
                String.valueOf(resumen.abiertas()),
                String.valueOf(resumen.expiradas()),
                String.valueOf(resumen.anonimas()) },
            -1);
    }

    private void motivos(
            DocumentoPdf documento, ResumenSustitucionesHistorialDTO resumen) {
        documento.seccion("2", "Motivos de la búsqueda");
        List<SustitucionConteoDTO> motivos = resumen.motivos() != null
            ? resumen.motivos() : List.of();
        if (motivos.isEmpty()) {
            documento.parrafo(
                "No hay eventos de sustitución en el período.");
            return;
        }

        TablaPdf tabla = new TablaPdf(
            new String[] { "Motivo", "Eventos", "Participación" },
            new float[] { 3.4f, 1.0f, 1.2f },
            new boolean[] { false, true, true });
        for (SustitucionConteoDTO motivo : motivos) {
            tabla.fila(
                FormatoPdf.texto(motivo.etiqueta()),
                String.valueOf(motivo.total()),
                porcentaje(motivo.total(), resumen.total()));
        }
        tabla.total("Total", String.valueOf(resumen.total()), "100%");
        documento.tabla(tabla);
    }

    private void eventos(
            DocumentoPdf documento, InformeSustitucionesHistorialDTO informe) {
        documento.seccion("3", "Eventos", "Detalle");
        List<SustitucionHistorialDTO> eventos = informe.eventos() != null
            ? informe.eventos() : List.of();
        if (eventos.isEmpty()) {
            documento.parrafo(
                "No hay eventos de sustitución para los filtros elegidos.");
            return;
        }

        TablaPdf tabla = new TablaPdf(
            new String[] { "Fecha", "Cliente", "Servicio origen",
                "Artista origen", "Motivo", "Opc.", "Pedido", "Resultado" },
            new float[] { 0.85f, 1.0f, 1.35f, 0.9f, 0.95f, 0.4f, 0.55f,
                2.1f },
            new boolean[] { false, false, false, false, false, true, true,
                false });

        for (SustitucionHistorialDTO evento : eventos) {
            tabla.fila(
                fecha(evento.fechaCreacion()),
                FormatoPdf.texto(evento.clienteUsuario() != null
                    ? evento.clienteUsuario() : VISITANTE),
                recortar(evento.servicioOrigen(), 30),
                FormatoPdf.texto(evento.artistaOrigen()),
                FormatoPdf.texto(evento.motivoEtiqueta()),
                String.valueOf(evento.cantidadOpciones()),
                evento.idPedido() != null ? "#" + evento.idPedido() : "—",
                recortar(evento.resultado(), 36));
        }
        tabla.total("Total", String.valueOf(eventos.size()), "", "", "", "",
            "", "");
        documento.tabla(tabla);

        if (informe.totalElementos() > eventos.size()) {
            documento.parrafoChico(
                "Se listan los primeros " + eventos.size() + " de "
                    + informe.totalElementos()
                    + " eventos. Acotá el período o los filtros para ver "
                    + "el resto.");
        }
    }

    private static String porcentaje(long parte, long total) {
        if (total == 0) {
            return "—";
        }
        return FormatoPdf.porcentaje(
            BigDecimal.valueOf(parte * 100.0 / total)
                .setScale(2, RoundingMode.HALF_UP));
    }

    private static String fecha(LocalDateTime valor) {
        return valor == null ? "—" : FormatoPdf.fecha(valor.toLocalDate());
    }

    private static String recortar(String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return "—";
        }
        return valor.length() <= maximo
            ? valor
            : valor.substring(0, maximo - 1) + "…";
    }

    private static String referencia(InformeSustitucionesHistorialDTO informe) {
        return "SUST-"
            + FormatoPdf.fechaCompacta(informe.desde()) + "-"
            + FormatoPdf.fechaCompacta(informe.hasta());
    }
}
