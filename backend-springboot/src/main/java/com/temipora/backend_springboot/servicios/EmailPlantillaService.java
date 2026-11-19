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

@Service
public class EmailPlantillaService {

    public static final String ASUNTO_REACTIVACION =
        "Ofertas para retomar tus compras en Temipora";

    private static final String PLANTILLA_REACTIVACION = "emails/reactivacion";
    private static final String PLANTILLA_CIERRE = "emails/cierre";
    private static final String PLANTILLA_BAJA_CONFIRMADA = "emails/baja-confirmada";
    private static final String PLANTILLA_BAJA_INVALIDA = "emails/baja-invalida";
    private static final String PREHEADER_REACTIVACION =
        "Tenés ofertas con descuento esperándote en Temipora.";
    private static final String PREHEADER_CIERRE =
        "Novedades sobre tus solicitudes y pedidos en Temipora.";

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
            String moneda) {
    }

    private final SpringTemplateEngine templateEngine;

    @Value("${temipora.mail.api-url:http://localhost:8080}")
    private String apiUrl;

    @Value("${temipora.mail.base-url:http://localhost:5173}")
    private String frontUrl;

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
