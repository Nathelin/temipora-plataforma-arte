package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.EEstadoTema;
import com.temipora.backend_springboot.modelo.TemaTermino;
import com.temipora.backend_springboot.modelo.TerminoDerechoRequest;
import com.temipora.backend_springboot.modelo.TerminoTemaRequest;
import com.temipora.backend_springboot.modelo.Terminos;
import com.temipora.backend_springboot.modelo.TerminosDTO;
import com.temipora.backend_springboot.modelo.TerminosRequest;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.DerechoTerminoRepository;
import com.temipora.backend_springboot.repositorios.TemaTerminoRepository;
import com.temipora.backend_springboot.repositorios.TerminosRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class TerminosServiceTest {

    @Mock
    private TerminosRepository terminosRepository;

    @Mock
    private TemaTerminoRepository temaTerminoRepository;

    @Mock
    private DerechoTerminoRepository derechoTerminoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private TerminosService terminosService;

    // ---------------------------------------------------------------
    // obtenerGlobal
    // ---------------------------------------------------------------

    @Test
    void obtenerGlobal_sinTerminos_devuelveVacio() {
        when(terminosRepository.findByArtistaIdUsuarioAndIdProductoIsNullAndIdServicioIsNull(5L))
            .thenReturn(Optional.empty());

        TerminosDTO dto = terminosService.obtenerGlobal(5L);

        assertThat(dto.getVersion()).isZero();
        assertThat(dto.getTemas()).isEmpty();
        assertThat(dto.getDerechos()).isEmpty();
    }

    @Test
    void obtenerGlobal_existente_devuelveElDelArtista() {
        Terminos terminos = terminos(1L, 2, "Hola");
        when(terminosRepository.findByArtistaIdUsuarioAndIdProductoIsNullAndIdServicioIsNull(5L))
            .thenReturn(Optional.of(terminos));

        TerminosDTO dto = terminosService.obtenerGlobal(5L);

        assertThat(dto.getVersion()).isEqualTo(2);
        assertThat(dto.getInformacionGeneral()).isEqualTo("Hola");
    }

    // ---------------------------------------------------------------
    // guardarGlobal
    // ---------------------------------------------------------------

    @Test
    void guardarGlobal_creaVersion1ConTemasYDerechos() {
        when(temaTerminoRepository.findByIdTemaTermino(1L)).thenReturn(Optional.of(tema(1L, "NSFW", 1)));
        when(derechoTerminoRepository.findByIdDerechoTermino(1L))
            .thenReturn(Optional.of(derecho(1L, "CONTENIDO", "Uso comercial en producción de contenido", 2)));
        when(terminosRepository.findByArtistaIdUsuarioAndIdProductoIsNullAndIdServicioIsNull(5L))
            .thenReturn(Optional.empty());
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));
        when(terminosRepository.save(any(Terminos.class))).thenAnswer(inv -> inv.getArgument(0));

        TerminosRequest request = requestVacio();
        request.setInformacionGeneral("  Hola  ");
        request.setTemas(List.of(terminoReq(1L, EEstadoTema.SI)));
        request.setDerechos(List.of(derechoReq(1L, true, new BigDecimal("50"))));

        TerminosDTO dto = terminosService.guardarGlobal(5L, request);

        assertThat(dto.getVersion()).isEqualTo(1);
        assertThat(dto.getInformacionGeneral()).isEqualTo("Hola");
        assertThat(dto.getTemas()).hasSize(1);
        assertThat(dto.getTemas().get(0).getEstado()).isEqualTo("SI");
        assertThat(dto.getTemas().get(0).isPersonalizado()).isFalse();
        assertThat(dto.getDerechos()).hasSize(1);
        assertThat(dto.getDerechos().get(0).isDisponible()).isTrue();
        assertThat(dto.getDerechos().get(0).getPorcentajeExtra()).isEqualByComparingTo("50");
    }

    @Test
    void guardarGlobal_personalSiempreDisponibleSinPorcentaje() {
        when(derechoTerminoRepository.findByIdDerechoTermino(2L))
            .thenReturn(Optional.of(derecho(2L, "PERSONAL", "Uso personal", 1)));
        when(terminosRepository.findByArtistaIdUsuarioAndIdProductoIsNullAndIdServicioIsNull(5L))
            .thenReturn(Optional.empty());
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));
        when(terminosRepository.save(any(Terminos.class))).thenAnswer(inv -> inv.getArgument(0));

        TerminosRequest request = requestVacio();
        request.setInformacionGeneral("Hola");
        request.setDerechos(List.of(derechoReq(2L, false, new BigDecimal("10"))));

        TerminosDTO dto = terminosService.guardarGlobal(5L, request);

        assertThat(dto.getDerechos().get(0).isDisponible()).isTrue();
        assertThat(dto.getDerechos().get(0).getPorcentajeExtra()).isNull();
    }

    @Test
    void guardarGlobal_temaPersonalizado_seGuarda() {
        when(terminosRepository.findByArtistaIdUsuarioAndIdProductoIsNullAndIdServicioIsNull(5L))
            .thenReturn(Optional.empty());
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));
        when(terminosRepository.save(any(Terminos.class))).thenAnswer(inv -> inv.getArgument(0));

        TerminosRequest request = requestVacio();
        request.setInformacionGeneral("Hola");
        request.setTemas(List.of(temaPersonalizadoReq("Mi tema", EEstadoTema.NO)));

        TerminosDTO dto = terminosService.guardarGlobal(5L, request);

        assertThat(dto.getTemas()).hasSize(1);
        assertThat(dto.getTemas().get(0).isPersonalizado()).isTrue();
        assertThat(dto.getTemas().get(0).getNombre()).isEqualTo("Mi tema");
    }

    @Test
    void guardarGlobal_temaPersonalizadoSinNombre_lanzaError() {
        when(terminosRepository.findByArtistaIdUsuarioAndIdProductoIsNullAndIdServicioIsNull(5L))
            .thenReturn(Optional.empty());
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));

        TerminosRequest request = requestVacio();
        request.setInformacionGeneral("Hola");
        request.setTemas(List.of(temaPersonalizadoReq("   ", EEstadoTema.NO)));

        assertThatThrownBy(() -> terminosService.guardarGlobal(5L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El tema personalizado necesita un nombre.");
    }

    @Test
    void guardarGlobal_existente_incrementaVersion() {
        Terminos existente = terminos(1L, 3, "Viejo");
        when(terminosRepository.findByArtistaIdUsuarioAndIdProductoIsNullAndIdServicioIsNull(5L))
            .thenReturn(Optional.of(existente));
        when(terminosRepository.save(any(Terminos.class))).thenAnswer(inv -> inv.getArgument(0));

        TerminosRequest request = requestVacio();
        request.setInformacionGeneral("Nuevo");

        TerminosDTO dto = terminosService.guardarGlobal(5L, request);

        assertThat(dto.getVersion()).isEqualTo(4);
        verify(usuarioRepository, never()).findByIdUsuario(any());
        verify(terminosRepository).flush();
    }

    @Test
    void guardarGlobal_sinContenido_lanzaError() {
        TerminosRequest request = requestVacio();

        assertThatThrownBy(() -> terminosService.guardarGlobal(5L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Completá al menos una sección de tus términos.");

        verify(terminosRepository, never()).save(any(Terminos.class));
    }

    @Test
    void guardarGlobal_temaInexistente_lanzaError() {
        when(temaTerminoRepository.findByIdTemaTermino(99L)).thenReturn(Optional.empty());
        when(terminosRepository.findByArtistaIdUsuarioAndIdProductoIsNullAndIdServicioIsNull(5L))
            .thenReturn(Optional.empty());
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));

        TerminosRequest request = requestVacio();
        request.setTemas(List.of(terminoReq(99L, EEstadoTema.NO)));

        assertThatThrownBy(() -> terminosService.guardarGlobal(5L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Tema no encontrado: 99");
    }

    @Test
    void guardarGlobal_artistaInexistente_lanzaNoSuchElement() {
        when(terminosRepository.findByArtistaIdUsuarioAndIdProductoIsNullAndIdServicioIsNull(5L))
            .thenReturn(Optional.empty());
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.empty());

        TerminosRequest request = requestVacio();
        request.setInformacionGeneral("Hola");

        assertThatThrownBy(() -> terminosService.guardarGlobal(5L, request))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Usuario no encontrado: 5");
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Terminos terminos(Long id, int version, String info) {
        Terminos terminos = new Terminos();
        terminos.setIdTerminos(id);
        terminos.setVersion(version);
        terminos.setInformacionGeneral(info);
        terminos.setTerminoTemas(new ArrayList<>());
        terminos.setTerminoDerechos(new ArrayList<>());
        return terminos;
    }

    private TemaTermino tema(Long id, String nombre, int orden) {
        TemaTermino tema = new TemaTermino();
        tema.setIdTemaTermino(id);
        tema.setNombre(nombre);
        tema.setOrden(orden);
        return tema;
    }

    private DerechoTermino derecho(Long id, String codigo, String nombre, int orden) {
        DerechoTermino derecho = new DerechoTermino();
        derecho.setIdDerechoTermino(id);
        derecho.setCodigo(codigo);
        derecho.setNombre(nombre);
        derecho.setOrden(orden);
        return derecho;
    }

    private TerminoTemaRequest terminoReq(Long id, EEstadoTema estado) {
        TerminoTemaRequest request = new TerminoTemaRequest();
        request.setIdTemaTermino(id);
        request.setEstado(estado);
        return request;
    }

    private TerminoTemaRequest temaPersonalizadoReq(String nombre, EEstadoTema estado) {
        TerminoTemaRequest request = new TerminoTemaRequest();
        request.setNombre(nombre);
        request.setEstado(estado);
        return request;
    }

    private TerminoDerechoRequest derechoReq(Long id, boolean disponible, BigDecimal porcentaje) {
        TerminoDerechoRequest request = new TerminoDerechoRequest();
        request.setIdDerechoTermino(id);
        request.setDisponible(disponible);
        request.setPorcentajeExtra(porcentaje);
        return request;
    }

    private TerminosRequest requestVacio() {
        TerminosRequest request = new TerminosRequest();
        request.setTemas(new ArrayList<>());
        request.setDerechos(new ArrayList<>());
        return request;
    }

    private Usuario usuario(Long id) {
        return Usuario.builder().idUsuario(id).build();
    }
}
