package com.temipora.backend_springboot.config.filters;

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.temipora.backend_springboot.modelo.TerminosPlataforma;
import com.temipora.backend_springboot.repositorios.AceptacionTerminosPlataformaRepository;
import com.temipora.backend_springboot.repositorios.TerminosPlataformaRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Bloquea cualquier request autenticado si el usuario no aceptó la versión
 * vigente de los TOS de plataforma. Los endpoints de TOS, login, registro y
 * recursos públicos quedan exentos.
 */
public class TerminosAceptadosFilter extends OncePerRequestFilter {

    private final AceptacionTerminosPlataformaRepository aceptacionRepository;
    private final TerminosPlataformaRepository terminosRepository;

    public TerminosAceptadosFilter(
            AceptacionTerminosPlataformaRepository aceptacionRepository,
            TerminosPlataformaRepository terminosRepository) {
        this.aceptacionRepository = aceptacionRepository;
        this.terminosRepository = terminosRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/")
            || path.startsWith("/api/terminos-plataforma")
            || path.equals("/api/checkNombreUsuario")
            || path.startsWith("/api/upload")
            || path.startsWith("/api/emails/baja/")
            || path.startsWith("/api/pagos/mercadopago/webhook")
            || path.startsWith("/api/pagos/mercadopago/oauth/callback")
            || path.startsWith("/ws")
            || path.startsWith("/error")
            || path.startsWith("/uploads")
            || path.startsWith("/css")
            || path.startsWith("/js")
            || path.startsWith("/images")
            || path.equals("/TOS");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        TerminosPlataforma vigente = terminosRepository
            .findFirstByVigenteTrueOrderByVersionDesc()
            .orElse(null);
        if (vigente == null) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean aceptado = aceptacionRepository
            .existsByUsuarioNombreUsuarioIgnoreCaseAndVersion(
                auth.getName(), vigente.getVersion());
        if (aceptado) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
            "{\"codigo\":\"TOS_NO_ACEPTADOS\","
                + "\"versionVigente\":" + vigente.getVersion() + ","
                + "\"message\":\"Tenés que aceptar los términos de la plataforma vigentes.\"}");
        response.getWriter().flush();
    }
}
