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
import com.temipora.backend_springboot.modelo.TipoArte;
import com.temipora.backend_springboot.modelo.TipoArteDTO;
import com.temipora.backend_springboot.modelo.TipoArteRequest;
import com.temipora.backend_springboot.repositorios.MedioRepository;
import com.temipora.backend_springboot.repositorios.TipoArteRepository;

@ExtendWith(MockitoExtension.class)
class TipoArteServiceTest {

    @Mock
    private TipoArteRepository tipoArteRepository;

    @Mock
    private MedioRepository medioRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private TipoArteService tipoArteService;

    // ---------------------------------------------------------------
    // listar / listarActivos
    // ---------------------------------------------------------------

    @Test
    void listar_mapeaEntidadesADtos() {
        when(tipoArteRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(tipo(1L, "Dibujo digital", true), tipo(2L, "Dibujo tradicional", false)));

        List<TipoArteDTO> resultado = tipoArteService.listar();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(TipoArteDTO::getNombre)
                .containsExactly("Dibujo digital", "Dibujo tradicional");
        assertThat(resultado.get(0).getIdMedio()).isEqualTo(1L);
        assertThat(resultado.get(0).getNombreMedio()).isEqualTo("Digital");
        assertThat(resultado.get(1).isActivo()).isFalse();
    }

    @Test
    void listarActivos_mapeaDtos() {
        when(tipoArteRepository.findByActivoTrueOrderByNombreAsc())
                .thenReturn(List.of(tipo(1L, "Dibujo digital", true)));

        List<TipoArteDTO> resultado = tipoArteService.listarActivos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getIdTipoArte()).isEqualTo(1L);
    }

    // ---------------------------------------------------------------
    // crear
    // ---------------------------------------------------------------

    @Test
    void crear_nombreNoRepetido_guardaActivoYConCamposLimpios() {
        TipoArteRequest request = request("  Ilustracion  ", "  Digital  ", 1L, null);
        when(tipoArteRepository.existsByNombreIgnoreCase("Ilustracion")).thenReturn(false);
        when(medioRepository.findByIdMedio(1L)).thenReturn(Optional.of(medio(1L, "Digital", true)));
        when(tipoArteRepository.save(any(TipoArte.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoArteDTO resultado = tipoArteService.crear(request);

        ArgumentCaptor<TipoArte> captor = ArgumentCaptor.forClass(TipoArte.class);
        verify(tipoArteRepository).save(captor.capture());
        TipoArte guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Ilustracion");
        assertThat(guardado.getMedio().getIdMedio()).isEqualTo(1L);
        assertThat(guardado.getObservaciones()).isNull();
        assertThat(guardado.isActivo()).isTrue();
        assertThat(resultado.getNombre()).isEqualTo("Ilustracion");
    }

    @Test
    void crear_nombreRepetido_lanzaError() {
        TipoArteRequest request = request("Digital", "desc", 1L, null);
        when(tipoArteRepository.existsByNombreIgnoreCase("Digital")).thenReturn(true);

        assertThatThrownBy(() -> tipoArteService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe un tipo de arte con ese nombre.");

        verify(tipoArteRepository, never()).save(any(TipoArte.class));
    }

    @Test
    void crear_medioDadoDeBaja_lanzaError() {
        TipoArteRequest request = request("Ilustracion", "desc", 3L, null);
        when(tipoArteRepository.existsByNombreIgnoreCase("Ilustracion")).thenReturn(false);
        when(medioRepository.findByIdMedio(3L)).thenReturn(Optional.of(medio(3L, "Vidrio", false)));

        assertThatThrownBy(() -> tipoArteService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El medio 'Vidrio' fue dado de baja y no puede asignarse a tipos de arte nuevos.");

        verify(tipoArteRepository, never()).save(any(TipoArte.class));
    }

    // ---------------------------------------------------------------
    // modificar
    // ---------------------------------------------------------------

    @Test
    void modificar_ok_actualizaCamposYPermiteCambiarElMedio() {
        TipoArte existente = tipo(1L, "Dibujo digital", false);
        when(tipoArteRepository.findByIdTipoArte(1L)).thenReturn(Optional.of(existente));
        when(tipoArteRepository.existsByNombreIgnoreCaseAndIdTipoArteNot("Ilustracion", 1L))
                .thenReturn(false);
        when(medioRepository.findByIdMedio(2L)).thenReturn(Optional.of(medio(2L, "Tradicional", true)));
        when(tipoArteRepository.save(any(TipoArte.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoArteDTO resultado = tipoArteService
                .modificar(1L, request("Ilustracion", "Descripcion nueva", 2L, "obs"));

        assertThat(resultado.getNombre()).isEqualTo("Ilustracion");
        assertThat(resultado.getIdMedio()).isEqualTo(2L);
        assertThat(resultado.getNombreMedio()).isEqualTo("Tradicional");
        assertThat(resultado.getObservaciones()).isEqualTo("obs");
        assertThat(resultado.isActivo()).isFalse();
    }

    @Test
    void modificar_nombreRepetidoEnOtra_lanzaError() {
        TipoArte existente = tipo(1L, "Dibujo digital", true);
        when(tipoArteRepository.findByIdTipoArte(1L)).thenReturn(Optional.of(existente));
        when(tipoArteRepository.existsByNombreIgnoreCaseAndIdTipoArteNot("Tradicional", 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> tipoArteService.modificar(1L, request("Tradicional", "desc", 2L, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe un tipo de arte con ese nombre.");
    }

    @Test
    void modificar_tipoInexistente_lanzaNoSuchElement() {
        when(tipoArteRepository.findByIdTipoArte(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoArteService.modificar(99L, request("X", "desc", 1L, null)))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Tipo de arte no encontrado: 99");
    }

    @Test
    void modificar_cambiaAMedioDadoDeBaja_lanzaError() {
        TipoArte existente = tipo(1L, "Dibujo digital", true);
        when(tipoArteRepository.findByIdTipoArte(1L)).thenReturn(Optional.of(existente));
        when(tipoArteRepository.existsByNombreIgnoreCaseAndIdTipoArteNot("Ilustracion", 1L))
                .thenReturn(false);
        when(medioRepository.findByIdMedio(3L)).thenReturn(Optional.of(medio(3L, "Vidrio", false)));

        assertThatThrownBy(() -> tipoArteService.modificar(1L, request("Ilustracion", "desc", 3L, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El medio 'Vidrio' fue dado de baja y no puede asignarse.");
    }

    @Test
    void modificar_conserveSuMedioInactivo_loPermite() {
        TipoArte existente = tipo(1L, "Dibujo digital", true);
        existente.setMedio(medio(3L, "Vidrio", false));
        when(tipoArteRepository.findByIdTipoArte(1L)).thenReturn(Optional.of(existente));
        when(tipoArteRepository.existsByNombreIgnoreCaseAndIdTipoArteNot("Ilustracion", 1L))
                .thenReturn(false);
        when(medioRepository.findByIdMedio(3L)).thenReturn(Optional.of(medio(3L, "Vidrio", false)));
        when(tipoArteRepository.save(any(TipoArte.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoArteDTO resultado = tipoArteService.modificar(1L, request("Ilustracion", "desc", 3L, null));

        assertThat(resultado.getIdMedio()).isEqualTo(3L);
        assertThat(resultado.isActivo()).isTrue();
    }

    // ---------------------------------------------------------------
    // darDeBaja / darDeAlta (ocultar conservando el uso)
    // ---------------------------------------------------------------

    @Test
    void darDeBaja_aunConObrasYMateriales_desactiva() {
        when(tipoArteRepository.findByIdTipoArte(1L)).thenReturn(Optional.of(tipo(1L, "Dibujo digital", true)));
        when(tipoArteRepository.save(any(TipoArte.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoArteDTO resultado = tipoArteService.darDeBaja(1L);

        assertThat(resultado.isActivo()).isFalse();
    }

    @Test
    void darDeBaja_tipoInexistente_lanzaNoSuchElement() {
        when(tipoArteRepository.findByIdTipoArte(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoArteService.darDeBaja(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void darDeAlta_reactivaElTipo() {
        when(tipoArteRepository.findByIdTipoArte(1L)).thenReturn(Optional.of(tipo(1L, "Dibujo digital", false)));
        when(tipoArteRepository.save(any(TipoArte.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoArteDTO resultado = tipoArteService.darDeAlta(1L);

        assertThat(resultado.isActivo()).isTrue();
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Medio medio(Long id, String nombre, boolean activo) {
        return Medio.builder()
                .idMedio(id)
                .nombre(nombre)
                .activo(activo)
                .build();
    }

    private TipoArte tipo(Long id, String nombre, boolean activo) {
        return TipoArte.builder()
                .idTipoArte(id)
                .nombre(nombre)
                .descripcion("descripcion")
                .medio(medio(1L, "Digital", true))
                .observaciones(null)
                .activo(activo)
                .build();
    }

    private TipoArteRequest request(String nombre, String descripcion, Long idMedio, String observaciones) {
        TipoArteRequest request = new TipoArteRequest();
        request.setNombre(nombre);
        request.setDescripcion(descripcion);
        request.setIdMedio(idMedio);
        request.setObservaciones(observaciones);
        return request;
    }
}
