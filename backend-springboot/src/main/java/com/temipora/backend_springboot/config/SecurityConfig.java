package com.temipora.backend_springboot.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

import com.temipora.backend_springboot.config.filters.JwtAuthenticationFilter;
import com.temipora.backend_springboot.config.filters.JwtAuthorizationFilter;
import com.temipora.backend_springboot.config.filters.TerminosAceptadosFilter;
import com.temipora.backend_springboot.servicios.UsuarioService;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final ObjectProvider<TerminosAceptadosFilter> terminosAceptadosFilterProvider;

    @org.springframework.beans.factory.annotation.Value(
        "${temipora.contacto.email:temiporasupport@gmail.com}")
    private String contactoEmail;

    public SecurityConfig(
            JwtUtils jwtUtils,
            UserDetailsService userDetailsService,
            ObjectProvider<TerminosAceptadosFilter> terminosAceptadosFilterProvider) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.terminosAceptadosFilterProvider = terminosAceptadosFilterProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            AuthenticationManager authenticationManager,
            UsuarioService usuarioService) throws Exception {

        ContactoPlataforma contactoPlataforma = new ContactoPlataforma(contactoEmail);
        JwtAuthenticationFilter jwtAuthenticationFilter =
            new JwtAuthenticationFilter(jwtUtils, usuarioService, contactoPlataforma);
        jwtAuthenticationFilter.setFilterProcessesUrl("/api/auth/login");
        jwtAuthenticationFilter.setAuthenticationManager(authenticationManager);

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/api/auth/login",
                    "/api/auth/registrarse",
                    "/api/auth/acceso",
                    "/api/auth/acceso/**",
                    "/api/contacto",
                    "/api/upload",
                    "/api/checkNombreUsuario",
                    "/uploads/**",
                    "/ws",
                    "/ws/**",
                    "/TOS",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/error",
                    "/api/servicios/**",
                    "/api/sustituciones",
                    "/api/perfil/**",
                    "/api/portfolio/**",
                    "/api/galeria/**",
                    "/api/resenias/**",
                    "/api/terminos-plataforma",
                    "/api/comisiones/**",
                    "/api/configuracion-pedido",
                    "/api/ofertas/campana/**",
                    "/api/emails/baja/**",
                    "/api/pagos/mercadopago/webhook",
                    "/api/pagos/mercadopago/oauth/callback"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception
                // Sin token/expirado: 401 (no 403) para que el front cierre sesión y redirija al login
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"message\":\"No autenticado.\"}");
                })
            )
            .addFilter(jwtAuthenticationFilter)
            .addFilterBefore(new JwtAuthorizationFilter(jwtUtils, userDetailsService, contactoPlataforma),
                    UsernamePasswordAuthenticationFilter.class);

        TerminosAceptadosFilter terminosAceptadosFilter =
            terminosAceptadosFilterProvider.getIfAvailable();
        if (terminosAceptadosFilter != null) {
            http.addFilterAfter(terminosAceptadosFilter,
                UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:5173",
            "https://*.ngrok-free.dev",
            "https://*.ngrok-free.app",
            "https://*.trycloudflare.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
}
