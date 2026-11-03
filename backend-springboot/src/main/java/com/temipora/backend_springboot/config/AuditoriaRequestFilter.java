package com.temipora.backend_springboot.config;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuditoriaRequestFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_ATTR = "auditoria.requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        request.setAttribute(REQUEST_ID_ATTR, UUID.randomUUID().toString());
        filterChain.doFilter(request, response);
    }
}
