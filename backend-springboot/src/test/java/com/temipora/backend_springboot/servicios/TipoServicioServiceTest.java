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

import com.temipora.backend_springboot.modelo.TipoServicio;
import com.temipora.backend_springboot.modelo.TipoServicioDTO;
import com.temipora.backend_springboot.modelo.TipoServicioRequest;
import com.temipora.backend_springboot.repositorios.TipoServicioRepository;

@ExtendWith(MockitoExtension.class)
class TipoServicioServiceTest {

    @Mock
    private TipoServicioRepository tipoServicioRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private TipoServicioService tipoServicioService;

    // ---------------------------------------------------------------
    // listar
    // ---------------------------------------------------------------

    @Test
    void listar_mapeaEntidadesADtos() {
        when(tipoServicioRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(tipoServicio(1L, "Ilustración"), tipoServicio(2L, "Comisión")));

        List<TipoServicioDTO> resultado = tipoServicioService.listar();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(TipoServicioDTO::getNombre)
                .containsExactly("Ilustración", "Comisión");
        assertThat(resultado.get(0).getIdTipoServicio()).isEqualTo(1L);
    }

    @Test
    void listar_sinResultados_devuelveListaVacia() {
        when(tipoServicioRepository.findAllByOrderByNombreAsc()).thenReturn(List.of());

        assertThat(tipoServicioService.listar()).isEmpty();
    }

    // ---------------------------------------------------------------
    // crear
    // ---------------------------------------------------------------

    @Test
    void crear_nombreNoRepetido_guardaActivoYConCamposLimpios() {
        TipoServicioRequest request = request("  Ilustración  ", "  Arte digital  ", null);
        when(tipoServicioRepository.existsByNombreIgnoreCase("Ilustración")).thenReturn(false);
        when(tipoServicioRepository.save(any(TipoServicio.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoServicioDTO resultado = tipoServicioService.crear(request);

        ArgumentCaptor<TipoServicio> captor = ArgumentCaptor.forClass(TipoServicio.class);
        verify(tipoServicioRepository).save(captor.capture());
        TipoServicio guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Ilustración");
        assertThat(guardado.getDescripcion()).isEqualTo("Arte digital");
        assertThat(guardado.getObservaciones()).isNull();
        assertThat(guardado.isActivo()).isTrue();
        assertThat(resultado.getNombre()).isEqualTo("Ilustración");
        assertThat(resultado.isActivo()).isTrue();
    }

    @Test
    void crear_observacionesPresentes_lasTrimea() {
        TipoServicioRequest request = request("Ilustración", "Arte digital", "  observación  ");
        when(tipoServicioRepository.existsByNombreIgnoreCase("Ilustración")).thenReturn(false);
        when(tipoServicioRepository.save(any(TipoServicio.class))).thenAnswer(inv -> inv.getArgument(0));

        tipoServicioService.crear(request);

        ArgumentCaptor<TipoServicio> captor = ArgumentCaptor.forClass(TipoServicio.class);
        verify(tipoServicioRepository).save(captor.capture());
        assertThat(captor.getValue().getObservaciones()).isEqualTo("observación");
    }

    @Test
    void crear_nombreYaExistente_lanzaArgumentoInvalidoYNoGuarda() {
        TipoServicioRequest request = request("ilustración", "Arte digital", null);
        when(tipoServicioRepository.existsByNombreIgnoreCase("ilustración")).thenReturn(true);

        assertThatThrownBy(() -> tipoServicioService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe un tipo de servicio con ese nombre.");

        verify(tipoServicioRepository, never()).save(any(TipoServicio.class));
    }

    // ---------------------------------------------------------------
    // modificar
    // ---------------------------------------------------------------

    @Test
    void modificar_idInexistente_lanzaNoSuchElementAntesDeValidarNombre() {
        TipoServicioRequest request = request("Ilustración", "Arte digital", null);
        when(tipoServicioRepository.findByIdTipoServicio(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoServicioService.modificar(99L, request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Tipo de servicio no encontrado: 99");

        verify(tipoServicioRepository, never()).save(any(TipoServicio.class));
    }

    @Test
    void modificar_nombreRepetidoEnOtroRegistro_lanzaArgumentoInvalido() {
        TipoServicioRequest request = request("Comisión", "Arte digital", null);
        when(tipoServicioRepository.findByIdTipoServicio(1L)).thenReturn(Optional.of(tipoServicio(1L, "Ilustración")));
        when(tipoServicioRepository.existsByNombreIgnoreCaseAndIdTipoServicioNot("Comisión", 1L)).thenReturn(true);

        assertThatThrownBy(() -> tipoServicioService.modificar(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe un tipo de servicio con ese nombre.");

        verify(tipoServicioRepository, never()).save(any(TipoServicio.class));
    }

    @Test
    void modificar_nombrePropioPermitido_actualizaYGuarda() {
        TipoServicio existente = tipoServicio(1L, "Ilustración");
        existente.setDescripcion("Descripción vieja");
        TipoServicioRequest request = request("  Ilustración digital  ", "  Descripción nueva  ", "  obs  ");

        when(tipoServicioRepository.findByIdTipoServicio(1L)).thenReturn(Optional.of(existente));
        when(tipoServicioRepository.existsByNombreIgnoreCaseAndIdTipoServicioNot("Ilustración digital", 1L))
                .thenReturn(false);
        when(tipoServicioRepository.save(any(TipoServicio.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoServicioDTO resultado = tipoServicioService.modificar(1L, request);

        assertThat(resultado.getNombre()).isEqualTo("Ilustración digital");
        assertThat(resultado.getDescripcion()).isEqualTo("Descripción nueva");
        assertThat(resultado.getObservaciones()).isEqualTo("obs");
        verify(tipoServicioRepository).save(existente);
    }

    @Test
    void modificar_conObservacionesNull_lasDejaEnNull() {
        TipoServicio existente = tipoServicio(1L, "Ilustración");
        existente.setObservaciones("obs anterior");
        TipoServicioRequest request = request("Ilustración", "Arte digital", null);

        when(tipoServicioRepository.findByIdTipoServicio(1L)).thenReturn(Optional.of(existente));
        when(tipoServicioRepository.existsByNombreIgnoreCaseAndIdTipoServicioNot("Ilustración", 1L))
                .thenReturn(false);
        when(tipoServicioRepository.save(any(TipoServicio.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoServicioDTO resultado = tipoServicioService.modificar(1L, request);

        assertThat(resultado.getObservaciones()).isNull();
    }

    // ---------------------------------------------------------------
    // darDeBaja / darDeAlta
    // ---------------------------------------------------------------

    @Test
    void darDeBaja_desactivaElTipo() {
        TipoServicio existente = tipoServicio(1L, "Ilustración");
        when(tipoServicioRepository.findByIdTipoServicio(1L)).thenReturn(Optional.of(existente));
        when(tipoServicioRepository.save(any(TipoServicio.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoServicioDTO resultado = tipoServicioService.darDeBaja(1L);

        assertThat(resultado.isActivo()).isFalse();
        verify(tipoServicioRepository).save(existente);
    }

    @Test
    void darDeAlta_activaElTipo() {
        TipoServicio existente = tipoServicio(1L, "Ilustración");
        existente.setActivo(false);
        when(tipoServicioRepository.findByIdTipoServicio(1L)).thenReturn(Optional.of(existente));
        when(tipoServicioRepository.save(any(TipoServicio.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoServicioDTO resultado = tipoServicioService.darDeAlta(1L);

        assertThat(resultado.isActivo()).isTrue();
        verify(tipoServicioRepository).save(existente);
    }

    @Test
    void darDeBaja_idInexistente_lanzaNoSuchElement() {
        when(tipoServicioRepository.findByIdTipoServicio(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoServicioService.darDeBaja(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Tipo de servicio no encontrado: 99");
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private TipoServicioRequest request(String nombre, String descripcion, String observaciones) {
        TipoServicioRequest request = new TipoServicioRequest();
        request.setNombre(nombre);
        request.setDescripcion(descripcion);
        request.setObservaciones(observaciones);
        return request;
    }

    private TipoServicio tipoServicio(Long id, String nombre) {
        return TipoServicio.builder()
                .idTipoServicio(id)
                .nombre(nombre)
                .descripcion("Descripción")
                .activo(true)
                .build();
    }
}
