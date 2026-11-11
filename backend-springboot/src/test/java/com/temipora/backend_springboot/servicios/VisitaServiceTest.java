package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.repositorios.VisitaArtistaRepository;
import com.temipora.backend_springboot.repositorios.VisitaProductoRepository;
import com.temipora.backend_springboot.repositorios.VisitaServicioRepository;

@ExtendWith(MockitoExtension.class)
class VisitaServiceTest {

    @Mock
    private VisitaArtistaRepository visitaArtistaRepository;

    @Mock
    private VisitaServicioRepository visitaServicioRepository;

    @Mock
    private VisitaProductoRepository visitaProductoRepository;

    @InjectMocks
    private VisitaService visitaService;

    @Test
    void registrarProducto_conNulosNoEscribe() {
        visitaService.registrarProducto(null, 1L);
        visitaService.registrarProducto(1L, null);

        verifyNoInteractions(visitaProductoRepository);
    }

    @Test
    void registrarProducto_delegaEnElRepositorio() {
        visitaService.registrarProducto(1L, 2L);

        verify(visitaProductoRepository).registrar(1L, 2L);
    }

    @Test
    void registrarProducto_tragaErroresDeTracking() {
        doThrow(new RuntimeException("boom"))
            .when(visitaProductoRepository).registrar(1L, 2L);

        assertThatCode(() -> visitaService.registrarProducto(1L, 2L))
            .doesNotThrowAnyException();
    }

    @Test
    void registrarArtista_ignoraLaVisitaAlPropioPerfil() {
        visitaService.registrarArtista(1L, 1L);

        verifyNoInteractions(visitaArtistaRepository);
    }

    @Test
    void registrarServicio_registraYTragaErrores() {
        visitaService.registrarServicio(1L, 2L);
        verify(visitaServicioRepository).registrar(1L, 2L);

        doThrow(new RuntimeException("boom"))
            .when(visitaServicioRepository).registrar(1L, 3L);
        assertThatCode(() -> visitaService.registrarServicio(1L, 3L))
            .doesNotThrowAnyException();
    }
}
