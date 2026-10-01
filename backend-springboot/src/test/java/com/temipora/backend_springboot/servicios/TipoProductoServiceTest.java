package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.temipora.backend_springboot.modelo.TipoProducto;
import com.temipora.backend_springboot.modelo.TipoProductoDTO;
import com.temipora.backend_springboot.modelo.TipoProductoRequest;
import com.temipora.backend_springboot.repositorios.TipoProductoRepository;

@ExtendWith(MockitoExtension.class)
class TipoProductoServiceTest {

    @Mock
    private TipoProductoRepository tipoProductoRepository;

    @InjectMocks
    private TipoProductoService tipoProductoService;

    // ---------------------------------------------------------------
    // listar
    // ---------------------------------------------------------------

    @Test
    void listar_mapeaEntidadesADtos() {
        when(tipoProductoRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(tipoProducto(1L, "Lienzo"), tipoProducto(2L, "Pincel")));

        List<TipoProductoDTO> resultado = tipoProductoService.listar();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(TipoProductoDTO::getNombre)
                .containsExactly("Lienzo", "Pincel");
        assertThat(resultado.get(0).getIdTipoProducto()).isEqualTo(1L);
    }

    @Test
    void listar_sinResultados_devuelveListaVacia() {
        when(tipoProductoRepository.findAllByOrderByNombreAsc()).thenReturn(List.of());

        assertThat(tipoProductoService.listar()).isEmpty();
    }

    // ---------------------------------------------------------------
    // crear
    // ---------------------------------------------------------------

    @Test
    void crear_nombreNoRepetido_guardaActivoYConCamposLimpios() {
        TipoProductoRequest request = request("  Lienzo  ", "  Material de dibujo  ", null);
        when(tipoProductoRepository.existsByNombreIgnoreCase("Lienzo")).thenReturn(false);
        when(tipoProductoRepository.save(any(TipoProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoProductoDTO resultado = tipoProductoService.crear(request);

        ArgumentCaptor<TipoProducto> captor = ArgumentCaptor.forClass(TipoProducto.class);
        verify(tipoProductoRepository).save(captor.capture());
        TipoProducto guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Lienzo");
        assertThat(guardado.getDescripcion()).isEqualTo("Material de dibujo");
        assertThat(guardado.getObservaciones()).isNull();
        assertThat(guardado.isActivo()).isTrue();
        assertThat(resultado.getNombre()).isEqualTo("Lienzo");
        assertThat(resultado.isActivo()).isTrue();
    }

    @Test
    void crear_observacionesPresentes_lasTrimea() {
        TipoProductoRequest request = request("Lienzo", "Material", "  observación  ");
        when(tipoProductoRepository.existsByNombreIgnoreCase("Lienzo")).thenReturn(false);
        when(tipoProductoRepository.save(any(TipoProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        tipoProductoService.crear(request);

        ArgumentCaptor<TipoProducto> captor = ArgumentCaptor.forClass(TipoProducto.class);
        verify(tipoProductoRepository).save(captor.capture());
        assertThat(captor.getValue().getObservaciones()).isEqualTo("observación");
    }

    @Test
    void crear_nombreYaExistente_lanzaArgumentoInvalidoYNoGuarda() {
        TipoProductoRequest request = request("lienzO", "Material", null);
        when(tipoProductoRepository.existsByNombreIgnoreCase("lienzO")).thenReturn(true);

        assertThatThrownBy(() -> tipoProductoService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe un tipo de producto con ese nombre.");

        verify(tipoProductoRepository, never()).save(any(TipoProducto.class));
    }

    // ---------------------------------------------------------------
    // modificar
    // ---------------------------------------------------------------

    @Test
    void modificar_idInexistente_lanzaNoSuchElementAntesDeValidarNombre() {
        TipoProductoRequest request = request("Lienzo", "Material", null);
        when(tipoProductoRepository.findByIdTipoProducto(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoProductoService.modificar(99L, request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Tipo de producto no encontrado: 99");

        verify(tipoProductoRepository, never()).save(any(TipoProducto.class));
    }

    @Test
    void modificar_nombreRepetidoEnOtroRegistro_lanzaArgumentoInvalido() {
        TipoProductoRequest request = request("Pincel", "Material", null);
        when(tipoProductoRepository.findByIdTipoProducto(1L)).thenReturn(Optional.of(tipoProducto(1L, "Lienzo")));
        when(tipoProductoRepository.existsByNombreIgnoreCaseAndIdTipoProductoNot("Pincel", 1L)).thenReturn(true);

        assertThatThrownBy(() -> tipoProductoService.modificar(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe un tipo de producto con ese nombre.");

        verify(tipoProductoRepository, never()).save(any(TipoProducto.class));
    }

    @Test
    void modificar_nombrePropioPermitido_actualizaYGuarda() {
        TipoProducto existente = tipoProducto(1L, "Lienzo");
        existente.setDescripcion("Descripción vieja");
        TipoProductoRequest request = request("  Lienzo nuevo  ", "  Descripción nueva  ", "  obs  ");

        when(tipoProductoRepository.findByIdTipoProducto(1L)).thenReturn(Optional.of(existente));
        when(tipoProductoRepository.existsByNombreIgnoreCaseAndIdTipoProductoNot("Lienzo nuevo", 1L))
                .thenReturn(false);
        when(tipoProductoRepository.save(any(TipoProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoProductoDTO resultado = tipoProductoService.modificar(1L, request);

        assertThat(resultado.getNombre()).isEqualTo("Lienzo nuevo");
        assertThat(resultado.getDescripcion()).isEqualTo("Descripción nueva");
        assertThat(resultado.getObservaciones()).isEqualTo("obs");
        verify(tipoProductoRepository).save(existente);
    }

    @Test
    void modificar_conObservacionesNull_lasDejaEnNull() {
        TipoProducto existente = tipoProducto(1L, "Lienzo");
        existente.setObservaciones("obs anterior");
        TipoProductoRequest request = request("Lienzo", "Material", null);

        when(tipoProductoRepository.findByIdTipoProducto(1L)).thenReturn(Optional.of(existente));
        when(tipoProductoRepository.existsByNombreIgnoreCaseAndIdTipoProductoNot("Lienzo", 1L))
                .thenReturn(false);
        when(tipoProductoRepository.save(any(TipoProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoProductoDTO resultado = tipoProductoService.modificar(1L, request);

        assertThat(resultado.getObservaciones()).isNull();
    }

    // ---------------------------------------------------------------
    // darDeBaja / darDeAlta
    // ---------------------------------------------------------------

    @Test
    void darDeBaja_desactivaElTipo() {
        TipoProducto existente = tipoProducto(1L, "Lienzo");
        when(tipoProductoRepository.findByIdTipoProducto(1L)).thenReturn(Optional.of(existente));
        when(tipoProductoRepository.save(any(TipoProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoProductoDTO resultado = tipoProductoService.darDeBaja(1L);

        assertThat(resultado.isActivo()).isFalse();
        verify(tipoProductoRepository).save(existente);
    }

    @Test
    void darDeAlta_activaElTipo() {
        TipoProducto existente = tipoProducto(1L, "Lienzo");
        existente.setActivo(false);
        when(tipoProductoRepository.findByIdTipoProducto(1L)).thenReturn(Optional.of(existente));
        when(tipoProductoRepository.save(any(TipoProducto.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoProductoDTO resultado = tipoProductoService.darDeAlta(1L);

        assertThat(resultado.isActivo()).isTrue();
        verify(tipoProductoRepository).save(existente);
    }

    @Test
    void darDeBaja_idInexistente_lanzaNoSuchElement() {
        when(tipoProductoRepository.findByIdTipoProducto(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoProductoService.darDeBaja(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Tipo de producto no encontrado: 99");
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private TipoProductoRequest request(String nombre, String descripcion, String observaciones) {
        TipoProductoRequest request = new TipoProductoRequest();
        request.setNombre(nombre);
        request.setDescripcion(descripcion);
        request.setObservaciones(observaciones);
        return request;
    }

    private TipoProducto tipoProducto(Long id, String nombre) {
        return TipoProducto.builder()
                .idTipoProducto(id)
                .nombre(nombre)
                .descripcion("Descripción")
                .activo(true)
                .build();
    }
}
