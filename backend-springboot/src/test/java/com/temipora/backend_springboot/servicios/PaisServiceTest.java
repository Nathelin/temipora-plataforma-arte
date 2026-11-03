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

import com.temipora.backend_springboot.modelo.Pais;
import com.temipora.backend_springboot.modelo.PaisDTO;
import com.temipora.backend_springboot.modelo.PaisRequest;
import com.temipora.backend_springboot.repositorios.PaisRepository;

@ExtendWith(MockitoExtension.class)
class PaisServiceTest {

    @Mock
    private PaisRepository paisRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private PaisService paisService;

    // ---------------------------------------------------------------
    // listar / listarActivos
    // ---------------------------------------------------------------

    @Test
    void listar_mapeaEntidadesADtos() {
        when(paisRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(pais(1L, "Argentina", "AR", "+54", true),
                        pais(2L, "Brasil", "BR", "+55", false)));

        List<PaisDTO> resultado = paisService.listar();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(PaisDTO::getNombre)
                .containsExactly("Argentina", "Brasil");
        assertThat(resultado.get(1).isActivo()).isFalse();
        assertThat(resultado.get(0).getPrefijoTelefonico()).isEqualTo("+54");
    }

    @Test
    void listarActivos_mapeaDtos() {
        when(paisRepository.findByEstadoTrueOrderByNombreAsc())
                .thenReturn(List.of(pais(1L, "Argentina", "AR", "+54", true)));

        List<PaisDTO> resultado = paisService.listarActivos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getIdPais()).isEqualTo(1L);
    }

    // ---------------------------------------------------------------
    // crear
    // ---------------------------------------------------------------

    @Test
    void crear_datosValidos_guardaActivoYNormalizaCampos() {
        PaisRequest request = request("  argentina  ", "ar", " +54 ");
        when(paisRepository.existsByNombreIgnoreCase("argentina")).thenReturn(false);
        when(paisRepository.existsByCodigoIsoIgnoreCase("AR")).thenReturn(false);
        when(paisRepository.save(any(Pais.class))).thenAnswer(inv -> inv.getArgument(0));

        PaisDTO resultado = paisService.crear(request);

        ArgumentCaptor<Pais> captor = ArgumentCaptor.forClass(Pais.class);
        verify(paisRepository).save(captor.capture());
        Pais guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("argentina");
        assertThat(guardado.getCodigoIso()).isEqualTo("AR");
        assertThat(guardado.getPrefijoTelefonico()).isEqualTo("+54");
        assertThat(guardado.isEstado()).isTrue();
        assertThat(resultado.getCodigoIso()).isEqualTo("AR");
    }

    @Test
    void crear_nombreRepetido_lanzaError() {
        PaisRequest request = request("Argentina", "AR", "+54");
        when(paisRepository.existsByNombreIgnoreCase("Argentina")).thenReturn(true);

        assertThatThrownBy(() -> paisService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe un país con ese nombre.");

        verify(paisRepository, never()).save(any(Pais.class));
    }

    @Test
    void crear_codigoIsoRepetido_lanzaError() {
        PaisRequest request = request("Argentinia", "AR", "+54");
        when(paisRepository.existsByNombreIgnoreCase("Argentinia")).thenReturn(false);
        when(paisRepository.existsByCodigoIsoIgnoreCase("AR")).thenReturn(true);

        assertThatThrownBy(() -> paisService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe un país con ese código ISO.");

        verify(paisRepository, never()).save(any(Pais.class));
    }

    // ---------------------------------------------------------------
    // modificar
    // ---------------------------------------------------------------

    @Test
    void modificar_ok_actualizaCampos() {
        Pais existente = pais(1L, "Argentina", "AR", "+54", true);
        when(paisRepository.findByIdPais(1L)).thenReturn(Optional.of(existente));
        when(paisRepository.existsByNombreIgnoreCaseAndIdPaisNot("Argentinia", 1L)).thenReturn(false);
        when(paisRepository.existsByCodigoIsoIgnoreCaseAndIdPaisNot("AR", 1L)).thenReturn(false);
        when(paisRepository.save(any(Pais.class))).thenAnswer(inv -> inv.getArgument(0));

        PaisDTO resultado = paisService.modificar(1L, request("Argentinia", "ar", "+54"));

        assertThat(resultado.getNombre()).isEqualTo("Argentinia");
        assertThat(resultado.getCodigoIso()).isEqualTo("AR");
    }

    @Test
    void modificar_codigoRepetidoEnOtro_lanzaError() {
        Pais existente = pais(1L, "Argentina", "AR", "+54", true);
        when(paisRepository.findByIdPais(1L)).thenReturn(Optional.of(existente));
        when(paisRepository.existsByNombreIgnoreCaseAndIdPaisNot("Argentina", 1L)).thenReturn(false);
        when(paisRepository.existsByCodigoIsoIgnoreCaseAndIdPaisNot("BR", 1L)).thenReturn(true);

        assertThatThrownBy(() -> paisService.modificar(1L, request("Argentina", "BR", "+54")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe un país con ese código ISO.");
    }

    @Test
    void modificar_paisInexistente_lanzaNoSuchElement() {
        when(paisRepository.findByIdPais(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paisService.modificar(99L, request("X", "XX", "+1")))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("País no encontrado: 99");
    }

    // ---------------------------------------------------------------
    // darDeBaja / darDeAlta (ocultar conservando el uso)
    // ---------------------------------------------------------------

    @Test
    void darDeBaja_desactivaConservandoElRegistro() {
        when(paisRepository.findByIdPais(1L)).thenReturn(Optional.of(pais(1L, "Argentina", "AR", "+54", true)));
        when(paisRepository.save(any(Pais.class))).thenAnswer(inv -> inv.getArgument(0));

        PaisDTO resultado = paisService.darDeBaja(1L);

        assertThat(resultado.isActivo()).isFalse();
    }

    @Test
    void darDeBaja_paisInexistente_lanzaNoSuchElement() {
        when(paisRepository.findByIdPais(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paisService.darDeBaja(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void darDeAlta_reactivaElPais() {
        when(paisRepository.findByIdPais(1L)).thenReturn(Optional.of(pais(1L, "Argentina", "AR", "+54", false)));
        when(paisRepository.save(any(Pais.class))).thenAnswer(inv -> inv.getArgument(0));

        PaisDTO resultado = paisService.darDeAlta(1L);

        assertThat(resultado.isActivo()).isTrue();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Pais pais(Long id, String nombre, String codigoIso, String prefijo, boolean estado) {
        return Pais.builder()
                .idPais(id)
                .nombre(nombre)
                .codigoIso(codigoIso)
                .prefijoTelefonico(prefijo)
                .estado(estado)
                .build();
    }

    private PaisRequest request(String nombre, String codigoIso, String prefijo) {
        PaisRequest request = new PaisRequest();
        request.setNombre(nombre);
        request.setCodigoIso(codigoIso);
        request.setPrefijoTelefonico(prefijo);
        return request;
    }
}
