package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.repositorios.RecomendacionProductoRepository;
import com.temipora.backend_springboot.repositorios.RecomendacionRepository;
import com.temipora.backend_springboot.repositorios.VisitaArtistaRepository;
import com.temipora.backend_springboot.repositorios.VisitaProductoRepository;
import com.temipora.backend_springboot.repositorios.VisitaServicioRepository;

@ExtendWith(MockitoExtension.class)
class InteresUsuarioServiceTest {

    @Mock
    private RecomendacionRepository recomendacionRepository;

    @Mock
    private RecomendacionProductoRepository recomendacionProductoRepository;

    @Mock
    private VisitaServicioRepository visitaServicioRepository;

    @Mock
    private VisitaProductoRepository visitaProductoRepository;

    @Mock
    private VisitaArtistaRepository visitaArtistaRepository;

    @InjectMocks
    private InteresUsuarioService interesService;

    @Test
    void cargar_uneInteresesDeServiciosYProductos() {
        LocalDateTime fecha = LocalDateTime.now().minusDays(2);
        Timestamp timestamp = Timestamp.valueOf(fecha);

        when(recomendacionRepository.artistasFavoritos(1L)).thenReturn(List.of(10L));
        when(recomendacionRepository.artistasComprados(1L)).thenReturn(List.of(11L));
        when(recomendacionProductoRepository.artistasComprados(1L))
            .thenReturn(List.of(12L));
        when(recomendacionRepository.etiquetasCompradas(1L)).thenReturn(List.of(1L));
        when(recomendacionRepository.etiquetasFavoritas(1L)).thenReturn(List.of(2L));
        when(visitaServicioRepository.etiquetasVisitadas(eq(1L), any()))
            .thenReturn(List.of(3L));
        when(recomendacionProductoRepository.etiquetasFavoritas(1L))
            .thenReturn(List.of(4L));
        when(recomendacionProductoRepository.etiquetasCompradas(1L))
            .thenReturn(List.of(5L));
        when(visitaProductoRepository.etiquetasVisitadas(eq(1L), any()))
            .thenReturn(List.of(6L));
        when(recomendacionRepository.categoriasDeEtiquetas(anyCollection()))
            .thenReturn(List.of(100L));

        when(visitaArtistaRepository.resumenPorUsuario(eq(1L), any()))
            .thenReturn(List.<Object[]>of(new Object[] { 13L, 1L, timestamp }));
        when(recomendacionRepository.ultimaCompraPorArtista(1L))
            .thenReturn(List.<Object[]>of(new Object[] { 10L, timestamp }));
        when(recomendacionRepository.ultimoFavoritoArtista(1L))
            .thenReturn(List.of());
        when(recomendacionProductoRepository.ultimaCompraPorArtista(1L))
            .thenReturn(List.<Object[]>of(new Object[] { 12L, timestamp }));

        InteresUsuarioService.InteresContenido interes =
            interesService.cargar(1L, 180);

        assertThat(interes.vacio()).isFalse();
        assertThat(interes.artistasFavoritos()).containsExactly(10L);
        assertThat(interes.artistasComprados()).containsExactlyInAnyOrder(11L, 12L);
        assertThat(interes.artistasVisitados()).containsExactly(13L);
        assertThat(interes.etiquetas())
            .containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L);
        assertThat(interes.categorias()).containsExactly(100L);
        assertThat(interes.ultimaInteraccion()).containsKeys(10L, 12L, 13L);
    }

    @Test
    void cargar_sinSenales_devuelveInteresVacio() {
        InteresUsuarioService.InteresContenido interes =
            interesService.cargar(2L, 30);

        assertThat(interes.vacio()).isTrue();
        assertThat(interes.etiquetas()).isEmpty();
        assertThat(interes.categorias()).isEmpty();
        assertThat(interes.ultimaInteraccion()).isEmpty();
    }

    @Test
    void cargar_aceptaFechasSqlDateYDescartaFilasInvalidas() {
        java.sql.Date fecha = java.sql.Date.valueOf("2026-01-10");
        when(visitaArtistaRepository.resumenPorUsuario(eq(3L), any()))
            .thenReturn(List.<Object[]>of(
                new Object[] { 20L, 1L, fecha },
                new Object[] { null, 1L, fecha }));

        InteresUsuarioService.InteresContenido interes =
            interesService.cargar(3L, 10);

        assertThat(interes.artistasVisitados()).containsExactly(20L);
        assertThat(interes.ultimaInteraccion()).containsKey(20L);
    }
}
