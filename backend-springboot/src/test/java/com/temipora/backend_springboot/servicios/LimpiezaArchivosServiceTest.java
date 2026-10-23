package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.repositorios.ImagenRepository;
import com.temipora.backend_springboot.repositorios.RespuestaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class LimpiezaArchivosServiceTest {

    @Mock
    private StorageService storageService;

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RespuestaRepository respuestaRepository;

    @InjectMocks
    private LimpiezaArchivosService limpieza;

    @Test
    void eliminar_archivoReferenciadoEnPortfolio_noLoBorra() {
        when(imagenRepository.existsByUrl("/uploads/a.webp")).thenReturn(true);

        int borrados = limpieza.eliminar(List.of("/uploads/a.webp"));

        assertThat(borrados).isZero();
        verify(storageService, never()).eliminar(any());
    }

    @Test
    void eliminar_archivoReferenciadoComoFotoDePerfil_noLoBorra() {
        when(imagenRepository.existsByUrl("/uploads/b.webp")).thenReturn(false);
        when(usuarioRepository.existeUrlEnPerfil("/uploads/b.webp")).thenReturn(true);

        int borrados = limpieza.eliminar(List.of("/uploads/b.webp"));

        assertThat(borrados).isZero();
        verify(storageService, never()).eliminar(any());
    }

    @Test
    void eliminar_archivoNoReferenciado_loBorra() {
        when(imagenRepository.existsByUrl("/uploads/c.webp")).thenReturn(false);
        when(usuarioRepository.existeUrlEnPerfil("/uploads/c.webp")).thenReturn(false);
        when(storageService.eliminar("/uploads/c.webp")).thenReturn(true);

        int borrados = limpieza.eliminar(List.of("/uploads/c.webp"));

        assertThat(borrados).isEqualTo(1);
    }

    @Test
    void eliminar_listaVacia_oNula_noInteractuaConLasDependencias() {
        assertThat(limpieza.eliminar(null)).isZero();
        assertThat(limpieza.eliminar(Collections.emptyList())).isZero();

        verifyNoInteractions(storageService, imagenRepository, usuarioRepository);
    }

    @Test
    void limpiarHuerfanos_excluyeUrlsReferenciadasYUsaAntiguedadDe24Horas() {
        when(imagenRepository.findUrls()).thenReturn(List.of("/uploads/viva.webp"));
        when(usuarioRepository.findUrlsFotoPerfil()).thenReturn(List.of("/uploads/avatar.webp"));
        when(usuarioRepository.findUrlsFotoPerfilMiniatura()).thenReturn(Collections.emptyList());
        when(usuarioRepository.findUrlsBanner()).thenReturn(Collections.emptyList());
        when(storageService.eliminarHuerfanos(any(), any())).thenAnswer(invocacion -> {
            Predicate<String> enUso = invocacion.getArgument(0);
            assertThat(enUso.test("/uploads/viva.webp")).isTrue();
            assertThat(enUso.test("/uploads/avatar.webp")).isTrue();
            assertThat(enUso.test("/uploads/huerfana.webp")).isFalse();
            Duration antiguedad = invocacion.getArgument(1);
            assertThat(antiguedad).isEqualTo(LimpiezaArchivosService.ANTIGUEDAD_HUERFANOS);
            return 1;
        });

        int borrados = limpieza.limpiarHuerfanos();

        assertThat(borrados).isEqualTo(1);
    }
}
