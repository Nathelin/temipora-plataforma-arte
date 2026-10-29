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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.AmbitoResenia;
import com.temipora.backend_springboot.modelo.CriterioResenia;
import com.temipora.backend_springboot.modelo.CriterioReseniaDTO;
import com.temipora.backend_springboot.modelo.CriterioReseniaRequest;
import com.temipora.backend_springboot.modelo.EscalaCriterio;
import com.temipora.backend_springboot.modelo.OpcionCriterio;
import com.temipora.backend_springboot.modelo.OpcionCriterioRequest;
import com.temipora.backend_springboot.modelo.PolaridadOpcion;
import com.temipora.backend_springboot.repositorios.CriterioReseniaRepository;
import com.temipora.backend_springboot.repositorios.OpcionCriterioRepository;
import com.temipora.backend_springboot.repositorios.ReseniaOpcionRepository;
import com.temipora.backend_springboot.repositorios.ReseniaValorRepository;

@ExtendWith(MockitoExtension.class)
class CriterioReseniaServiceTest {

    @Mock
    private CriterioReseniaRepository criterioRepository;

    @Mock
    private OpcionCriterioRepository opcionRepository;

    @Mock
    private ReseniaValorRepository valorRepository;

    @Mock
    private ReseniaOpcionRepository reseniaOpcionRepository;

    @InjectMocks
    private CriterioReseniaService criterioReseniaService;

    @Test
    void crear_nombreLibre_guardaActivoYOrdenPorDefecto() {
        when(criterioRepository.existsByNombreIgnoreCaseAndAmbito(
                "Puntualidad", AmbitoResenia.SERVICIO)).thenReturn(false);
        when(criterioRepository.save(any(CriterioResenia.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        CriterioReseniaDTO resultado = criterioReseniaService
            .crear(request("  Puntualidad  ", "  Llegó en fecha  ",
                AmbitoResenia.SERVICIO, EscalaCriterio.ESTRELLAS, true, null));

        assertThat(resultado.getNombre()).isEqualTo("Puntualidad");
        assertThat(resultado.getDescripcion()).isEqualTo("Llegó en fecha");
        assertThat(resultado.isActivo()).isTrue();
        assertThat(resultado.getOrden()).isZero();
        assertThat(resultado.isObligatorio()).isTrue();
    }

    @Test
    void crear_mismoNombreEnOtroAmbito_permite() {
        when(criterioRepository.existsByNombreIgnoreCaseAndAmbito(
                "Comunicación", AmbitoResenia.CLIENTE)).thenReturn(false);
        when(criterioRepository.save(any(CriterioResenia.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        CriterioReseniaDTO resultado = criterioReseniaService
            .crear(request("Comunicación", "Trato", AmbitoResenia.CLIENTE,
                EscalaCriterio.ESTRELLAS, true, 2));

        assertThat(resultado.getAmbito()).isEqualTo(AmbitoResenia.CLIENTE);
        assertThat(resultado.getOrden()).isEqualTo(2);
    }

    @Test
    void crear_nombreRepetidoEnElAmbito_lanzaError() {
        when(criterioRepository.existsByNombreIgnoreCaseAndAmbito(
                "Puntualidad", AmbitoResenia.SERVICIO)).thenReturn(true);

        assertThatThrownBy(() -> criterioReseniaService.crear(
                request("Puntualidad", "desc", AmbitoResenia.SERVICIO,
                    EscalaCriterio.ESTRELLAS, false, 0)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ya existe un criterio");

        verify(criterioRepository, never()).save(any(CriterioResenia.class));
    }

    @Test
    void modificar_conReseniasYCambioDeAmbito_lanzaError() {
        CriterioResenia existente = criterio(1L, EscalaCriterio.ESTRELLAS);
        when(criterioRepository.findByIdCriterioResenia(1L))
            .thenReturn(Optional.of(existente));
        when(valorRepository.contarReseniasPorCriterio(1L)).thenReturn(2L);

        assertThatThrownBy(() -> criterioReseniaService.modificar(1L,
                request("Puntualidad", "desc", AmbitoResenia.CLIENTE,
                    EscalaCriterio.ESTRELLAS, false, 0)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ámbito");
    }

    @Test
    void modificar_conReseniasYCambioDeEscala_permiteConservarElHistorial() {
        CriterioResenia existente = criterio(1L, EscalaCriterio.ESTRELLAS);
        when(criterioRepository.findByIdCriterioResenia(1L))
            .thenReturn(Optional.of(existente));
        when(valorRepository.contarReseniasPorCriterio(1L)).thenReturn(2L);
        when(criterioRepository
            .existsByNombreIgnoreCaseAndAmbitoAndIdCriterioReseniaNot(
                "Puntualidad", AmbitoResenia.SERVICIO, 1L)).thenReturn(false);
        when(criterioRepository.save(any(CriterioResenia.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        CriterioReseniaDTO dto = criterioReseniaService.modificar(1L,
            request("Puntualidad", "desc", AmbitoResenia.SERVICIO,
                EscalaCriterio.OPCIONES_MULTIPLES, false, 0));

        assertThat(dto.getEscala()).isEqualTo(EscalaCriterio.OPCIONES_MULTIPLES);
    }

    @Test
    void modificar_conOpcionesSinResenias_permiteVolverAEstrellas() {
        CriterioResenia existente = criterio(1L, EscalaCriterio.OPCIONES_MULTIPLES);
        existente.agregarOpcion(OpcionCriterio.builder()
            .idOpcionCriterio(5L).etiqueta("A tiempo").build());
        when(criterioRepository.findByIdCriterioResenia(1L))
            .thenReturn(Optional.of(existente));
        when(criterioRepository
            .existsByNombreIgnoreCaseAndAmbitoAndIdCriterioReseniaNot(
                "Puntualidad", AmbitoResenia.SERVICIO, 1L)).thenReturn(false);
        when(criterioRepository.save(any(CriterioResenia.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        CriterioReseniaDTO dto = criterioReseniaService.modificar(1L,
            request("Puntualidad", "desc", AmbitoResenia.SERVICIO,
                EscalaCriterio.ESTRELLAS, false, 0));

        assertThat(dto.getEscala()).isEqualTo(EscalaCriterio.ESTRELLAS);
    }

    @Test
    void modificar_sinResenias_actualizaCamposYNoTocaEstado() {
        CriterioResenia existente = criterio(1L, EscalaCriterio.ESTRELLAS);
        existente.setActivo(false);
        when(criterioRepository.findByIdCriterioResenia(1L))
            .thenReturn(Optional.of(existente));
        when(criterioRepository
            .existsByNombreIgnoreCaseAndAmbitoAndIdCriterioReseniaNot(
                "Nuevo", AmbitoResenia.SERVICIO, 1L)).thenReturn(false);
        when(criterioRepository.save(any(CriterioResenia.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        CriterioReseniaDTO resultado = criterioReseniaService.modificar(1L,
            request("Nuevo", "Otra descripción", AmbitoResenia.SERVICIO,
                EscalaCriterio.ESTRELLAS, false, 3));

        assertThat(resultado.getNombre()).isEqualTo("Nuevo");
        assertThat(resultado.getOrden()).isEqualTo(3);
        assertThat(resultado.isActivo()).isFalse();
    }

    @Test
    void crearOpcion_enCriterioDeEstrellas_lanzaError() {
        when(criterioRepository.findByIdCriterioResenia(1L))
            .thenReturn(Optional.of(criterio(1L, EscalaCriterio.ESTRELLAS)));

        assertThatThrownBy(() -> criterioReseniaService
                .crearOpcion(1L, opcionRequest("A tiempo")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("opciones múltiples");
    }

    @Test
    void crearOpcion_ok_agregaOpcionAlCriterio() {
        CriterioResenia existente = criterio(1L, EscalaCriterio.OPCIONES_MULTIPLES);
        when(criterioRepository.findByIdCriterioResenia(1L))
            .thenReturn(Optional.of(existente));
        when(opcionRepository
            .existsByCriterioIdCriterioReseniaAndEtiquetaIgnoreCase(
                1L, "A tiempo")).thenReturn(false);
        when(criterioRepository.save(any(CriterioResenia.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        CriterioReseniaDTO resultado = criterioReseniaService
            .crearOpcion(1L, opcionRequest("A tiempo"));

        assertThat(resultado.getOpciones()).hasSize(1);
        assertThat(resultado.getOpciones().get(0).getEtiqueta()).isEqualTo("A tiempo");
        assertThat(resultado.getOpciones().get(0).getPolaridad())
            .isEqualTo(PolaridadOpcion.POSITIVA);
    }

    @Test
    void crearOpcion_etiquetaRepetida_lanzaError() {
        when(criterioRepository.findByIdCriterioResenia(1L))
            .thenReturn(Optional.of(criterio(1L, EscalaCriterio.OPCIONES_MULTIPLES)));
        when(opcionRepository
            .existsByCriterioIdCriterioReseniaAndEtiquetaIgnoreCase(
                1L, "A tiempo")).thenReturn(true);

        assertThatThrownBy(() -> criterioReseniaService
                .crearOpcion(1L, opcionRequest("A tiempo")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ya existe una opción");
    }

    @Test
    void darDeBajaOpcion_ultimaActivaConResenias_lanzaError() {
        CriterioResenia existente = criterio(1L, EscalaCriterio.OPCIONES_MULTIPLES);
        OpcionCriterio opcion = OpcionCriterio.builder()
            .idOpcionCriterio(5L).etiqueta("A tiempo").activo(true).build();
        existente.agregarOpcion(opcion);
        when(criterioRepository.findByIdCriterioResenia(1L))
            .thenReturn(Optional.of(existente));
        when(opcionRepository
            .findByIdOpcionCriterioAndCriterioIdCriterioResenia(5L, 1L))
            .thenReturn(Optional.of(opcion));
        when(reseniaOpcionRepository
            .existsByOpcionCriterioIdCriterioResenia(1L)).thenReturn(true);
        when(opcionRepository
            .countByCriterioIdCriterioReseniaAndActivoTrue(1L)).thenReturn(1L);

        assertThatThrownBy(() -> criterioReseniaService.darDeBajaOpcion(1L, 5L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("última opción activa");

        verify(opcionRepository, never()).save(any(OpcionCriterio.class));
    }

    @Test
    void darDeBajaOpcion_conOtrasActivas_desactiva() {
        CriterioResenia existente = criterio(1L, EscalaCriterio.OPCIONES_MULTIPLES);
        OpcionCriterio opcion = OpcionCriterio.builder()
            .idOpcionCriterio(5L).etiqueta("A tiempo").activo(true).build();
        existente.agregarOpcion(opcion);
        when(criterioRepository.findByIdCriterioResenia(1L))
            .thenReturn(Optional.of(existente));
        when(opcionRepository
            .findByIdOpcionCriterioAndCriterioIdCriterioResenia(5L, 1L))
            .thenReturn(Optional.of(opcion));
        when(reseniaOpcionRepository
            .existsByOpcionCriterioIdCriterioResenia(1L)).thenReturn(true);
        when(opcionRepository
            .countByCriterioIdCriterioReseniaAndActivoTrue(1L)).thenReturn(2L);

        CriterioReseniaDTO resultado = criterioReseniaService
            .darDeBajaOpcion(1L, 5L);

        assertThat(opcion.isActivo()).isFalse();
        assertThat(resultado.getOpciones().get(0).isActivo()).isFalse();
    }

    @Test
    void buscarCriterioInexistente_lanzaNoSuchElement() {
        when(criterioRepository.findByIdCriterioResenia(99L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> criterioReseniaService.darDeBaja(99L))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("99");
    }

    @Test
    void listarActivos_delegaEnElRepositorioPorAmbito() {
        when(criterioRepository
            .findByActivoTrueAndAmbitoOrderByOrdenAscNombreAsc(AmbitoResenia.SERVICIO))
            .thenReturn(List.of(criterio(1L, EscalaCriterio.ESTRELLAS)));

        List<CriterioReseniaDTO> resultado =
            criterioReseniaService.listarActivos(AmbitoResenia.SERVICIO);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getAmbito()).isEqualTo(AmbitoResenia.SERVICIO);
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private CriterioResenia criterio(Long id, EscalaCriterio escala) {
        return CriterioResenia.builder()
            .idCriterioResenia(id)
            .nombre("Puntualidad")
            .descripcion("descripcion")
            .ambito(AmbitoResenia.SERVICIO)
            .escala(escala)
            .obligatorio(false)
            .orden(0)
            .activo(true)
            .build();
    }

    private CriterioReseniaRequest request(
            String nombre, String descripcion, AmbitoResenia ambito,
            EscalaCriterio escala, boolean obligatorio, Integer orden) {
        CriterioReseniaRequest request = new CriterioReseniaRequest();
        request.setNombre(nombre);
        request.setDescripcion(descripcion);
        request.setAmbito(ambito);
        request.setEscala(escala);
        request.setObligatorio(obligatorio);
        request.setOrden(orden);
        return request;
    }

    private OpcionCriterioRequest opcionRequest(String etiqueta) {
        OpcionCriterioRequest request = new OpcionCriterioRequest();
        request.setEtiqueta(etiqueta);
        return request;
    }
}
