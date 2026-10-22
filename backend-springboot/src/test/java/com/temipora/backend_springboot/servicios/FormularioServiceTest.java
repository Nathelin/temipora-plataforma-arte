package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.Formulario;
import com.temipora.backend_springboot.modelo.FormularioDTO;
import com.temipora.backend_springboot.modelo.FormularioRequest;
import com.temipora.backend_springboot.modelo.Pregunta;
import com.temipora.backend_springboot.modelo.PreguntaRequest;
import com.temipora.backend_springboot.modelo.TipoCampo;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.FormularioRepository;
import com.temipora.backend_springboot.repositorios.TipoCampoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class FormularioServiceTest {

    @Mock
    private FormularioRepository formularioRepository;

    @Mock
    private TipoCampoRepository tipoCampoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private FormularioService formularioService;

    // ---------------------------------------------------------------
    // obtenerGlobal
    // ---------------------------------------------------------------

    @Test
    void obtenerGlobal_existente_devuelveElDelArtista() {
        Formulario form = formulario(false, List.of(pregunta(tipo(1L, "TEXTO"), "¿Qué querés?", 0)));
        when(formularioRepository.findByArtistaIdUsuarioAndIdServicioIsNullAndEsSistemaFalse(5L))
            .thenReturn(Optional.of(form));

        FormularioDTO dto = formularioService.obtenerGlobal(5L);

        assertThat(dto.isEsSistema()).isFalse();
        assertThat(dto.getPreguntas()).hasSize(1);
        assertThat(dto.getPreguntas().get(0).getCodigoTipo()).isEqualTo("TEXTO");
    }

    @Test
    void obtenerGlobal_sinGlobal_devuelveElDelSistema() {
        Formulario sistema = formulario(true, List.of(pregunta(tipo(4L, "ENCABEZADO"), "Datos de tu pedido", 0)));
        when(formularioRepository.findByArtistaIdUsuarioAndIdServicioIsNullAndEsSistemaFalse(5L))
            .thenReturn(Optional.empty());
        when(formularioRepository.findFirstByEsSistemaTrue()).thenReturn(Optional.of(sistema));

        FormularioDTO dto = formularioService.obtenerGlobal(5L);

        assertThat(dto.isEsSistema()).isTrue();
        assertThat(dto.getPreguntas()).hasSize(1);
    }

    // ---------------------------------------------------------------
    // formulario por servicio
    // ---------------------------------------------------------------

    @Test
    void obtenerServicio_conFormularioPropio_devuelvePropio() {
        Formulario propio = formulario(false, List.of(pregunta(tipo(1L, "TEXTO"), "Solo para este servicio", 0)));
        propio.setIdServicio(7L);
        when(formularioRepository.findByArtistaIdUsuarioAndIdServicio(5L, 7L))
            .thenReturn(Optional.of(propio));

        FormularioDTO dto = formularioService.obtenerServicio(7L, 5L);

        assertThat(dto.isPropio()).isTrue();
        assertThat(dto.getIdServicio()).isEqualTo(7L);
        assertThat(dto.getPreguntas()).hasSize(1);
    }

    @Test
    void obtenerServicio_sinFormularioPropio_devuelveElGlobal() {
        Formulario global = formulario(false, List.of(pregunta(tipo(1L, "TEXTO"), "Global", 0)));
        when(formularioRepository.findByArtistaIdUsuarioAndIdServicio(5L, 7L))
            .thenReturn(Optional.empty());
        when(formularioRepository.findByArtistaIdUsuarioAndIdServicioIsNullAndEsSistemaFalse(5L))
            .thenReturn(Optional.of(global));

        FormularioDTO dto = formularioService.obtenerServicio(7L, 5L);

        assertThat(dto.isPropio()).isFalse();
        assertThat(dto.getPreguntas().get(0).getEnunciado()).isEqualTo("Global");
    }

    @Test
    void guardarServicio_creaFormularioPropio() {
        TipoCampo texto = tipo(1L, "TEXTO");
        when(tipoCampoRepository.findByIdTipoCampo(1L)).thenReturn(Optional.of(texto));
        when(formularioRepository.findByArtistaIdUsuarioAndIdServicio(5L, 7L))
            .thenReturn(Optional.empty());
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));
        when(formularioRepository.save(any(Formulario.class))).thenAnswer(inv -> inv.getArgument(0));

        FormularioDTO dto = formularioService.guardarServicio(7L, 5L,
            request(List.of(preguntaRequest(1L, "Pregunta propia", true, null))));

        assertThat(dto.isPropio()).isTrue();
        assertThat(dto.getIdServicio()).isEqualTo(7L);
        assertThat(dto.getPreguntas()).hasSize(1);
    }

    @Test
    void eliminarServicio_borraElFormularioPropio() {
        Formulario propio = formulario(false, List.of());
        propio.setIdServicio(7L);
        when(formularioRepository.findByArtistaIdUsuarioAndIdServicio(5L, 7L))
            .thenReturn(Optional.of(propio));

        formularioService.eliminarServicio(7L, 5L);

        verify(formularioRepository).delete(propio);
    }

    // ---------------------------------------------------------------
    // guardarGlobal
    // ---------------------------------------------------------------

    @Test
    void guardarGlobal_creaFormularioConPreguntasYOrden() {
        TipoCampo texto = tipo(1L, "TEXTO");
        when(tipoCampoRepository.findByIdTipoCampo(1L)).thenReturn(Optional.of(texto));
        when(formularioRepository.findByArtistaIdUsuarioAndIdServicioIsNullAndEsSistemaFalse(5L))
            .thenReturn(Optional.empty());
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));
        when(formularioRepository.save(any(Formulario.class))).thenAnswer(inv -> inv.getArgument(0));

        FormularioRequest request = request(List.of(
            preguntaRequest(1L, "  Primera  ", false, null),
            preguntaRequest(1L, "Segunda", true, null)));

        FormularioDTO dto = formularioService.guardarGlobal(5L, request);

        assertThat(dto.isEsSistema()).isFalse();
        assertThat(dto.getPreguntas()).hasSize(2);
        assertThat(dto.getPreguntas().get(0).getEnunciado()).isEqualTo("Primera");
        assertThat(dto.getPreguntas().get(0).isObligatoria()).isFalse();
        assertThat(dto.getPreguntas().get(1).isObligatoria()).isTrue();
    }

    @Test
    void guardarGlobal_casillas_ordenaYOcultaOpcionesVacias() {
        TipoCampo casillas = tipo(2L, "CASILLAS");
        when(tipoCampoRepository.findByIdTipoCampo(2L)).thenReturn(Optional.of(casillas));
        when(formularioRepository.findByArtistaIdUsuarioAndIdServicioIsNullAndEsSistemaFalse(5L))
            .thenReturn(Optional.empty());
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.of(usuario(5L)));
        when(formularioRepository.save(any(Formulario.class))).thenAnswer(inv -> inv.getArgument(0));

        FormularioRequest request = request(List.of(
            preguntaRequest(2L, "Elegí", false, List.of("  Z  ", "A", "   "))));

        FormularioDTO dto = formularioService.guardarGlobal(5L, request);

        assertThat(dto.getPreguntas().get(0).getOpciones()).containsExactly("Z", "A");
    }

    @Test
    void guardarGlobal_actualizaFormularioExistente() {
        TipoCampo texto = tipo(1L, "TEXTO");
        Formulario existente = formulario(false, List.of(pregunta(texto, "Vieja", 0)));
        when(tipoCampoRepository.findByIdTipoCampo(1L)).thenReturn(Optional.of(texto));
        when(formularioRepository.findByArtistaIdUsuarioAndIdServicioIsNullAndEsSistemaFalse(5L))
            .thenReturn(Optional.of(existente));
        when(formularioRepository.save(any(Formulario.class))).thenAnswer(inv -> inv.getArgument(0));

        FormularioDTO dto = formularioService.guardarGlobal(5L,
            request(List.of(preguntaRequest(1L, "Nueva", true, null))));

        assertThat(dto.getPreguntas()).hasSize(1);
        assertThat(dto.getPreguntas().get(0).getEnunciado()).isEqualTo("Nueva");
        verify(usuarioRepository, never()).findByIdUsuario(any());
    }

    @Test
    void guardarGlobal_tipoInexistente_lanzaError() {
        when(tipoCampoRepository.findByIdTipoCampo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> formularioService.guardarGlobal(5L,
            request(List.of(preguntaRequest(99L, "X", false, null)))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Tipo de campo no encontrado: 99");

        verify(formularioRepository, never()).save(any(Formulario.class));
    }

    @Test
    void guardarGlobal_soloEncabezados_lanzaError() {
        TipoCampo encabezado = tipo(4L, "ENCABEZADO");
        when(tipoCampoRepository.findByIdTipoCampo(4L)).thenReturn(Optional.of(encabezado));

        assertThatThrownBy(() -> formularioService.guardarGlobal(5L,
            request(List.of(preguntaRequest(4L, "Solo título", false, null)))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("al menos una pregunta que el cliente pueda completar");

        verify(formularioRepository, never()).save(any(Formulario.class));
    }

    @Test
    void guardarGlobal_casillasSinOpciones_lanzaError() {
        TipoCampo casillas = tipo(2L, "CASILLAS");
        when(tipoCampoRepository.findByIdTipoCampo(2L)).thenReturn(Optional.of(casillas));

        assertThatThrownBy(() -> formularioService.guardarGlobal(5L,
            request(List.of(preguntaRequest(2L, "Elegí", false, List.of())))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("necesita al menos una opción");

        verify(formularioRepository, never()).save(any(Formulario.class));
    }

    @Test
    void guardarGlobal_artistaInexistente_lanzaNoSuchElement() {
        TipoCampo texto = tipo(1L, "TEXTO");
        when(tipoCampoRepository.findByIdTipoCampo(1L)).thenReturn(Optional.of(texto));
        when(formularioRepository.findByArtistaIdUsuarioAndIdServicioIsNullAndEsSistemaFalse(5L))
            .thenReturn(Optional.empty());
        when(usuarioRepository.findByIdUsuario(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> formularioService.guardarGlobal(5L,
            request(List.of(preguntaRequest(1L, "X", false, null)))))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Usuario no encontrado: 5");
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private TipoCampo tipo(Long id, String codigo) {
        TipoCampo tipo = new TipoCampo();
        tipo.setIdTipoCampo(id);
        tipo.setCodigo(codigo);
        tipo.setNombre(codigo);
        return tipo;
    }

    private Pregunta pregunta(TipoCampo tipo, String enunciado, int orden) {
        Pregunta pregunta = new Pregunta();
        pregunta.setTipoCampo(tipo);
        pregunta.setEnunciado(enunciado);
        pregunta.setOrden(orden);
        return pregunta;
    }

    private Formulario formulario(boolean esSistema, List<Pregunta> preguntas) {
        Formulario formulario = new Formulario();
        formulario.setEsSistema(esSistema);
        formulario.setPreguntas(new ArrayList<>(preguntas));
        return formulario;
    }

    private PreguntaRequest preguntaRequest(Long idTipoCampo, String enunciado, boolean obligatoria, List<String> opciones) {
        PreguntaRequest request = new PreguntaRequest();
        request.setIdTipoCampo(idTipoCampo);
        request.setEnunciado(enunciado);
        request.setObligatoria(obligatoria);
        request.setOpciones(opciones);
        return request;
    }

    private FormularioRequest request(List<PreguntaRequest> preguntas) {
        FormularioRequest request = new FormularioRequest();
        request.setPreguntas(preguntas);
        return request;
    }

    private Usuario usuario(Long id) {
        return Usuario.builder().idUsuario(id).build();
    }
}
