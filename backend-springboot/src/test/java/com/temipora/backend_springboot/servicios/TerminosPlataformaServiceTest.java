package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.AceptacionTerminosPlataforma;
import com.temipora.backend_springboot.modelo.ComisionConfigDTO;
import com.temipora.backend_springboot.modelo.TerminosPlataforma;
import com.temipora.backend_springboot.modelo.TerminosPlataformaDTO;
import com.temipora.backend_springboot.modelo.TerminosPlataformaEstadoDTO;
import com.temipora.backend_springboot.modelo.TerminosPlataformaRequest;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.AceptacionTerminosPlataformaRepository;
import com.temipora.backend_springboot.repositorios.TerminosPlataformaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class TerminosPlataformaServiceTest {

    @Mock
    private TerminosPlataformaRepository terminosRepository;

    @Mock
    private AceptacionTerminosPlataformaRepository aceptacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ComisionService comisionService;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private TerminosPlataformaService service;

    // ---------------------------------------------------------------
    // lectura
    // ---------------------------------------------------------------

    @Test
    void obtenerEntidadVigente_sinTerminos_lanzaNoSuchElement() {
        when(terminosRepository.findFirstByVigenteTrueOrderByVersionDesc())
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerEntidadVigente())
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void obtenerVigente_devuelveElDTO() {
        when(terminosRepository.findFirstByVigenteTrueOrderByVersionDesc())
            .thenReturn(Optional.of(terminos(2, true)));

        TerminosPlataformaDTO dto = service.obtenerVigente();

        assertThat(dto.getVersion()).isEqualTo(2);
        assertThat(dto.isVigente()).isTrue();
    }

    @Test
    void listarVersiones_mapeaTodas() {
        when(terminosRepository.findAllByOrderByVersionDesc())
            .thenReturn(List.of(terminos(2, true), terminos(1, false)));

        List<TerminosPlataformaDTO> resultado = service.listarVersiones();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(TerminosPlataformaDTO::getVersion)
            .containsExactly(2, 1);
    }

    // ---------------------------------------------------------------
    // estado del usuario
    // ---------------------------------------------------------------

    @Test
    void obtenerEstadoUsuario_sinTerminos_noHayNadaPendiente() {
        when(terminosRepository.findFirstByVigenteTrueOrderByVersionDesc())
            .thenReturn(Optional.empty());

        TerminosPlataformaEstadoDTO estado = service.obtenerEstadoUsuario(1L);

        assertThat(estado.isAceptada()).isTrue();
    }

    @Test
    void obtenerEstadoUsuario_conAceptacion_marcaAceptada() {
        when(terminosRepository.findFirstByVigenteTrueOrderByVersionDesc())
            .thenReturn(Optional.of(terminos(3, true)));
        AceptacionTerminosPlataforma aceptacion = new AceptacionTerminosPlataforma();
        aceptacion.setFechaAceptacion(LocalDateTime.of(2026, 1, 1, 9, 0));
        when(aceptacionRepository.findByUsuarioIdUsuarioAndVersion(1L, 3))
            .thenReturn(Optional.of(aceptacion));

        TerminosPlataformaEstadoDTO estado = service.obtenerEstadoUsuario(1L);

        assertThat(estado.isAceptada()).isTrue();
        assertThat(estado.getFechaAceptacion()).isNotNull();
    }

    @Test
    void obtenerEstadoUsuario_sinAceptacion_quedaPendiente() {
        when(terminosRepository.findFirstByVigenteTrueOrderByVersionDesc())
            .thenReturn(Optional.of(terminos(3, true)));
        when(aceptacionRepository.findByUsuarioIdUsuarioAndVersion(1L, 3))
            .thenReturn(Optional.empty());

        TerminosPlataformaEstadoDTO estado = service.obtenerEstadoUsuario(1L);

        assertThat(estado.isAceptada()).isFalse();
        assertThat(estado.getFechaAceptacion()).isNull();
    }

    // ---------------------------------------------------------------
    // aceptar
    // ---------------------------------------------------------------

    @Test
    void aceptar_versionNula_lanzaIllegalArgument() {
        assertThatThrownBy(() -> service.aceptar(1L, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aceptar_versionDistintaAALaVigente_lanzaConflicto() {
        when(terminosRepository.findFirstByVigenteTrueOrderByVersionDesc())
            .thenReturn(Optional.of(terminos(3, true)));

        assertThatThrownBy(() -> service.aceptar(1L, 2))
            .isInstanceOf(ConflictoException.class);
    }

    @Test
    void aceptar_usuarioInexistente_lanzaNoSuchElement() {
        when(terminosRepository.findFirstByVigenteTrueOrderByVersionDesc())
            .thenReturn(Optional.of(terminos(3, true)));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.aceptar(1L, 3))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void aceptar_yaAceptada_noGuardaOtraVez() {
        when(terminosRepository.findFirstByVigenteTrueOrderByVersionDesc())
            .thenReturn(Optional.of(terminos(3, true)));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L)));
        AceptacionTerminosPlataforma previa = new AceptacionTerminosPlataforma();
        when(aceptacionRepository.findByUsuarioIdUsuarioAndVersion(1L, 3))
            .thenReturn(Optional.of(previa));

        service.aceptar(1L, 3);

        verify(aceptacionRepository, never()).save(any());
    }

    @Test
    void aceptar_nuevaVersion_registraLaAceptacion() {
        when(terminosRepository.findFirstByVigenteTrueOrderByVersionDesc())
            .thenReturn(Optional.of(terminos(3, true)));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L)));
        when(aceptacionRepository.findByUsuarioIdUsuarioAndVersion(1L, 3))
            .thenReturn(Optional.empty());

        TerminosPlataformaEstadoDTO estado = service.aceptar(1L, 3);

        assertThat(estado).isNotNull();
        verify(aceptacionRepository).save(any(AceptacionTerminosPlataforma.class));
    }

    // ---------------------------------------------------------------
    // publicar
    // ---------------------------------------------------------------

    @Test
    void publicar_tituloVacio_lanzaIllegalArgument() {
        TerminosPlataformaRequest request = new TerminosPlataformaRequest();
        request.setTitulo("   ");
        request.setContenido("contenido");

        assertThatThrownBy(() -> service.publicar(request))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publicar_contenidoVacio_lanzaIllegalArgument() {
        TerminosPlataformaRequest request = new TerminosPlataformaRequest();
        request.setTitulo("Título");
        request.setContenido("");

        assertThatThrownBy(() -> service.publicar(request))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publicar_tituloDemasiadoLargo_lanzaIllegalArgument() {
        TerminosPlataformaRequest request = new TerminosPlataformaRequest();
        request.setTitulo("t".repeat(121));
        request.setContenido("contenido");

        assertThatThrownBy(() -> service.publicar(request))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publicar_primeraVersion_desactivaAnterioresYCongelaLaComision() {
        TerminosPlataforma anterior = terminos(3, true);
        when(comisionService.obtenerEstado()).thenReturn(ComisionConfigDTO.builder()
            .porcentajeServicio(new BigDecimal("12.00"))
            .porcentajeProducto(new BigDecimal("6.00"))
            .build());
        when(terminosRepository.findFirstByOrderByVersionDesc())
            .thenReturn(Optional.of(anterior));
        when(terminosRepository.findByVigenteTrue()).thenReturn(List.of(anterior));
        when(terminosRepository.save(any(TerminosPlataforma.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        TerminosPlataformaRequest request = new TerminosPlataformaRequest();
        request.setTitulo("  Términos v4  ");
        request.setContenido("Contenido nuevo");

        TerminosPlataformaDTO dto = service.publicar(request);

        assertThat(dto.getVersion()).isEqualTo(4);
        assertThat(dto.getTitulo()).isEqualTo("Términos v4");
        assertThat(dto.getComisionServicio()).isEqualByComparingTo("12.00");
        assertThat(dto.isVigente()).isTrue();
        assertThat(anterior.isVigente()).isFalse();
    }

    @Test
    void publicar_sinVersionesPrevias_arrancaEnUno() {
        when(comisionService.obtenerEstado()).thenReturn(ComisionConfigDTO.builder()
            .porcentajeServicio(new BigDecimal("10.00"))
            .porcentajeProducto(new BigDecimal("5.00"))
            .build());
        when(terminosRepository.findFirstByOrderByVersionDesc())
            .thenReturn(Optional.empty());
        when(terminosRepository.findByVigenteTrue()).thenReturn(List.of());
        when(terminosRepository.save(any(TerminosPlataforma.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        TerminosPlataformaRequest request = new TerminosPlataformaRequest();
        request.setTitulo("Primeros términos");
        request.setContenido("Contenido");

        assertThat(service.publicar(request).getVersion()).isEqualTo(1);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Usuario usuario(Long id) {
        return Usuario.builder().idUsuario(id).nombreUsuario("u" + id).build();
    }

    private TerminosPlataforma terminos(int version, boolean vigente) {
        TerminosPlataforma terminos = new TerminosPlataforma();
        terminos.setVersion(version);
        terminos.setTitulo("Términos " + version);
        terminos.setContenido("Contenido " + version);
        terminos.setComisionServicio(new BigDecimal("10.00"));
        terminos.setComisionProducto(new BigDecimal("5.00"));
        terminos.setFechaPublicacion(LocalDateTime.of(2026, 1, 1, 0, 0));
        terminos.setVigente(vigente);
        return terminos;
    }
}
