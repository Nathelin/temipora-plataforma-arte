package com.temipora.backend_springboot.config.filters;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.temipora.backend_springboot.config.ContactoPlataforma;
import com.temipora.backend_springboot.config.JwtUtils;
import com.temipora.backend_springboot.modelo.LoginRequest;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final JwtUtils jwtUtils;
    private final UsuarioService usuarioService;
    private final ContactoPlataforma contacto;

    public JwtAuthenticationFilter(
            JwtUtils jwtUtils,
            UsuarioService usuarioService,
            ContactoPlataforma contacto) {
        this.jwtUtils = jwtUtils;
        this.usuarioService = usuarioService;
        this.contacto = contacto;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {

        String nombreUsuario = "";
        String contrasenia = "";

        try {
            LoginRequest login = new ObjectMapper().readValue(request.getInputStream(), LoginRequest.class);
            nombreUsuario = login.getNombreUsuario();
            contrasenia = login.getContrasenia();
        } catch (Exception e) {
            logger.error("Error al leer el body del login", e);
        }

        UsernamePasswordAuthenticationToken token =
            new UsernamePasswordAuthenticationToken(nombreUsuario, contrasenia);

        return getAuthenticationManager().authenticate(token);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication result) throws IOException, ServletException {

        User usuario = (User) result.getPrincipal();
        Usuario entidad = usuarioService.getByNombreUsuario(usuario.getUsername());

        // La contraseña ya fue validada: recién ahora es seguro revelar la sanción.
        var sancionVigente = usuarioService.sancionVigente(entidad.getIdUsuario());
        if (sancionVigente.isPresent()) {
            escribirSancion(response, sancionVigente.get());
            return;
        }

        // El rol del token es el rol real del usuario, no la primera autoridad
        // (que puede ser un rol heredado por la jerarquía).
        String rol = entidad.getRol() != null && entidad.getRol().getNombre() != null
            ? entidad.getRol().getNombre().name()
            : "";

        usuarioService.actualizarUltimaConexion(usuario.getUsername());

        String token = jwtUtils.generarTokenAcceso(usuario.getUsername(), rol);

        Map<String, Object> httpResponse = new HashMap<>();
        httpResponse.put("token", token);
        httpResponse.put("Message", "Autenticación realizada correctamente");
        httpResponse.put("NombreUsuario", usuario.getUsername());
        httpResponse.put("rol", rol);
        httpResponse.put("idUsuario", entidad.getIdUsuario());
        httpResponse.put("urlFotoPerfil", entidad.getUrlFotoPerfil());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(new ObjectMapper().writeValueAsString(httpResponse));
        response.getWriter().flush();
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        if (failed instanceof DisabledException) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            Map<String, Object> body = new HashMap<>();
            body.put("codigo", "CUENTA_INACTIVA");
            body.put("contacto", contacto.getEmail());
            body.put("message", "Tu cuenta se encuentra desactivada.");
            response.getWriter().write(new ObjectMapper().writeValueAsString(body));
            response.getWriter().flush();
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        Map<String, String> body = new HashMap<>();
        body.put("error", "Credenciales inválidas");

        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        response.getWriter().flush();
    }

    private void escribirSancion(HttpServletResponse response, SancionCuenta sancion)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new HashMap<>();
        body.put("codigo", "CUENTA_SANCIONADA");
        body.put("motivo", sancion.getMotivo());
        body.put("contacto", contacto.getEmail());
        if (sancion.getFechaFin() != null) {
            body.put("fechaFin", sancion.getFechaFin().toString());
        }
        body.put("message", "Tu cuenta se encuentra inhabilitada.");

        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        response.getWriter().flush();
    }
}
