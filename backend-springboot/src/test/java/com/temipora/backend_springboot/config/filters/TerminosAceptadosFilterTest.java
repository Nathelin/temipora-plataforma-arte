package com.temipora.backend_springboot.config.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.temipora.backend_springboot.modelo.TerminosPlataforma;
import com.temipora.backend_springboot.repositorios.AceptacionTerminosPlataformaRepository;
import com.temipora.backend_springboot.repositorios.TerminosPlataformaRepository;

import jakarta.servlet.FilterChain;

@ExtendWith(MockitoExtension.class)
class TerminosAceptadosFilterTest {

    @Mock
    private AceptacionTerminosPlataformaRepository aceptacionRepository;

    @Mock
    private TerminosPlataformaRepository terminosRepository;

    @InjectMocks
    private TerminosAceptadosFilter filter;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinAutenticacion_dejaPasar() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/servicios/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean paso = new AtomicBoolean(false);

        filter.doFilter(request, response, cadena(paso));

        assertThat(paso).isTrue();
    }

    @Test
    void endpointTos_noFiltra() throws Exception {
        autenticar("persona");
        MockHttpServletRequest request = new MockHttpServletRequest(
            "GET", "/api/terminos-plataforma/estado");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean paso = new AtomicBoolean(false);

        filter.doFilter(request, response, cadena(paso));

        assertThat(paso).isTrue();
    }

    @Test
    void sinAceptacionVigente_devuelve403() throws Exception {
        autenticar("persona");
        when(terminosRepository.findFirstByVigenteTrueOrderByVersionDesc())
            .thenReturn(Optional.of(terminos(4)));
        when(aceptacionRepository.existsByUsuarioNombreUsuarioIgnoreCaseAndVersion("persona", 4))
            .thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/servicios/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean paso = new AtomicBoolean(false);

        filter.doFilter(request, response, cadena(paso));

        assertThat(paso).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("TOS_NO_ACEPTADOS");
    }

    @Test
    void conAceptacionVigente_dejaPasar() throws Exception {
        autenticar("persona");
        when(terminosRepository.findFirstByVigenteTrueOrderByVersionDesc())
            .thenReturn(Optional.of(terminos(4)));
        when(aceptacionRepository.existsByUsuarioNombreUsuarioIgnoreCaseAndVersion("persona", 4))
            .thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/servicios/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean paso = new AtomicBoolean(false);

        filter.doFilter(request, response, cadena(paso));

        assertThat(paso).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private void autenticar(String username) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }

    private FilterChain cadena(AtomicBoolean paso) {
        return (request, response) -> paso.set(true);
    }

    private TerminosPlataforma terminos(int version) {
        TerminosPlataforma terminos = new TerminosPlataforma();
        terminos.setVersion(version);
        terminos.setVigente(true);
        return terminos;
    }
}
