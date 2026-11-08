// NUEVO

package com.temipora.backend_springboot.config.filters;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.temipora.backend_springboot.config.ContactoPlataforma;
import com.temipora.backend_springboot.config.JwtUtils;
import com.temipora.backend_springboot.servicios.CuentaSancionadaException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;


public class JwtAuthorizationFilter extends OncePerRequestFilter {

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
                    try {
                        userDetails = userDetailsService.loadUserByUsername(nombreUsuario);
                    } catch (CuentaSancionadaException sancionada) {
                        escribir403(response, "CUENTA_SANCIONADA",
                            sancionada.getMotivo(), sancionada.getContacto(),
                            sancionada.getFechaFin());
                        return;
                    } catch (DisabledException deshabilitada) {
                        escribir403(response, "CUENTA_INACTIVA", null, contacto.getEmail(), null);
                        return;
                    } catch (UsernameNotFoundException inexistente) {
                        userDetails = null;
                    }

                    if (userDetails != null) {
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

    private void escribir403(
            HttpServletResponse response,
            String codigo,
            String motivo,
            String contactoPlataforma,
            java.time.LocalDateTime fechaFin) throws IOException {
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
}