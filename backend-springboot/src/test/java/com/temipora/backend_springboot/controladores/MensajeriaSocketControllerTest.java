package com.temipora.backend_springboot.controladores;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.ConversacionService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@ExtendWith(MockitoExtension.class)
class MensajeriaSocketControllerTest {

    @Mock
    private ConversacionService conversacionService;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private MensajeriaSocketController controller;

    @Test
    void escribiendo_marcaAlUsuarioAutenticado() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(2L);
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(usuario);

        controller.escribiendo(10L, principal());

        verify(conversacionService).marcarEscribiendo(10L, 2L);
    }

    @Test
    void escribiendo_siElServicioFalla_noPropagaElError() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(2L);
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(usuario);
        doThrow(new IllegalStateException("sin permiso"))
            .when(conversacionService)
            .marcarEscribiendo(anyLong(), anyLong());

        controller.escribiendo(10L, principal());
    }

    @Test
    void escribiendo_sinPrincipal_noHaceNada() {
        controller.escribiendo(10L, null);
    }

    private Principal principal() {
        return () -> "cliente";
    }
}
