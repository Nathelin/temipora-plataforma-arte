package com.temipora.backend_springboot.controladores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
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

import com.temipora.backend_springboot.modelo.CuentaCobroDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.UsuarioService;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoOAuthService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MercadoPagoOAuthControllerTest {

    @Mock
    private MercadoPagoOAuthService oauthService;

    @Mock
    private UsuarioService usuarioService;

    private MercadoPagoOAuthController controller;

    @BeforeEach
    void setUp() {
        controller = new MercadoPagoOAuthController(
            oauthService, usuarioService, "http://localhost:5173");
        lenient().when(usuarioService.getByNombreUsuario("u"))
            .thenReturn(Usuario.builder().idUsuario(1L).nombreUsuario("u").build());
    }

    @Test
    void inicio_devuelveLaUrlDeAutorizacion() {
        when(oauthService.urlAutorizacion(1L)).thenReturn("http://auth");

        assertThat(controller.inicio(auth()).get("url")).isEqualTo("http://auth");
    }

    @Test
    void estado_delegaEnElServicio() {
        when(oauthService.estado(1L)).thenReturn(new CuentaCobroDTO(true, "ACTIVA", "x", null));

        assertThat(controller.estado(auth()).conectada()).isTrue();
    }

    @Test
    void desconectar_devuelveNoContent() {
        ResponseEntity<Void> respuesta = controller.desconectar(auth());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(oauthService).desconectar(1L);
    }

    @Test
    void callback_ok_redirigeConExito() {
        ResponseEntity<Void> respuesta = controller.callback("code", "state");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(respuesta.getHeaders().getLocation().toString())
            .isEqualTo("http://localhost:5173/configuracion/cobros?mp=ok");
        verify(oauthService).procesarCallback("code", "state");
    }

    @Test
    void callback_sinCodigo_redirigeConError() {
        ResponseEntity<Void> respuesta = controller.callback(null, "state");

        assertThat(respuesta.getHeaders().getLocation().toString())
            .isEqualTo("http://localhost:5173/configuracion/cobros?mp=error");
    }

    @Test
    void callback_fallaElIntercambio_redirigeConError() {
        doThrow(new IllegalStateException("boom"))
            .when(oauthService).procesarCallback("code", "state");

        ResponseEntity<Void> respuesta = controller.callback("code", "state");

        assertThat(respuesta.getHeaders().getLocation().toString())
            .isEqualTo("http://localhost:5173/configuracion/cobros?mp=error");
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Authentication auth() {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn("u");
        return authentication;
    }
}
