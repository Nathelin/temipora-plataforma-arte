package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.CategoriaEtiqueta;
import com.temipora.backend_springboot.modelo.CategoriaEtiquetaDTO;
import com.temipora.backend_springboot.modelo.CategoriaEtiquetaRequest;
import com.temipora.backend_springboot.repositorios.CategoriaEtiquetaRepository;
import com.temipora.backend_springboot.repositorios.EtiquetaRepository;

@ExtendWith(MockitoExtension.class)
class CategoriaEtiquetaServiceTest {

    @Mock
    private CategoriaEtiquetaRepository categoriaEtiquetaRepository;

    @Mock
    private EtiquetaRepository etiquetaRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private CategoriaEtiquetaService categoriaEtiquetaService;

    // ---------------------------------------------------------------
    // listar / listarActivas
    // ---------------------------------------------------------------

    @Test
    void listar_mapeaEntidadesADtos() {
        when(categoriaEtiquetaRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(categoria(1L, "Estilo", true), categoria(2L, "Tecnica", false)));

        List<CategoriaEtiquetaDTO> resultado = categoriaEtiquetaService.listar();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(CategoriaEtiquetaDTO::getNombre)
                .containsExactly("Estilo", "Tecnica");
        assertThat(resultado.get(0).isActivo()).isTrue();
        assertThat(resultado.get(1).isActivo()).isFalse();
    }

    @Test
    void listarActivas_mapeaDtos() {
        when(categoriaEtiquetaRepository.findByActivoTrueOrderByNombreAsc())
                .thenReturn(List.of(categoria(1L, "Estilo", true)));

        List<CategoriaEtiquetaDTO> resultado = categoriaEtiquetaService.listarActivas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getIdCategoriaEtiqueta()).isEqualTo(1L);
    }

    // ---------------------------------------------------------------
    // crear
    // ---------------------------------------------------------------

    @Test
    void crear_nombreNoRepetido_guardaActivoYConCamposLimpios() {
        CategoriaEtiquetaRequest request = request("  Estilo  ", "  Agrupa estilos  ", null);
        when(categoriaEtiquetaRepository.existsByNombreIgnoreCase("Estilo")).thenReturn(false);
        when(categoriaEtiquetaRepository.save(any(CategoriaEtiqueta.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CategoriaEtiquetaDTO resultado = categoriaEtiquetaService.crear(request);

        ArgumentCaptor<CategoriaEtiqueta> captor = ArgumentCaptor.forClass(CategoriaEtiqueta.class);
        verify(categoriaEtiquetaRepository).save(captor.capture());
        CategoriaEtiqueta guardada = captor.getValue();
        assertThat(guardada.getNombre()).isEqualTo("Estilo");
        assertThat(guardada.getDescripcion()).isEqualTo("Agrupa estilos");
        assertThat(guardada.getObservaciones()).isNull();
        assertThat(guardada.isActivo()).isTrue();
        assertThat(resultado.getNombre()).isEqualTo("Estilo");
    }

    @Test
    void crear_nombreRepetido_lanzaError() {
        CategoriaEtiquetaRequest request = request("Estilo", "desc", null);
        when(categoriaEtiquetaRepository.existsByNombreIgnoreCase("Estilo")).thenReturn(true);

        assertThatThrownBy(() -> categoriaEtiquetaService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe una categoría con ese nombre.");

        verify(categoriaEtiquetaRepository, never()).save(any(CategoriaEtiqueta.class));
    }

    // ---------------------------------------------------------------
    // modificar
    // ---------------------------------------------------------------

    @Test
    void modificar_ok_actualizaCamposSinTocarEstado() {
        CategoriaEtiqueta existente = categoria(1L, "Estilo", false);
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(1L)).thenReturn(Optional.of(existente));
        when(categoriaEtiquetaRepository.existsByNombreIgnoreCaseAndIdCategoriaEtiquetaNot("Estilo Nuevo", 1L))
                .thenReturn(false);
        when(categoriaEtiquetaRepository.save(any(CategoriaEtiqueta.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CategoriaEtiquetaDTO resultado = categoriaEtiquetaService
                .modificar(1L, request("Estilo Nuevo", "Descripcion nueva", "obs"));

        assertThat(resultado.getNombre()).isEqualTo("Estilo Nuevo");
        assertThat(resultado.getDescripcion()).isEqualTo("Descripcion nueva");
        assertThat(resultado.getObservaciones()).isEqualTo("obs");
        assertThat(resultado.isActivo()).isFalse();
    }

    @Test
    void modificar_nombreRepetidoEnOtra_lanzaError() {
        CategoriaEtiqueta existente = categoria(1L, "Estilo", true);
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(1L)).thenReturn(Optional.of(existente));
        when(categoriaEtiquetaRepository.existsByNombreIgnoreCaseAndIdCategoriaEtiquetaNot("Tecnica", 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> categoriaEtiquetaService.modificar(1L, request("Tecnica", "desc", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe una categoría con ese nombre.");
    }

    @Test
    void modificar_categoriaInexistente_lanzaNoSuchElement() {
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaEtiquetaService.modificar(99L, request("X", "desc", null)))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Categoría de etiqueta no encontrada: 99");
    }

    // ---------------------------------------------------------------
    // darDeBaja / darDeAlta
    // ---------------------------------------------------------------

    @Test
    void darDeBaja_conEtiquetasVivas_lanzaError() {
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(1L))
                .thenReturn(Optional.of(categoria(1L, "Estilo", true)));
        when(etiquetaRepository.countByCategoriaIdCategoriaEtiquetaAndEstadoIn(anyLong(), any()))
                .thenReturn(3L);

        assertThatThrownBy(() -> categoriaEtiquetaService.darDeBaja(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3")
                .hasMessageContaining("válida o pendiente");

        verify(categoriaEtiquetaRepository, never()).save(any(CategoriaEtiqueta.class));
    }

    @Test
    void darDeBaja_sinEtiquetasVivas_desactiva() {
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(1L))
                .thenReturn(Optional.of(categoria(1L, "Estilo", true)));
        when(etiquetaRepository.countByCategoriaIdCategoriaEtiquetaAndEstadoIn(anyLong(), any()))
                .thenReturn(0L);
        when(categoriaEtiquetaRepository.save(any(CategoriaEtiqueta.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CategoriaEtiquetaDTO resultado = categoriaEtiquetaService.darDeBaja(1L);

        assertThat(resultado.isActivo()).isFalse();
    }

    @Test
    void darDeBaja_categoriaInexistente_lanzaNoSuchElement() {
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaEtiquetaService.darDeBaja(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void darDeAlta_reactivaLaCategoria() {
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(1L))
                .thenReturn(Optional.of(categoria(1L, "Estilo", false)));
        when(categoriaEtiquetaRepository.save(any(CategoriaEtiqueta.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CategoriaEtiquetaDTO resultado = categoriaEtiquetaService.darDeAlta(1L);

        assertThat(resultado.isActivo()).isTrue();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private CategoriaEtiqueta categoria(Long id, String nombre, boolean activo) {
        return CategoriaEtiqueta.builder()
                .idCategoriaEtiqueta(id)
                .nombre(nombre)
                .descripcion("descripcion")
                .observaciones(null)
                .activo(activo)
                .build();
    }

    private CategoriaEtiquetaRequest request(String nombre, String descripcion, String observaciones) {
        CategoriaEtiquetaRequest request = new CategoriaEtiquetaRequest();
        request.setNombre(nombre);
        request.setDescripcion(descripcion);
        request.setObservaciones(observaciones);
        return request;
    }
}
