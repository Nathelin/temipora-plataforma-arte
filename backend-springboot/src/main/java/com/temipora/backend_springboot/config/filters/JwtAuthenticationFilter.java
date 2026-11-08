package com.temipora.backend_springboot.config.filters;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.temipora.backend_springboot.config.JwtUtils;
import com.temipora.backend_springboot.modelo.LoginRequest;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private JwtUtils jwtUtils;
    private UsuarioService usuarioService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UsuarioService usuarioService) {
        this.jwtUtils = jwtUtils;
        this.usuarioService = usuarioService;
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
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, String> body = new HashMap<>();
        body.put("error", "Credenciales inválidas");

        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        response.getWriter().flush();
    }
}