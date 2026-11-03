package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.temipora.backend_springboot.modelo.CategoriaEtiqueta;
import com.temipora.backend_springboot.modelo.CategoriaEtiquetaDTO;
import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.EtiquetaDTO;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;
import com.temipora.backend_springboot.modelo.EtiquetaRequest;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CategoriaEtiquetaRepository;
import com.temipora.backend_springboot.repositorios.EtiquetaRepository;
import com.temipora.backend_springboot.repositorios.ImagenEtiquetaRepository;

@ExtendWith(MockitoExtension.class)
class EtiquetaServiceTest {

    @Mock
    private EtiquetaRepository etiquetaRepository;

    @Mock
    private CategoriaEtiquetaRepository categoriaEtiquetaRepository;

    @Mock
    private ImagenEtiquetaRepository imagenEtiquetaRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private EtiquetaService etiquetaService;

    private final CategoriaEtiqueta estilo = categoria(1L, "Estilo", true);
    private final Usuario admin = usuario(10L, "admin");
    private final Usuario artista = usuario(20L, "dibujito");

    // ---------------------------------------------------------------
    // listarAdmin
    // ---------------------------------------------------------------

    @Test
    void listarAdmin_sinFiltros_devuelveTodasOrdenadasPorNombre() {
        when(etiquetaRepository.findAll(any(Pageable.class)))
                .thenReturn(pagina(List.of(etiqueta(1L, "Acuarela", EtiquetaEstado.VALIDA, estilo, admin, 0))));

        PaginaDTO<EtiquetaDTO> resultado = etiquetaService.listarAdmin(null, null, 0, 10);

        assertThat(resultado.getContenido()).hasSize(1);
        assertThat(resultado.getContenido().get(0).getNombre()).isEqualTo("Acuarela");
        assertThat(resultado.getContenido().get(0).getNombreCategoria()).isEqualTo("Estilo");
        assertThat(resultado.getTotalElementos()).isEqualTo(1);
    }

    @Test
    void listarAdmin_pendientes_consultaOrdenadasPorFecha() {
        when(etiquetaRepository.findByEstado(eq(EtiquetaEstado.PENDIENTE), any(Pageable.class)))
                .thenReturn(pagina(List.of(etiqueta(1L, "Pixel", EtiquetaEstado.PENDIENTE, estilo, artista, 1))));

        PaginaDTO<EtiquetaDTO> resultado = etiquetaService.listarAdmin(EtiquetaEstado.PENDIENTE, null, 0, 10);

        assertThat(resultado.getContenido()).extracting(EtiquetaDTO::getEstado)
                .containsExactly(EtiquetaEstado.PENDIENTE);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(etiquetaRepository).findByEstado(eq(EtiquetaEstado.PENDIENTE), captor.capture());
        assertThat(captor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.ASC, "fechaCreacion"));
    }

    @Test
    void listarAdmin_rechazadas_consultaOrdenadasPorDemanda() {
        when(etiquetaRepository.findByEstado(eq(EtiquetaEstado.RECHAZADA), any(Pageable.class)))
                .thenReturn(pagina(List.of(etiqueta(1L, "Manga", EtiquetaEstado.RECHAZADA, estilo, artista, 5))));

        PaginaDTO<EtiquetaDTO> resultado = etiquetaService.listarAdmin(EtiquetaEstado.RECHAZADA, null, 0, 10);

        assertThat(resultado.getContenido().get(0).getVecesSolicitada()).isEqualTo(5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(etiquetaRepository).findByEstado(eq(EtiquetaEstado.RECHAZADA), captor.capture());
        assertThat(captor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "vecesSolicitada"));
    }

    @Test
    void listarAdmin_conEstadoYCategoria_filtraPorAmbos() {
        when(etiquetaRepository.findByEstadoAndCategoriaIdCategoriaEtiqueta(
                eq(EtiquetaEstado.VALIDA), eq(1L), any(Pageable.class)))
                .thenReturn(pagina(List.of(etiqueta(1L, "Lienzo", EtiquetaEstado.VALIDA, estilo, admin, 0))));

        PaginaDTO<EtiquetaDTO> resultado = etiquetaService.listarAdmin(EtiquetaEstado.VALIDA, 1L, 0, 10);

        assertThat(resultado.getContenido()).hasSize(1);
    }

    @Test
    void listarAdmin_soloConCategoria_consultaPorCategoria() {
        when(etiquetaRepository.findByCategoriaIdCategoriaEtiqueta(eq(1L), any(Pageable.class)))
                .thenReturn(pagina(List.of(etiqueta(1L, "Lienzo", EtiquetaEstado.VALIDA, estilo, admin, 0))));

        PaginaDTO<EtiquetaDTO> resultado = etiquetaService.listarAdmin(null, 1L, 0, 10);

        assertThat(resultado.getContenido()).hasSize(1);
    }

    @Test
    void listarAdmin_validadas_consultaOrdenadasPorNombre() {
        when(etiquetaRepository.findByEstado(eq(EtiquetaEstado.VALIDA), any(Pageable.class)))
                .thenReturn(pagina(List.of(etiqueta(1L, "Acuarela", EtiquetaEstado.VALIDA, estilo, admin, 0))));

        PaginaDTO<EtiquetaDTO> resultado = etiquetaService.listarAdmin(EtiquetaEstado.VALIDA, null, 0, 10);

        assertThat(resultado.getContenido()).hasSize(1);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(etiquetaRepository).findByEstado(eq(EtiquetaEstado.VALIDA), captor.capture());
        assertThat(captor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.ASC, "nombre"));
    }

    @Test
    void listarAdmin_acotaPaginaYTamanio() {
        when(etiquetaRepository.findAll(any(Pageable.class)))
                .thenReturn(pagina(List.of()));

        etiquetaService.listarAdmin(null, null, -5, 5000);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(etiquetaRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void contarPendientes_delegaEnElRepositorio() {
        when(etiquetaRepository.countByEstado(EtiquetaEstado.PENDIENTE)).thenReturn(7L);

        assertThat(etiquetaService.contarPendientes()).isEqualTo(7);
    }

    // ---------------------------------------------------------------
    // crearDesdeAdmin
    // ---------------------------------------------------------------

    @Test
    void crearDesdeAdmin_ok_creaValidaDemandaCero() {
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela")).thenReturn(Optional.empty());
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(1L)).thenReturn(Optional.of(estilo));
        when(etiquetaRepository.save(any(Etiqueta.class))).thenAnswer(inv -> inv.getArgument(0));

        EtiquetaDTO resultado = etiquetaService.crearDesdeAdmin(admin, request("  Acuarela  ", 1L));

        ArgumentCaptor<Etiqueta> captor = ArgumentCaptor.forClass(Etiqueta.class);
        verify(etiquetaRepository).save(captor.capture());
        Etiqueta guardada = captor.getValue();
        assertThat(guardada.getNombre()).isEqualTo("Acuarela");
        assertThat(guardada.getEstado()).isEqualTo(EtiquetaEstado.VALIDA);
        assertThat(guardada.getVecesSolicitada()).isZero();
        assertThat(guardada.getCreadoPor()).isSameAs(admin);
        assertThat(guardada.getFechaCreacion()).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo(EtiquetaEstado.VALIDA);
    }

    @Test
    void crearDesdeAdmin_nombreYaExistente_lanzaError() {
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela"))
                .thenReturn(Optional.of(etiqueta(1L, "Acuarela", EtiquetaEstado.VALIDA, estilo, admin, 0)));

        assertThatThrownBy(() -> etiquetaService.crearDesdeAdmin(admin, request("Acuarela", 1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe una etiqueta válida con ese nombre.");

        verify(etiquetaRepository, never()).save(any(Etiqueta.class));
    }

    @Test
    void crearDesdeAdmin_categoriaInexistente_lanzaNoSuchElement() {
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela")).thenReturn(Optional.empty());
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> etiquetaService.crearDesdeAdmin(admin, request("Acuarela", 99L)))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Categoría de etiqueta no encontrada: 99");
    }

    @Test
    void crearDesdeAdmin_categoriaInactiva_lanzaError() {
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela")).thenReturn(Optional.empty());
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(2L))
                .thenReturn(Optional.of(categoria(2L, "Vieja", false)));

        assertThatThrownBy(() -> etiquetaService.crearDesdeAdmin(admin, request("Acuarela", 2L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La categoría seleccionada está inactiva.");
    }

    @Test
    void crearDesdeAdmin_conObservaciones_lasTrimea() {
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela")).thenReturn(Optional.empty());
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(1L)).thenReturn(Optional.of(estilo));
        when(etiquetaRepository.save(any(Etiqueta.class))).thenAnswer(inv -> inv.getArgument(0));

        EtiquetaRequest conObservaciones = request("Acuarela", 1L);
        conObservaciones.setObservaciones("  nota del admin  ");
        etiquetaService.crearDesdeAdmin(admin, conObservaciones);

        ArgumentCaptor<Etiqueta> captor = ArgumentCaptor.forClass(Etiqueta.class);
        verify(etiquetaRepository).save(captor.capture());
        assertThat(captor.getValue().getObservaciones()).isEqualTo("nota del admin");
    }

    @Test
    void crearDesdeAdmin_nombreRepetidoEnPendiente_lanzaError() {
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela"))
                .thenReturn(Optional.of(etiqueta(1L, "Acuarela", EtiquetaEstado.PENDIENTE, estilo, artista, 1)));

        assertThatThrownBy(() -> etiquetaService.crearDesdeAdmin(admin, request("Acuarela", 1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe una etiqueta pendiente de evaluación con ese nombre.");
    }

    @Test
    void crearDesdeAdmin_nombreRepetidoEnRechazada_lanzaError() {
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela"))
                .thenReturn(Optional.of(etiqueta(1L, "Acuarela", EtiquetaEstado.RECHAZADA, estilo, artista, 2)));

        assertThatThrownBy(() -> etiquetaService.crearDesdeAdmin(admin, request("Acuarela", 1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe una etiqueta rechazada con ese nombre.");
    }

    @Test
    void crearDesdeAdmin_nombreRepetidoEnInactiva_lanzaError() {
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela"))
                .thenReturn(Optional.of(etiqueta(1L, "Acuarela", EtiquetaEstado.INACTIVA, estilo, admin, 0)));

        assertThatThrownBy(() -> etiquetaService.crearDesdeAdmin(admin, request("Acuarela", 1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe una etiqueta dada de baja con ese nombre.");
    }

    // ---------------------------------------------------------------
    // modificar
    // ---------------------------------------------------------------

    @Test
    void modificar_ok_conservaEstadoYSuDemanda() {
        Etiqueta existente = etiqueta(1L, "Acuarela", EtiquetaEstado.RECHAZADA, estilo, artista, 4);
        when(etiquetaRepository.findByIdEtiqueta(1L)).thenReturn(Optional.of(existente));
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela Nueva")).thenReturn(Optional.empty());
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(1L)).thenReturn(Optional.of(estilo));
        when(etiquetaRepository.save(any(Etiqueta.class))).thenAnswer(inv -> inv.getArgument(0));

        EtiquetaDTO resultado = etiquetaService.modificar(1L, request("Acuarela Nueva", 1L));

        assertThat(resultado.getNombre()).isEqualTo("Acuarela Nueva");
        assertThat(resultado.getEstado()).isEqualTo(EtiquetaEstado.RECHAZADA);
        assertThat(resultado.getVecesSolicitada()).isEqualTo(4);
    }

    @Test
    void modificar_categoriaInexistente_lanzaNoSuchElement() {
        Etiqueta existente = etiqueta(1L, "Acuarela", EtiquetaEstado.VALIDA, estilo, admin, 0);
        when(etiquetaRepository.findByIdEtiqueta(1L)).thenReturn(Optional.of(existente));
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela")).thenReturn(Optional.of(existente));
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> etiquetaService.modificar(1L, request("Acuarela", 99L)))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Categoría de etiqueta no encontrada: 99");
    }

    @Test
    void modificar_conObservaciones_lasTrimea() {
        Etiqueta existente = etiqueta(1L, "Acuarela", EtiquetaEstado.VALIDA, estilo, admin, 0);
        when(etiquetaRepository.findByIdEtiqueta(1L)).thenReturn(Optional.of(existente));
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela")).thenReturn(Optional.of(existente));
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(1L)).thenReturn(Optional.of(estilo));
        when(etiquetaRepository.save(any(Etiqueta.class))).thenAnswer(inv -> inv.getArgument(0));

        EtiquetaRequest conObservaciones = request("Acuarela", 1L);
        conObservaciones.setObservaciones("  revisión  ");
        etiquetaService.modificar(1L, conObservaciones);

        ArgumentCaptor<Etiqueta> captor = ArgumentCaptor.forClass(Etiqueta.class);
        verify(etiquetaRepository).save(captor.capture());
        assertThat(captor.getValue().getObservaciones()).isEqualTo("revisión");
    }

    @Test
    void modificar_nombreRepetidoEnOtra_lanzaError() {
        Etiqueta existente = etiqueta(1L, "Acuarela", EtiquetaEstado.VALIDA, estilo, admin, 0);
        when(etiquetaRepository.findByIdEtiqueta(1L)).thenReturn(Optional.of(existente));
        when(etiquetaRepository.findByNombreIgnoreCase("Manga"))
                .thenReturn(Optional.of(etiqueta(2L, "Manga", EtiquetaEstado.VALIDA, estilo, admin, 0)));

        assertThatThrownBy(() -> etiquetaService.modificar(1L, request("Manga", 1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe una etiqueta válida con ese nombre.");
    }

    @Test
    void modificar_mismoIdNoCuentaComoRepetido() {
        Etiqueta existente = etiqueta(1L, "Acuarela", EtiquetaEstado.VALIDA, estilo, admin, 0);
        when(etiquetaRepository.findByIdEtiqueta(1L)).thenReturn(Optional.of(existente));
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela")).thenReturn(Optional.of(existente));
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(1L)).thenReturn(Optional.of(estilo));
        when(etiquetaRepository.save(any(Etiqueta.class))).thenAnswer(inv -> inv.getArgument(0));

        EtiquetaDTO resultado = etiquetaService.modificar(1L, request("Acuarela", 1L));

        assertThat(resultado.getNombre()).isEqualTo("Acuarela");
    }

    @Test
    void modificar_haciaCategoriaInactivaDistinta_lanzaError() {
        Etiqueta existente = etiqueta(1L, "Acuarela", EtiquetaEstado.VALIDA, estilo, admin, 0);
        when(etiquetaRepository.findByIdEtiqueta(1L)).thenReturn(Optional.of(existente));
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela")).thenReturn(Optional.of(existente));
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(2L))
                .thenReturn(Optional.of(categoria(2L, "Vieja", false)));

        assertThatThrownBy(() -> etiquetaService.modificar(1L, request("Acuarela", 2L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La categoría seleccionada está inactiva y no puede ofrecerse para editar etiquetas.");
    }

    @Test
    void modificar_etiquetaInexistente_lanzaNoSuchElement() {
        when(etiquetaRepository.findByIdEtiqueta(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> etiquetaService.modificar(99L, request("X", 1L)))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Etiqueta no encontrada: 99");
    }

    // ---------------------------------------------------------------
    // aprobar (UC-57)
    // ---------------------------------------------------------------

    @Test
    void aprobar_pendiente_pasaAValidaYRegistraEvaluador() {
        Etiqueta pendiente = etiqueta(1L, "Pixel", EtiquetaEstado.PENDIENTE, estilo, artista, 1);
        when(etiquetaRepository.findByIdEtiqueta(1L)).thenReturn(Optional.of(pendiente));
        when(etiquetaRepository.existsByNombreIgnoreCaseAndEstado("Pixel", EtiquetaEstado.VALIDA))
                .thenReturn(false);
        when(etiquetaRepository.save(any(Etiqueta.class))).thenAnswer(inv -> inv.getArgument(0));

        EtiquetaDTO resultado = etiquetaService.aprobar(admin, 1L);

        ArgumentCaptor<Etiqueta> captor = ArgumentCaptor.forClass(Etiqueta.class);
        verify(etiquetaRepository).save(captor.capture());
        Etiqueta guardada = captor.getValue();
        assertThat(guardada.getEstado()).isEqualTo(EtiquetaEstado.VALIDA);
        assertThat(guardada.getEvaluadoPor()).isSameAs(admin);
        assertThat(guardada.getFechaEvaluacion()).isNotNull();
        assertThat(resultado.getEvaluadoPor()).isEqualTo("admin");
    }

    @Test
    void aprobar_rechazada_pasaAValida() {
        Etiqueta rechazada = etiqueta(1L, "Pixel", EtiquetaEstado.RECHAZADA, estilo, artista, 3);
        when(etiquetaRepository.findByIdEtiqueta(1L)).thenReturn(Optional.of(rechazada));
        when(etiquetaRepository.existsByNombreIgnoreCaseAndEstado("Pixel", EtiquetaEstado.VALIDA))
                .thenReturn(false);
        when(etiquetaRepository.save(any(Etiqueta.class))).thenAnswer(inv -> inv.getArgument(0));

        EtiquetaDTO resultado = etiquetaService.aprobar(admin, 1L);

        assertThat(resultado.getEstado()).isEqualTo(EtiquetaEstado.VALIDA);
        assertThat(resultado.getVecesSolicitada()).isEqualTo(3);
    }

    @Test
    void aprobar_yaValida_lanzaError() {
        when(etiquetaRepository.findByIdEtiqueta(1L))
                .thenReturn(Optional.of(etiqueta(1L, "Pixel", EtiquetaEstado.VALIDA, estilo, admin, 0)));

        assertThatThrownBy(() -> etiquetaService.aprobar(admin, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La etiqueta ya está aprobada.");
    }

    @Test
    void aprobar_inactiva_lanzaError() {
        when(etiquetaRepository.findByIdEtiqueta(1L))
                .thenReturn(Optional.of(etiqueta(1L, "Pixel", EtiquetaEstado.INACTIVA, estilo, admin, 0)));

        assertThatThrownBy(() -> etiquetaService.aprobar(admin, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No se puede aprobar una etiqueta dada de baja.");
    }

    @Test
    void aprobar_yaExisteValidaConMismoNombre_lanzaError() {
        when(etiquetaRepository.findByIdEtiqueta(1L))
                .thenReturn(Optional.of(etiqueta(1L, "Pixel", EtiquetaEstado.PENDIENTE, estilo, artista, 1)));
        when(etiquetaRepository.existsByNombreIgnoreCaseAndEstado("Pixel", EtiquetaEstado.VALIDA))
                .thenReturn(true);

        assertThatThrownBy(() -> etiquetaService.aprobar(admin, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe una etiqueta válida con ese nombre; modifícala o eliminala primero.");

        verify(etiquetaRepository, never()).save(any(Etiqueta.class));
    }

    // ---------------------------------------------------------------
    // rechazar (UC-57)
    // ---------------------------------------------------------------

    @Test
    void rechazar_pendiente_pasaARechazadaYQuitaAsignaturas() {
        when(etiquetaRepository.findByIdEtiqueta(1L))
                .thenReturn(Optional.of(etiqueta(1L, "Pixel", EtiquetaEstado.PENDIENTE, estilo, artista, 1)));
        when(etiquetaRepository.save(any(Etiqueta.class))).thenAnswer(inv -> inv.getArgument(0));

        EtiquetaDTO resultado = etiquetaService.rechazar(admin, 1L);

        assertThat(resultado.getEstado()).isEqualTo(EtiquetaEstado.RECHAZADA);
        assertThat(resultado.getEvaluadoPor()).isEqualTo("admin");
        verify(imagenEtiquetaRepository).deleteByEtiquetaIdEtiqueta(1L);
    }

    @Test
    void rechazar_noPendiente_lanzaError() {
        when(etiquetaRepository.findByIdEtiqueta(1L))
                .thenReturn(Optional.of(etiqueta(1L, "Pixel", EtiquetaEstado.VALIDA, estilo, admin, 0)));

        assertThatThrownBy(() -> etiquetaService.rechazar(admin, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Solo se pueden rechazar etiquetas pendientes de evaluación.");

        verify(imagenEtiquetaRepository, never()).deleteByEtiquetaIdEtiqueta(any());
    }

    // ---------------------------------------------------------------
    // darDeBaja (UC-56)
    // ---------------------------------------------------------------

    @Test
    void darDeBaja_valida_pasaAInactiva() {
        when(etiquetaRepository.findByIdEtiqueta(1L))
                .thenReturn(Optional.of(etiqueta(1L, "Acuarela", EtiquetaEstado.VALIDA, estilo, admin, 0)));
        when(etiquetaRepository.save(any(Etiqueta.class))).thenAnswer(inv -> inv.getArgument(0));

        EtiquetaDTO resultado = etiquetaService.darDeBaja(1L);

        assertThat(resultado.getEstado()).isEqualTo(EtiquetaEstado.INACTIVA);
    }

    @Test
    void darDeBaja_noValida_lanzaError() {
        when(etiquetaRepository.findByIdEtiqueta(1L))
                .thenReturn(Optional.of(etiqueta(1L, "Pixel", EtiquetaEstado.PENDIENTE, estilo, artista, 1)));

        assertThatThrownBy(() -> etiquetaService.darDeBaja(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Solo puede darse de baja una etiqueta en estado VALIDA.");
    }

    // ---------------------------------------------------------------
    // reactivar
    // ---------------------------------------------------------------

    @Test
    void reactivar_inactiva_pasaAValidaYRegistraQuienReactivo() {
        Etiqueta inactiva = etiqueta(1L, "Acuarela", EtiquetaEstado.INACTIVA, estilo, admin, 0);
        when(etiquetaRepository.findByIdEtiqueta(1L)).thenReturn(Optional.of(inactiva));
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela")).thenReturn(Optional.of(inactiva));
        when(etiquetaRepository.save(any(Etiqueta.class))).thenAnswer(inv -> inv.getArgument(0));

        EtiquetaDTO resultado = etiquetaService.reactivar(admin, 1L);

        ArgumentCaptor<Etiqueta> captor = ArgumentCaptor.forClass(Etiqueta.class);
        verify(etiquetaRepository).save(captor.capture());
        Etiqueta guardada = captor.getValue();
        assertThat(guardada.getEstado()).isEqualTo(EtiquetaEstado.VALIDA);
        assertThat(guardada.getEvaluadoPor()).isSameAs(admin);
        assertThat(guardada.getFechaEvaluacion()).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo(EtiquetaEstado.VALIDA);
    }

    @Test
    void reactivar_noInactiva_lanzaError() {
        when(etiquetaRepository.findByIdEtiqueta(1L))
                .thenReturn(Optional.of(etiqueta(1L, "Pixel", EtiquetaEstado.VALIDA, estilo, admin, 0)));

        assertThatThrownBy(() -> etiquetaService.reactivar(admin, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Solo puede reactivarse una etiqueta dada de baja.");

        verify(etiquetaRepository, never()).save(any(Etiqueta.class));
    }

    @Test
    void reactivar_existeOtraEtiquetaMismoNombre_lanzaError() {
        Etiqueta inactiva = etiqueta(1L, "Acuarela", EtiquetaEstado.INACTIVA, estilo, admin, 0);
        Etiqueta duplicada = etiqueta(2L, "Acuarela", EtiquetaEstado.PENDIENTE, estilo, artista, 1);
        when(etiquetaRepository.findByIdEtiqueta(1L)).thenReturn(Optional.of(inactiva));
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela")).thenReturn(Optional.of(duplicada));

        assertThatThrownBy(() -> etiquetaService.reactivar(admin, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe una etiqueta pendiente de evaluación con ese nombre.");

        verify(etiquetaRepository, never()).save(any(Etiqueta.class));
    }

    // ---------------------------------------------------------------
    // crearDesdeArtista (UC-54)
    // ---------------------------------------------------------------

    @Test
    void crearDesdeArtista_nueva_creaPendienteConDemandaUno() {
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela")).thenReturn(Optional.empty());
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(1L)).thenReturn(Optional.of(estilo));
        when(etiquetaRepository.save(any(Etiqueta.class))).thenAnswer(inv -> inv.getArgument(0));

        EtiquetaDTO resultado = etiquetaService.crearDesdeArtista(artista, request("  Acuarela  ", 1L));

        ArgumentCaptor<Etiqueta> captor = ArgumentCaptor.forClass(Etiqueta.class);
        verify(etiquetaRepository).save(captor.capture());
        Etiqueta guardada = captor.getValue();
        assertThat(guardada.getEstado()).isEqualTo(EtiquetaEstado.PENDIENTE);
        assertThat(guardada.getVecesSolicitada()).isEqualTo(1);
        assertThat(guardada.getCreadoPor()).isSameAs(artista);
        assertThat(resultado.getCreadoPor()).isEqualTo("dibujito");
    }

    @Test
    void crearDesdeArtista_conObservaciones_lasTrimea() {
        when(etiquetaRepository.findByNombreIgnoreCase("Pixel")).thenReturn(Optional.empty());
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(1L)).thenReturn(Optional.of(estilo));
        when(etiquetaRepository.save(any(Etiqueta.class))).thenAnswer(inv -> inv.getArgument(0));

        EtiquetaRequest conObservaciones = request("Pixel", 1L);
        conObservaciones.setObservaciones("  estilo propio  ");
        etiquetaService.crearDesdeArtista(artista, conObservaciones);

        ArgumentCaptor<Etiqueta> captor = ArgumentCaptor.forClass(Etiqueta.class);
        verify(etiquetaRepository).save(captor.capture());
        Etiqueta guardada = captor.getValue();
        assertThat(guardada.getNombre()).isEqualTo("Pixel");
        assertThat(guardada.getObservaciones()).isEqualTo("estilo propio");
    }

    @Test
    void crearDesdeArtista_nombreValida_lanzaError() {
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela"))
                .thenReturn(Optional.of(etiqueta(1L, "Acuarela", EtiquetaEstado.VALIDA, estilo, admin, 0)));

        assertThatThrownBy(() -> etiquetaService.crearDesdeArtista(artista, request("Acuarela", 1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ya existe una etiqueta válida con ese nombre; puedes usarla directamente en el selector.");
    }

    @Test
    void crearDesdeArtista_propiaPendiente_reutilizaSinGuardar() {
        when(etiquetaRepository.findByNombreIgnoreCase("Pixel"))
                .thenReturn(Optional.of(etiqueta(1L, "Pixel", EtiquetaEstado.PENDIENTE, estilo, artista, 1)));

        EtiquetaDTO resultado = etiquetaService.crearDesdeArtista(artista, request("Pixel", 1L));

        assertThat(resultado.getIdEtiqueta()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EtiquetaEstado.PENDIENTE);
        verify(etiquetaRepository, never()).save(any(Etiqueta.class));
    }

    @Test
    void crearDesdeArtista_ajenaPendiente_lanzaError() {
        when(etiquetaRepository.findByNombreIgnoreCase("Pixel"))
                .thenReturn(Optional.of(etiqueta(1L, "Pixel", EtiquetaEstado.PENDIENTE, estilo, usuario(30L, "otro"), 1)));

        assertThatThrownBy(() -> etiquetaService.crearDesdeArtista(artista, request("Pixel", 1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Otro artista propuso esa etiqueta y está pendiente de evaluación.");
    }

    @Test
    void crearDesdeArtista_rechazada_lanzaError() {
        when(etiquetaRepository.findByNombreIgnoreCase("Manga"))
                .thenReturn(Optional.of(etiqueta(1L, "Manga", EtiquetaEstado.RECHAZADA, estilo, artista, 2)));

        assertThatThrownBy(() -> etiquetaService.crearDesdeArtista(artista, request("Manga", 1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rechazada")
                .hasMessageContaining("Mis etiquetas");
    }

    @Test
    void crearDesdeArtista_inactiva_lanzaError() {
        when(etiquetaRepository.findByNombreIgnoreCase("Vieja"))
                .thenReturn(Optional.of(etiqueta(1L, "Vieja", EtiquetaEstado.INACTIVA, estilo, admin, 0)));

        assertThatThrownBy(() -> etiquetaService.crearDesdeArtista(artista, request("Vieja", 1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La etiqueta fue dada de baja por los administradores y no puede asignarse.");
    }

    @Test
    void crearDesdeArtista_categoriaInactiva_lanzaError() {
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela")).thenReturn(Optional.empty());
        when(categoriaEtiquetaRepository.findByIdCategoriaEtiqueta(2L))
                .thenReturn(Optional.of(categoria(2L, "Vieja", false)));

        assertThatThrownBy(() -> etiquetaService.crearDesdeArtista(artista, request("Acuarela", 2L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La categoría seleccionada está inactiva.");
    }

    // ---------------------------------------------------------------
    // reSolicitar (UC-59)
    // ---------------------------------------------------------------

    @Test
    void reSolicitar_rechazadaPropia_sumaDemanda() {
        when(etiquetaRepository.findByIdEtiqueta(1L))
                .thenReturn(Optional.of(etiqueta(1L, "Manga", EtiquetaEstado.RECHAZADA, estilo, artista, 2)));
        when(etiquetaRepository.save(any(Etiqueta.class))).thenAnswer(inv -> inv.getArgument(0));

        EtiquetaDTO resultado = etiquetaService.reSolicitar(artista, 1L);

        assertThat(resultado.getVecesSolicitada()).isEqualTo(3);
    }

    @Test
    void reSolicitar_ajena_lanzaError() {
        when(etiquetaRepository.findByIdEtiqueta(1L))
                .thenReturn(Optional.of(etiqueta(1L, "Manga", EtiquetaEstado.RECHAZADA, estilo, usuario(30L, "otro"), 2)));

        assertThatThrownBy(() -> etiquetaService.reSolicitar(artista, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Solo el artista que propuso la etiqueta puede re-solicitarla.");
    }

    @Test
    void reSolicitar_creadaPorAdmin_lanzaError() {
        when(etiquetaRepository.findByIdEtiqueta(1L))
                .thenReturn(Optional.of(etiqueta(1L, "Manga", EtiquetaEstado.RECHAZADA, estilo, admin, 2)));

        assertThatThrownBy(() -> etiquetaService.reSolicitar(artista, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reSolicitar_noRechazada_lanzaError() {
        when(etiquetaRepository.findByIdEtiqueta(1L))
                .thenReturn(Optional.of(etiqueta(1L, "Manga", EtiquetaEstado.PENDIENTE, estilo, artista, 1)));

        assertThatThrownBy(() -> etiquetaService.reSolicitar(artista, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solo se pueden re-solicitar etiquetas rechazadas");
    }

    @Test
    void reSolicitar_inexistente_lanzaNoSuchElement() {
        when(etiquetaRepository.findByIdEtiqueta(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> etiquetaService.reSolicitar(artista, 99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Etiqueta no encontrada: 99");
    }

    // ---------------------------------------------------------------
    // selector / misEtiquetas / buscar / categorias
    // ---------------------------------------------------------------

    @Test
    void selector_concatenaValidasYPropiasPendientes() {
        Etiqueta valida = etiqueta(1L, "Acuarela", EtiquetaEstado.VALIDA, estilo, admin, 0);
        Etiqueta propia = etiqueta(2L, "Pixel", EtiquetaEstado.PENDIENTE, estilo, artista, 1);
        when(etiquetaRepository.findByEstadoOrderByNombreAsc(EtiquetaEstado.VALIDA)).thenReturn(List.of(valida));
        when(etiquetaRepository.findByEstadoAndCreadoPorIdUsuario(EtiquetaEstado.PENDIENTE, 20L))
                .thenReturn(List.of(propia));

        List<EtiquetaDTO> resultado = etiquetaService.selector(artista);

        assertThat(resultado).extracting(EtiquetaDTO::getNombre)
                .containsExactly("Acuarela", "Pixel");
    }

    @Test
    void misEtiquetas_devuelveSoloLasPropias() {
        when(etiquetaRepository.findByCreadoPorIdUsuarioOrderByFechaCreacionDesc(20L))
                .thenReturn(List.of(etiqueta(1L, "Pixel", EtiquetaEstado.PENDIENTE, estilo, artista, 1)));

        List<EtiquetaDTO> resultado = etiquetaService.misEtiquetas(artista);

        assertThat(resultado).extracting(EtiquetaDTO::getCreadoPor)
                .containsExactly("dibujito");
    }

    @Test
    void buscar_encontrada_devuelveDTO() {
        when(etiquetaRepository.findByNombreIgnoreCase("Acuarela"))
                .thenReturn(Optional.of(etiqueta(1L, "Acuarela", EtiquetaEstado.VALIDA, estilo, admin, 0)));

        Optional<EtiquetaDTO> resultado = etiquetaService.buscar(" Acuarela ");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNombre()).isEqualTo("Acuarela");
    }

    @Test
    void buscar_noEncontrada_devuelveVacio() {
        when(etiquetaRepository.findByNombreIgnoreCase("fantasma")).thenReturn(Optional.empty());

        assertThat(etiquetaService.buscar("fantasma")).isEmpty();
    }

    @Test
    void categoriasActivas_mapeaDtos() {
        when(categoriaEtiquetaRepository.findByActivoTrueOrderByNombreAsc())
                .thenReturn(List.of(estilo));

        List<CategoriaEtiquetaDTO> resultado = etiquetaService.categoriasActivas();

        assertThat(resultado).extracting(CategoriaEtiquetaDTO::getNombre)
                .containsExactly("Estilo");
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private CategoriaEtiqueta categoria(Long id, String nombre, boolean activo) {
        return CategoriaEtiqueta.builder()
                .idCategoriaEtiqueta(id)
                .nombre(nombre)
                .descripcion("descripcion")
                .activo(activo)
                .build();
    }

    private Etiqueta etiqueta(
            Long id, String nombre, EtiquetaEstado estado,
            CategoriaEtiqueta categoria, Usuario creador, int demanda) {
        return Etiqueta.builder()
                .idEtiqueta(id)
                .nombre(nombre)
                .descripcion("descripcion")
                .observaciones(null)
                .categoria(categoria)
                .creadoPor(creador)
                .estado(estado)
                .vecesSolicitada(demanda)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    private PageImpl<Etiqueta> pagina(List<Etiqueta> etiquetas) {
        return new PageImpl<>(etiquetas);
    }

    private EtiquetaRequest request(String nombre, Long idCategoria) {
        EtiquetaRequest request = new EtiquetaRequest();
        request.setNombre(nombre);
        request.setDescripcion("descripcion");
        request.setObservaciones(null);
        request.setIdCategoria(idCategoria);
        return request;
    }

    private Usuario usuario(Long id, String nombreUsuario) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombreUsuario(nombreUsuario);
        return usuario;
    }
}
