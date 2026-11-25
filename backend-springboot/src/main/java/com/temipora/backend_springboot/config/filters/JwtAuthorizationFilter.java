// NUEVO

package com.temipora.backend_springboot.config.filters;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.temipora.backend_springboot.config.AccesoUsuarioService;
import com.temipora.backend_springboot.config.ContactoPlataforma;
import com.temipora.backend_springboot.config.JwtUtils;
import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.servicios.CuentaDeBajaException;
import com.temipora.backend_springboot.servicios.CuentaSancionadaException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;


public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private static final Pattern PEDIDO_DETALLE =
        Pattern.compile("^/api/pedidos/\\d+$");
    private static final Pattern PEDIDOS_RESTRINGIDOS =
        Pattern.compile("^/api/pedidos/restringido$");
    private static final Pattern SOLICITUD_DETALLE =
        Pattern.compile("^/api/solicitudes-servicio/\\d+$");
    private static final Pattern ENTREGA_HITO =
        Pattern.compile("^/api/pedidos/\\d+/hitos/\\d+/entregas$");
    private static final Pattern DEUDAS =
        Pattern.compile("^/api/pedidos/deudas$");
    private static final Pattern PAGO_HITO =
        Pattern.compile("^/api/pedidos/\\d+/hitos/\\d+/pagar$");

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final ContactoPlataforma contacto;

    public JwtAuthorizationFilter(
            JwtUtils jwtUtils,
            UserDetailsService userDetailsService,
            ContactoPlataforma contacto) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.contacto = contacto;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtUtils.tokenEsValido(token)) {
                String nombreUsuario = jwtUtils.obtenerNombreUsuarioToken(token);

                if (nombreUsuario != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails;
                    Optional<AlcanceSancion> restriccion = Optional.empty();
                    try {
                        if (userDetailsService instanceof AccesoUsuarioService acceso) {
                            userDetails = acceso.loadUserForSession(nombreUsuario);
                            restriccion = acceso.restriccionVigente(nombreUsuario);
                        } else {
                            userDetails = userDetailsService.loadUserByUsername(nombreUsuario);
                        }
                    } catch (CuentaSancionadaException sancionada) {
                        escribir403(response, "CUENTA_SANCIONADA",
                            sancionada.getMotivo(), sancionada.getContacto(),
                            sancionada.getFechaFin());
                        return;
                    } catch (CuentaDeBajaException deBaja) {
                        escribirBaja(response, deBaja);
                        return;
                    } catch (DisabledException deshabilitada) {
                        escribir403(response, "CUENTA_INACTIVA", null, contacto.getEmail(), null);
                        return;
                    } catch (UsernameNotFoundException inexistente) {
                        userDetails = null;
                    }

                    if (userDetails != null) {
                        AlcanceSancion alcanceRestringido = restriccion.orElse(null);
                        if (esAlcanceConListaBlanca(alcanceRestringido)
                                && !permitidaParaRestringido(
                                    request, alcanceRestringido)) {
                            escribirRestringida(response, alcanceRestringido);
                            return;
                        }
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }
        }

        chain.doFilter(request, response);
    }

    private boolean esAlcanceConListaBlanca(AlcanceSancion alcance) {
        return alcance == AlcanceSancion.ENTREGAS
            || alcance == AlcanceSancion.PAGOS;
    }

    private boolean permitidaParaRestringido(
            HttpServletRequest request, AlcanceSancion alcance) {
        return alcance == AlcanceSancion.PAGOS
            ? permitidaParaPagos(request)
            : permitidaParaEntregas(request);
    }

    private boolean permitidaParaEntregas(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String metodo = request.getMethod();
        if ("GET".equals(metodo)) {
            return PEDIDO_DETALLE.matcher(uri).matches()
                || PEDIDOS_RESTRINGIDOS.matcher(uri).matches()
                || SOLICITUD_DETALLE.matcher(uri).matches()
                || uri.startsWith("/api/notificaciones")
                || uri.startsWith("/api/apelaciones")
                || uri.startsWith("/api/contacto");
        }
        if ("POST".equals(metodo)) {
            return ENTREGA_HITO.matcher(uri).matches()
                || uri.startsWith("/api/apelaciones");
        }
        return false;
    }

    private boolean permitidaParaPagos(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String metodo = request.getMethod();
        if ("GET".equals(metodo)) {
            return DEUDAS.matcher(uri).matches()
                || PEDIDO_DETALLE.matcher(uri).matches()
                || uri.equals("/api/mi-estado-cuenta")
                || uri.startsWith("/api/notificaciones")
                || uri.startsWith("/api/apelaciones")
                || uri.startsWith("/api/contacto")
                || uri.startsWith("/api/configuracion-pedido");
        }
        if ("POST".equals(metodo)) {
            return PAGO_HITO.matcher(uri).matches()
                || uri.startsWith("/api/apelaciones")
                || uri.equals("/api/auth/logout");
        }
        return false;
    }

    private void escribirRestringida(
            HttpServletResponse response, AlcanceSancion alcance) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("codigo", "CUENTA_RESTRINGIDA");
        body.put("alcance", alcance != null ? alcance.name() : "ENTREGAS");
        if (contacto != null && contacto.getEmail() != null) {
            body.put("contacto", contacto.getEmail());
        }
        body.put("message", alcance == AlcanceSancion.PAGOS
            ? "Tu cuenta solo puede regularizar los pagos pendientes."
            : "Tu cuenta solo puede subir los entregables pendientes.");

        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        response.getWriter().flush();
    }

    private void escribir403(
            HttpServletResponse response,
            String codigo,
            String motivo,
            String contactoPlataforma,
            LocalDateTime fechaFin) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("codigo", codigo);
        if (motivo != null) {
            body.put("motivo", motivo);
        }
        if (contactoPlataforma != null) {
            body.put("contacto", contactoPlataforma);
        }
        if (fechaFin != null) {
            body.put("fechaFin", fechaFin.toString());
        }
        body.put("message", "CUENTA_SANCIONADA".equals(codigo)
            ? "Tu cuenta se encuentra inhabilitada."
            : "Tu cuenta se encuentra desactivada.");

        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        response.getWriter().flush();
    }

    private void escribirBaja(
            HttpServletResponse response,
            CuentaDeBajaException deBaja) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("codigo", "CUENTA_DE_BAJA");
        if (deBaja.getFechaBaja() != null) {
            body.put("fechaBaja", deBaja.getFechaBaja().toString());
        }
        if (deBaja.getReactivarHasta() != null) {
            body.put("reactivarHasta", deBaja.getReactivarHasta().toString());
        }
        body.put("puedeReactivar", deBaja.isPuedeReactivar());
        if (deBaja.getContacto() != null) {
            body.put("contacto", deBaja.getContacto());
        }
        body.put("message", deBaja.isPuedeReactivar()
            ? "Tu cuenta está dada de baja."
            : "El plazo para reactivar tu cuenta venció.");

        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        response.getWriter().flush();
    }
}
