package com.temipora.backend_springboot.controladores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.ConfiguracionMensajeriaService;
import com.temipora.backend_springboot.servicios.ConversacionService;
import com.temipora.backend_springboot.servicios.MensajeService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConversacionControllerTest {

    @Mock
    private ConversacionService conversacionService;

    @Mock
    private MensajeService mensajeService;

    @Mock
    private ConfiguracionMensajeriaService configuracionService;

    @Mock
    private UsuarioService usuarioService;

    private ConversacionController controller;

    @BeforeEach
    void setUp() {
        controller = new ConversacionController(
            conversacionService, mensajeService, configuracionService, usuarioService);
    }

    @Test
    void crear_devuelveCreated() {
        ResponseEntity<?> respuesta = controller.crear(null, auth(1L));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(conversacionService).crear(eq(1L), eq(null));
    }

    @Test
    void listar_delegaConLosFiltros() {
        controller.listar(null, false, null, 0, 20, auth(1L));

        verify(conversacionService).listar(1L, null, false, null, 0, 20);
    }

    @Test
    void contador_delega() {
        controller.contador(auth(1L));

        verify(conversacionService).contador(1L);
    }

    @Test
    void estadoConUsuario_delega() {
        controller.estadoConUsuario("artista", auth(1L));

        verify(conversacionService).estadoConUsuario(1L, "artista");
    }

    @Test
    void configuracion_delega() {
        controller.configuracion();

        verify(configuracionService).obtenerEstado();
    }

    @Test
    void obtener_delega() {
        controller.obtener(7L, auth(1L));

        verify(conversacionService).obtener(7L, 1L);
    }

    @Test
    void aceptar_delega() {
        controller.aceptar(7L, auth(1L));

        verify(conversacionService).aceptar(7L, 1L);
    }

    @Test
    void rechazar_delega() {
        controller.rechazar(7L, null, auth(1L));

        verify(conversacionService).rechazar(7L, 1L, null);
    }

    @Test
    void cancelar_delega() {
        controller.cancelar(7L, auth(1L));

        verify(conversacionService).cancelar(7L, 1L);
    }

    @Test
    void archivar_yDesarchivar_pasanElFlag() {
        controller.archivar(7L, auth(1L));
        controller.desarchivar(7L, auth(1L));

        verify(conversacionService).archivar(7L, 1L, true);
        verify(conversacionService).archivar(7L, 1L, false);
    }

    @Test
    void historial_delega() {
        controller.historial(7L, 4L, 10, auth(1L));

        verify(mensajeService).historial(7L, 1L, 4L, 10);
    }

    @Test
    void enviar_devuelveCreated() {
        ResponseEntity<?> respuesta = controller.enviar(7L, null, auth(1L));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(mensajeService).enviar(7L, 1L, null);
    }

    @Test
    void marcarLeido_devuelveNoContent() {
        ResponseEntity<Void> respuesta = controller.marcarLeido(7L, auth(1L));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(mensajeService).marcarLeido(7L, 1L);
    }

    @Test
    void marcarEscribiendo_devuelveNoContent() {
        ResponseEntity<Void> respuesta = controller.marcarEscribiendo(7L, auth(1L));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(conversacionService).marcarEscribiendo(7L, 1L);
    }

    @Test
    void estadoEscritura_delega() {
        controller.estadoEscritura(7L, auth(1L));

        verify(conversacionService).estadoEscritura(7L, 1L);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Authentication auth(Long idUsuario) {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn("u");
        lenient().when(usuarioService.getByNombreUsuario("u"))
            .thenReturn(Usuario.builder().idUsuario(idUsuario).nombreUsuario("u").build());
        return authentication;
    }
}
