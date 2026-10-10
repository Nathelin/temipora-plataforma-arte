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

import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.MedioDTO;
import com.temipora.backend_springboot.modelo.MedioRequest;
import com.temipora.backend_springboot.repositorios.MedioRepository;

@ExtendWith(MockitoExtension.class)
class MedioServiceTest {

    @Mock
    private MedioRepository medioRepository;

    @InjectMocks
    private MedioService medioService;

    // ---------------------------------------------------------------
    // listar / listarActivos
    // ---------------------------------------------------------------

    @Test
    void listar_mapeaEntidadesADtos() {
        when(medioRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(medio(1L, "Digital", true), medio(2L, "Tradicional", false)));

        List<MedioDTO> resultado = medioService.listar();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(MedioDTO::getNombre)
                .containsExactly("Digital", "Tradicional");
        assertThat(resultado.get(1).isActivo()).isFalse();
    }

    @Test
    void listarActivos_mapeaDtos() {
        when(medioRepository.findByActivoTrueOrderByNombreAsc())
                .thenReturn(List.of(medio(1L, "Digital", true)));

        List<MedioDTO> resultado = medioService.listarActivos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getIdMedio()).isEqualTo(1L);
    }

    // ---------------------------------------------------------------
    // crear
    // ---------------------------------------------------------------

    @Test
    void crear_nombreNoRepetido_guardaActivoYConCamposLimpios() {
        MedioRequest request = request("  Pintura  ", "  Al óleo  ", null);
        when(medioRepository.existsByNombreIgnoreCase("Pintura")).thenReturn(false);
        when(medioRepository.save(any(Medio.class))).thenAnswer(inv -> inv.getArgument(0));

        MedioDTO resultado = medioService.crear(request);

        ArgumentCaptor<Medio> captor = ArgumentCaptor.forClass(Medio.class);
        verify(medioRepository).save(captor.capture());
        Medio guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Pintura");
        assertThat(guardado.getDescripcion()).isEqualTo("Al óleo");
        assertThat(guardado.getObservaciones()).isNull();
        assertThat(guardado.isActivo()).isTrue();
        assertThat(resultado.getNombre()).isEqualTo("Pintura");
    }

    @Test
    void crear_nombreRepetido_lanzaError() {
        MedioRequest request = request("Digital", null, null);
        when(medioRepository.existsByNombreIgnoreCase("Digital")).thenReturn(true);

        assertThatThrownBy(() -> medioService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe un medio con ese nombre.");

        verify(medioRepository, never()).save(any(Medio.class));
    }

    // ---------------------------------------------------------------
    // modificar
    // ---------------------------------------------------------------

    @Test
    void modificar_ok_actualizaCampos() {
        Medio existente = medio(1L, "Digital", true);
        when(medioRepository.findByIdMedio(1L)).thenReturn(Optional.of(existente));
        when(medioRepository.existsByNombreIgnoreCaseAndIdMedioNot("Digital 2", 1L))
                .thenReturn(false);
        when(medioRepository.save(any(Medio.class))).thenAnswer(inv -> inv.getArgument(0));

        MedioDTO resultado = medioService.modificar(1L, request("Digital 2", "Nueva desc", "obs"));

        assertThat(resultado.getNombre()).isEqualTo("Digital 2");
        assertThat(resultado.getDescripcion()).isEqualTo("Nueva desc");
        assertThat(resultado.getObservaciones()).isEqualTo("obs");
    }

    @Test
    void modificar_nombreRepetidoEnOtro_lanzaError() {
        Medio existente = medio(1L, "Digital", true);
        when(medioRepository.findByIdMedio(1L)).thenReturn(Optional.of(existente));
        when(medioRepository.existsByNombreIgnoreCaseAndIdMedioNot("Tradicional", 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> medioService.modificar(1L, request("Tradicional", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe un medio con ese nombre.");
    }

    @Test
    void modificar_medioInexistente_lanzaNoSuchElement() {
        when(medioRepository.findByIdMedio(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medioService.modificar(99L, request("X", null, null)))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Medio no encontrado: 99");
    }

    // ---------------------------------------------------------------
    // darDeBaja / darDeAlta (ocultar conservando el uso)
    // ---------------------------------------------------------------

    @Test
    void darDeBaja_aunEnUso_desactivaConservandoElRegistro() {
        when(medioRepository.findByIdMedio(1L)).thenReturn(Optional.of(medio(1L, "Digital", true)));
        when(medioRepository.save(any(Medio.class))).thenAnswer(inv -> inv.getArgument(0));

        MedioDTO resultado = medioService.darDeBaja(1L);

        assertThat(resultado.isActivo()).isFalse();
    }

    @Test
    void darDeBaja_medioInexistente_lanzaNoSuchElement() {
        when(medioRepository.findByIdMedio(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medioService.darDeBaja(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void darDeAlta_reactivaElMedio() {
        when(medioRepository.findByIdMedio(1L)).thenReturn(Optional.of(medio(1L, "Digital", false)));
        when(medioRepository.save(any(Medio.class))).thenAnswer(inv -> inv.getArgument(0));

        MedioDTO resultado = medioService.darDeAlta(1L);

        assertThat(resultado.isActivo()).isTrue();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Medio medio(Long id, String nombre, boolean activo) {
        return Medio.builder()
                .idMedio(id)
                .nombre(nombre)
                .descripcion("descripcion")
                .observaciones(null)
                .activo(activo)
                .build();
    }

    private MedioRequest request(String nombre, String descripcion, String observaciones) {
        MedioRequest request = new MedioRequest();
        request.setNombre(nombre);
        request.setDescripcion(descripcion);
        request.setObservaciones(observaciones);
        return request;
    }
}
