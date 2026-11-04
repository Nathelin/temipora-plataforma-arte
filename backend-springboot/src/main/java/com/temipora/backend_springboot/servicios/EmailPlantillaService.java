package com.temipora.backend_springboot.servicios;

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
    private static final String PLANTILLA_BAJA_CONFIRMADA = "emails/baja-confirmada";
    private static final String PLANTILLA_BAJA_INVALIDA = "emails/baja-invalida";
    private static final String PREHEADER_REACTIVACION =
        "Tenés ofertas con descuento esperándote en Temipora.";

    private final SpringTemplateEngine templateEngine;

    @Value("${temipora.mail.api-url:http://localhost:8080}")
    private String apiUrl;

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
        vista.put("precioDesde", oferta.precioDesde());
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
        return apiUrl == null ? "" : apiUrl;
    }

    private String url(String valor) {
        return valor == null ? "" : valor;
    }
}
