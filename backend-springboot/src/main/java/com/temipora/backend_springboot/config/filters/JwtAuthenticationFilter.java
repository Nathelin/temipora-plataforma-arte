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
import com.temipora.backend_springboot.modelo.Usuario;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private JwtUtils jwtUtils;
    public JwtAuthenticationFilter(JwtUtils jwtUtils){
        this.jwtUtils = jwtUtils;
    }
    
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, 
                                                HttpServletResponse response)throws AuthenticationException{
        // cuando el usuario se intente autenticar
        // recupero el usuario, eoeo

        Usuario usuario = null;
        String nombreUsuario = "";
        String contrasenia = ""; // TODO: ver si hay otra forma de hacerlo sin poner contraseña acá..

        try {
            usuario = new ObjectMapper().readValue(request.getInputStream(), com.temipora.backend_springboot.modelo.Usuario.class);
            nombreUsuario = usuario.getNombreUsuario();
            contrasenia = usuario.getContrasenia();
        } catch (Exception e) {
            // tengo q controlar
        }

        UsernamePasswordAuthenticationToken token = 
            new UsernamePasswordAuthenticationToken(nombreUsuario, contrasenia);

        return getAuthenticationManager().authenticate(token);
    }


    @Override
    protected void successfulAuthentication(HttpServletRequest request, 
                                            HttpServletResponse response, 
                                            FilterChain chain, 
                                            Authentication result) throws IOException, ServletException{

        User usuario = (User)result.getPrincipal();
        String token = jwtUtils.generarTokenAcceso(usuario.getUsername());

        response.addHeader("Authorization", token);

        Map<String, Object> httpResponse = new HashMap<>();
        httpResponse.put("token", token);
        httpResponse.put("Message", "Autenticación realizada correctamente");
        httpResponse.put("NombreUsuario", usuario.getUsername());

        response.getWriter().write(new ObjectMapper().writeValueAsString(httpResponse));
        response.setStatus(200); // valor del HttpRequest.OK
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().flush();



        super.successfulAuthentication(request, response, chain, result);
    }

    // y puedo configurar tambien despues el de unsuccessful authentication
}
