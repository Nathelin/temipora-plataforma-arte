package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.temipora.backend_springboot.modelo.AlcanceSancion;

@Service
public class EmailPlantillaService {

    public static final String ASUNTO_REACTIVACION =
        "Ofertas para retomar tus compras en Temipora";

    private static final String PLANTILLA_REACTIVACION = "emails/reactivacion";
    private static final String PLANTILLA_CIERRE = "emails/cierre";
    private static final String PLANTILLA_RECORDATORIO = "emails/recordatorio";
    private static final String PLANTILLA_BAJA_CONFIRMADA = "emails/baja-confirmada";
    private static final String PLANTILLA_BAJA_INVALIDA = "emails/baja-invalida";
    private static final String PLANTILLA_SANCION = "emails/sancion";
    private static final String PREHEADER_REACTIVACION =
        "Tenés ofertas con descuento esperándote en Temipora.";
    private static final String PREHEADER_CIERRE =
        "Novedades sobre tus solicitudes y pedidos en Temipora.";
    private static final String PREHEADER_RECORDATORIO =
        "Tenés pagos o entregas pendientes en Temipora.";
    private static final String PREHEADER_SANCION =
        "Hay una restricción activa en tu cuenta de Temipora.";

    private static final DateTimeFormatter FECHA_CIERRE =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record CierreEmail(
            String contexto,
            Long idSolicitud,
            Long idPedido,
            String nombreServicio,
            String nombreArtista,
            String imagen,
            String titulo,
            String detalle,
            LocalDateTime fecha) {
    }

    public record AlternativaEmail(
            Long idServicio,
            Long idSustitucion,
            String nombreServicio,
            String nombreArtista,
            String imagen,
            String precio,
            String moneda,
            String motivo,
            List<String> caracteristicas) {
    }

    public record SancionEmail(
            AlcanceSancion alcance,
            Long idPedido,
            String nombreServicio,
            String nombreCliente,
            String motivo,
            LocalDateTime fechaFin) {
    }

    public record RecordatorioEmail(
            String contexto,
            Long idPedido,
            String nombreServicio,
            String imagen,
            String contraparte,
            String detalle,
            String vence,
            BigDecimal monto,
            String moneda,
            boolean urgente,
            boolean alerta,
            String consecuencia) {

        public boolean esPago() {
            return "PAGO".equals(contexto);
        }
    }

    private final SpringTemplateEngine templateEngine;

    @Value("${temipora.mail.api-url:http://localhost:8080}")
    private String apiUrl;

    @Value("${temipora.mail.base-url:http://localhost:5173}")
    private String frontUrl;

    @Value("${temipora.contacto.email:}")
    private String contactoEmail;

    public EmailPlantillaService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String reactivacion(
            String nombre,
            List<OfertaEmail> ofertas,
            String tokenBaja) {
        Context contexto = contexto();
        contexto.setVariable("nombre", nombre);
        contexto.setVariable("preheader", PREHEADER_REACTIVACION);
        contexto.setVariable("urlBaja", baseApi() + "/api/emails/baja/" + url(tokenBaja));
        contexto.setVariable("ofertas", vistaOfertas(ofertas));
        return templateEngine.process(PLANTILLA_REACTIVACION, contexto);
    }

    public String asuntoCierre(List<CierreEmail> cierres) {
        if (cierres == null || cierres.isEmpty()) {
            return "Novedades sobre tus pedidos en Temipora";
        }
        if (cierres.size() == 1) {
            CierreEmail cierre = cierres.get(0);
            String entidad = "PEDIDO".equals(cierre.contexto())
                ? "pedido"
                : "solicitud";
            return "Se cerró tu " + entidad + " de «"
                + cierre.nombreServicio() + "»";
        }
        return "Se cerraron " + cierres.size()
            + " de tus solicitudes y pedidos";
    }

    public String cierre(
            String nombre,
            List<CierreEmail> cierres,
            List<AlternativaEmail> alternativas,
            String tokenAcceso) {
        Context contexto = contexto();
        contexto.setVariable("nombre", nombre);
        contexto.setVariable("preheader", PREHEADER_CIERRE);
        contexto.setVariable("cierres", vistaCierres(cierres, tokenAcceso));
        contexto.setVariable("alternativas",
            vistaAlternativas(alternativas, tokenAcceso));
        contexto.setVariable("urlExplorar", baseFront() + "/servicios");
        return templateEngine.process(PLANTILLA_CIERRE, contexto);
    }

    public String asuntoRecordatorio(
            boolean hayPagos, boolean hayEntregas, boolean pagoCuentaEnRiesgo,
            boolean alerta, boolean urgente) {
        boolean riesgo = alerta || urgente;
        if (hayPagos && hayEntregas) {
            if (urgente) {
                return "URGENTE: pagos y entregas pendientes — tu cuenta en riesgo";
            }
            return riesgo
                ? "Tu cuenta en riesgo: pagos y entregas pendientes en Temipora"
                : "Tenés pagos y entregas pendientes en Temipora";
        }
        if (hayPagos) {
            if (pagoCuentaEnRiesgo && riesgo) {
                return urgente
                    ? "URGENTE: pagá hoy o tu cuenta queda limitada"
                    : "Tu cuenta puede limitarse: pago de entrega final pendiente";
            }
            if (urgente) {
                return "URGENTE: pagá hoy o se cierra tu pedido";
            }
            return "Pago pendiente: evitá que se cierre tu pedido";
        }
        if (hayEntregas) {
            if (urgente) {
                return "URGENTE: entregá hoy o tu cuenta queda limitada";
            }
            return riesgo
                ? "Tu cuenta puede quedar limitada: entrega pendiente en Temipora"
                : "Entrega pendiente: evitá que se limite tu cuenta";
        }
        return "Tenés pendientes en Temipora";
    }

    public String asuntoSancion(AlcanceSancion alcance) {
        if (alcance == AlcanceSancion.ENTREGAS) {
            return "Tu cuenta quedó limitada a entregas en Temipora";
        }
        if (alcance == AlcanceSancion.ACCESO) {
            return "Tu cuenta quedó inhabilitada en Temipora";
        }
        if (alcance == AlcanceSancion.VENTAS) {
            return "Tus ventas quedaron suspendidas en Temipora";
        }
        return "Tu cuenta tiene una restricción en Temipora";
    }

    public String sancion(String nombre, SancionEmail datos) {
        return sancion(nombre, datos, null);
    }

    public String sancion(
            String nombre, SancionEmail datos, String tokenAcceso) {
        boolean entregas = datos != null
            && datos.alcance() == AlcanceSancion.ENTREGAS;
        boolean bloqueo = datos != null
            && datos.alcance() == AlcanceSancion.ACCESO;
        Context contexto = contexto();
        contexto.setVariable("nombre", nombre);
        contexto.setVariable("preheader", PREHEADER_SANCION);
        contexto.setVariable("entregas", entregas);
        contexto.setVariable("etiqueta", entregas
            ? "CUENTA LIMITADA"
            : bloqueo ? "CUENTA INHABILITADA" : "VENTAS SUSPENDIDAS");
        contexto.setVariable("titulo", entregas
            ? "Tu cuenta quedó limitada a entregas"
            : bloqueo
                ? "Tu cuenta quedó inhabilitada"
                : "Tus ventas quedaron suspendidas");
        contexto.setVariable("motivo", datos != null ? datos.motivo() : null);
        contexto.setVariable("idPedido", datos != null ? datos.idPedido() : null);
        contexto.setVariable("nombreServicio", datos != null
            ? datos.nombreServicio() : null);
        contexto.setVariable("nombreCliente", datos != null
            ? datos.nombreCliente() : null);
        contexto.setVariable("fechaFin", datos != null
                && datos.fechaFin() != null
            ? FECHA_CIERRE.format(datos.fechaFin())
            : null);
        contexto.setVariable("consecuencia", entregas
            ? "Solo podés subir los entregables pendientes, ver tus"
                + " notificaciones y apelar la sanción. El resto de la plataforma"
                + " queda bloqueado hasta que el cliente apruebe la entrega o un"
                + " administrador revise tu caso."
            : bloqueo
                ? "No podés iniciar sesión ni usar la plataforma. Para apelar la"
                    + " sanción, escribí a soporte."
                : "Podés terminar los pedidos que ya tenés abiertos, pero no vas a"
                    + " poder recibir encargos nuevos ni publicar productos mientras"
                    + " dure la suspensión. Si no resolvés la entrega, la cuenta pasa"
                    + " a estar limitada solo a entregas.");
        contexto.setVariable("url", urlSancion(datos, tokenAcceso));
        contexto.setVariable("etiquetaBoton", entregas
            ? "Subir entrega"
            : bloqueo ? "Escribir a soporte" : "Ver mis pedidos");
        contexto.setVariable("contacto", contactoEmail);
        return templateEngine.process(PLANTILLA_SANCION, contexto);
    }

    public String recordatorio(
            String nombre,
            List<RecordatorioEmail> pagos,
            List<RecordatorioEmail> entregas,
            String tokenAcceso) {
        Context contexto = contexto();
        contexto.setVariable("nombre", nombre);
        contexto.setVariable("preheader", PREHEADER_RECORDATORIO);
        contexto.setVariable("pagos", vistaRecordatorios(pagos, tokenAcceso));
        contexto.setVariable("entregas", vistaRecordatorios(entregas, tokenAcceso));
        contexto.setVariable("urgente", hayUrgente(pagos, entregas));
        contexto.setVariable("alerta", hayAlerta(pagos, entregas));
        contexto.setVariable("urlPanel",
            baseFront() + "/panel/pedidos" + enlaceAcceso(tokenAcceso));
        return templateEngine.process(PLANTILLA_RECORDATORIO, contexto);
    }

    public String bajaConfirmada(String nombre) {
        Context contexto = contexto();
        contexto.setVariable("nombre", nombre);
        return templateEngine.process(PLANTILLA_BAJA_CONFIRMADA, contexto);
    }

    public String bajaInvalida() {
        return templateEngine.process(PLANTILLA_BAJA_INVALIDA, contexto());
    }

    private Context contexto() {
        return new Context(Locale.forLanguageTag("es-AR"));
    }

    private List<Map<String, Object>> vistaRecordatorios(
            List<RecordatorioEmail> items, String tokenAcceso) {
        List<Map<String, Object>> vistas = new ArrayList<>();
        if (items == null) {
            return vistas;
        }
        for (RecordatorioEmail item : items) {
            vistas.add(vistaRecordatorio(item, tokenAcceso));
        }
        return vistas;
    }

    private Map<String, Object> vistaRecordatorio(
            RecordatorioEmail item, String tokenAcceso) {
        Map<String, Object> vista = new LinkedHashMap<>();
        vista.put("imagenUrl", urlImagenSeguro(item.imagen()));
        vista.put("inicial", inicial(item.nombreServicio()));
        vista.put("nombreServicio", item.nombreServicio());
        vista.put("contraparte", item.contraparte());
        vista.put("detalle", item.detalle());
        vista.put("vence", item.vence());
        vista.put("monto", item.monto() != null
            ? formatearMonto(item.monto())
            : null);
        vista.put("moneda", item.moneda());
        vista.put("urgente", item.urgente());
        vista.put("alerta", item.alerta());
        vista.put("consecuencia", item.consecuencia());
        vista.put("etiqueta", item.esPago() ? "Pagar" : "Subir entrega");
        vista.put("eyebrow", item.esPago()
            ? "Pago pendiente"
            : "Entrega pendiente");
        vista.put("url", urlRecordatorio(item, tokenAcceso));
        return vista;
    }

    private String urlRecordatorio(
            RecordatorioEmail item, String tokenAcceso) {
        String path;
        if (item.idPedido() == null) {
            path = "/servicios";
        } else if (item.esPago()) {
            path = "/pedidos/" + item.idPedido();
        } else {
            path = "/panel/pedidos/" + item.idPedido();
        }
        return baseFront() + path + enlaceAcceso(tokenAcceso);
    }

    private String urlSancion(SancionEmail datos, String tokenAcceso) {
        if (datos != null && datos.alcance() == AlcanceSancion.ACCESO) {
            return contactoEmail != null && !contactoEmail.isBlank()
                ? "mailto:" + contactoEmail
                : baseFront() + "/contacto";
        }
        if (datos != null && datos.alcance() == AlcanceSancion.ENTREGAS) {
            return baseFront() + "/cuenta/restringida"
                + enlaceAcceso(tokenAcceso);
        }
        if (datos != null && datos.idPedido() != null) {
            return baseFront() + "/panel/pedidos/" + datos.idPedido()
                + enlaceAcceso(tokenAcceso);
        }
        return baseFront() + "/panel/pedidos" + enlaceAcceso(tokenAcceso);
    }

    private boolean hayAlerta(
            List<RecordatorioEmail> pagos,
            List<RecordatorioEmail> entregas) {
        for (List<RecordatorioEmail> lista : List.of(
                pagos != null ? pagos : List.<RecordatorioEmail>of(),
                entregas != null ? entregas : List.<RecordatorioEmail>of())) {
            for (RecordatorioEmail item : lista) {
                if (item.alerta()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hayUrgente(
            List<RecordatorioEmail> pagos,
            List<RecordatorioEmail> entregas) {
        if (pagos != null) {
            for (RecordatorioEmail pago : pagos) {
                if (pago.urgente()) {
                    return true;
                }
            }
        }
        if (entregas != null) {
            for (RecordatorioEmail entrega : entregas) {
                if (entrega.urgente()) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Map<String, Object>> vistaCierres(
            List<CierreEmail> cierres, String tokenAcceso) {
        List<Map<String, Object>> vistas = new ArrayList<>();
        if (cierres == null) {
            return vistas;
        }
        for (CierreEmail cierre : cierres) {
            vistas.add(vistaCierre(cierre, tokenAcceso));
        }
        return vistas;
    }

    private Map<String, Object> vistaCierre(
            CierreEmail cierre, String tokenAcceso) {
        Map<String, Object> vista = new LinkedHashMap<>();
        vista.put("imagenUrl", urlImagenSeguro(cierre.imagen()));
        vista.put("inicial", inicial(cierre.nombreServicio()));
        vista.put("nombreServicio", cierre.nombreServicio());
        vista.put("nombreArtista", cierre.nombreArtista());
        vista.put("titulo", cierre.titulo());
        vista.put("detalle", cierre.detalle());
        vista.put("fecha", cierre.fecha() != null
            ? FECHA_CIERRE.format(cierre.fecha())
            : null);
        vista.put("url", urlCierre(cierre, tokenAcceso));
        vista.put("etiqueta", "PEDIDO".equals(cierre.contexto())
            ? "Ver pedido"
            : "Ver solicitud");
        return vista;
    }

    private List<Map<String, Object>> vistaAlternativas(
            List<AlternativaEmail> alternativas,
            String tokenAcceso) {
        List<Map<String, Object>> vistas = new ArrayList<>();
        if (alternativas == null) {
            return vistas;
        }
        for (AlternativaEmail alternativa : alternativas) {
            Map<String, Object> vista = new LinkedHashMap<>();
            vista.put("imagenUrl", urlImagenSeguro(alternativa.imagen()));
            vista.put("inicial", inicial(alternativa.nombreServicio()));
            vista.put("nombreServicio", alternativa.nombreServicio());
            vista.put("nombreArtista", alternativa.nombreArtista());
            vista.put("precio", monto(alternativa.precio()));
            vista.put("moneda", alternativa.moneda());
            vista.put("motivo", alternativa.motivo());
            vista.put("caracteristicas", alternativa.caracteristicas());
            vista.put("url", urlAlternativa(alternativa, tokenAcceso));
            vistas.add(vista);
        }
        return vistas;
    }

    private String urlAlternativa(
            AlternativaEmail alternativa, String tokenAcceso) {
        if (alternativa.idServicio() == null) {
            return baseFront() + "/servicios" + enlaceAcceso(tokenAcceso);
        }
        List<String> parametros = new ArrayList<>();
        if (tokenAcceso != null && !tokenAcceso.isBlank()) {
            parametros.add("acceso=" + tokenAcceso);
        }
        if (alternativa.idSustitucion() != null) {
            parametros.add("sustitucion=" + alternativa.idSustitucion());
        }
        String query = parametros.isEmpty()
            ? ""
            : "?" + String.join("&", parametros);
        return baseFront() + "/servicios/" + alternativa.idServicio() + query;
    }

    private String urlCierre(CierreEmail cierre, String tokenAcceso) {
        String path;
        if ("PEDIDO".equals(cierre.contexto()) && cierre.idPedido() != null) {
            path = "/pedidos/" + cierre.idPedido() + "/resultado";
        } else if (cierre.idSolicitud() != null) {
            path = "/solicitudes/" + cierre.idSolicitud() + "/resultado";
        } else {
            path = "/servicios";
        }
        return baseFront() + path
            + enlaceAcceso(tokenAcceso);
    }

    private String enlaceAcceso(String tokenAcceso) {
        return tokenAcceso == null || tokenAcceso.isBlank()
            ? ""
            : "?acceso=" + tokenAcceso;
    }

    private String urlImagenSeguro(String imagen) {
        if (imagen == null || imagen.isBlank()) {
            return null;
        }
        return urlImagen(imagen);
    }

    private String baseFront() {
        return frontUrl == null ? "" : frontUrl.replaceAll("/+$", "");
    }

    private List<Map<String, Object>> vistaOfertas(List<OfertaEmail> ofertas) {
        List<Map<String, Object>> vistas = new ArrayList<>();
        if (ofertas == null) {
            return vistas;
        }
        for (OfertaEmail oferta : ofertas) {
            vistas.add(vistaOferta(oferta));
        }
        return vistas;
    }

    private Map<String, Object> vistaOferta(OfertaEmail oferta) {
        Map<String, Object> vista = new LinkedHashMap<>();
        vista.put("imagenUrl", imagen(oferta));
        vista.put("inicial", inicial(oferta.nombreServicio()));
        vista.put("nombreServicio", oferta.nombreServicio());
        vista.put("nombreArtista", oferta.nombreArtista());
        vista.put("porcentaje", porcentaje(oferta));
        vista.put("precioDesde", monto(oferta.precioDesde()));
        vista.put("precioConDescuento", precioConDescuento(oferta));
        vista.put("moneda", oferta.moneda());
        vista.put("motivo", oferta.motivo());
        vista.put("url", oferta.url());
        return vista;
    }

    private String imagen(OfertaEmail oferta) {
        if (oferta.imagen() == null || oferta.imagen().isBlank()) {
            return null;
        }
        return urlImagen(oferta.imagen());
    }

    private String porcentaje(OfertaEmail oferta) {
        if (oferta.porcentajeDescuento() == null) {
            return "0";
        }
        return oferta.porcentajeDescuento()
            .stripTrailingZeros()
            .toPlainString();
    }

    private String precioConDescuento(OfertaEmail oferta) {
        if (oferta.precioDesde() == null || oferta.precioDesde().isBlank()
                || oferta.porcentajeDescuento() == null) {
            return null;
        }
        try {
            BigDecimal base = new BigDecimal(oferta.precioDesde().trim());
            BigDecimal porcentaje = oferta.porcentajeDescuento()
                .max(BigDecimal.ZERO)
                .min(BigDecimal.valueOf(100));
            BigDecimal descuento = base.multiply(porcentaje)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return formatearMonto(base.subtract(descuento).max(BigDecimal.ZERO));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String monto(String valor) {
        if (valor == null || valor.isBlank()) {
            return valor;
        }
        try {
            return formatearMonto(new BigDecimal(valor.trim()));
        } catch (NumberFormatException e) {
            return valor;
        }
    }

    private String formatearMonto(BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        NumberFormat formato = NumberFormat.getNumberInstance(
            Locale.forLanguageTag("es-AR"));
        formato.setMinimumFractionDigits(0);
        formato.setMaximumFractionDigits(2);
        return formato.format(valor);
    }

    private String inicial(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "?";
        }
        return nombre.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private String urlImagen(String imagen) {
        if (imagen.startsWith("http://") || imagen.startsWith("https://")) {
            return imagen;
        }
        return baseApi() + (imagen.startsWith("/") ? imagen : "/" + imagen);
    }

    private String baseApi() {
        return apiUrl == null ? "" : apiUrl.replaceAll("/+$", "");
    }

    private String url(String valor) {
        return valor == null ? "" : valor;
    }
}
