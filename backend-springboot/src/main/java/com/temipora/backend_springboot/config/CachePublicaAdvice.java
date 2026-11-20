package com.temipora.backend_springboot.config;

import java.util.Set;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Permite cachear en el navegador los catálogos de referencia (tipos, medios,
 * etiquetas, monedas, países, etc.). Son datos que cambian solo desde el panel
 * de administración, así que una ventana corta de 30 segundos alcanza para que
 * la navegación repetida no vuelva a pedirlos, sin dejar valores viejos por
 * mucho tiempo.
 *
 * <p>No toca listados, búsquedas ni datos de usuario: esas respuestas siguen
 * con {@code no-store} como las configura Spring Security.
 */
@ControllerAdvice
public class CachePublicaAdvice implements ResponseBodyAdvice<Object> {

    private static final String CABECERA =
        "private, max-age=30, stale-while-revalidate=120";

    private static final Set<String> RUTAS = Set.of(
        "/api/tipos-servicio",
        "/api/tipos-producto",
        "/api/tipos-arte",
        "/api/tipos-campo",
        "/api/medios",
        "/api/monedas",
        "/api/paises",
        "/api/materiales",
        "/api/derechos-termino",
        "/api/etiquetas/selector",
        "/api/etiquetas/categorias",
        "/api/criterios-resenia",
        "/api/contacto",
        "/api/terminos-plataforma"
    );

    @Override
    public boolean supports(MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {
        if (request.getMethod() != HttpMethod.GET) {
            return body;
        }
        if (!RUTAS.contains(request.getURI().getPath())) {
            return body;
        }
        if (response instanceof ServletServerHttpResponse servlet) {
            int estado = servlet.getServletResponse().getStatus();
            if (estado < 200 || estado >= 300) {
                return body;
            }
        }
        response.getHeaders().setCacheControl(CABECERA);
        return body;
    }
}
