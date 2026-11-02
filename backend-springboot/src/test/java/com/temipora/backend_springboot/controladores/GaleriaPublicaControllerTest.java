package com.temipora.backend_springboot.controladores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.temipora.backend_springboot.modelo.EntregaArchivo;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.GaleriaPublicaService;
import com.temipora.backend_springboot.servicios.ImagenGaleriaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GaleriaPublicaControllerTest {

    @Mock
    private GaleriaPublicaService galeriaService;

    @Mock
    private ImagenGaleriaService imagenGaleriaService;

    @Mock
    private UsuarioService usuarioService;

    private GaleriaPublicaController controller;

    @BeforeEach
    void setUp() {
        controller = new GaleriaPublicaController(
            galeriaService, imagenGaleriaService, usuarioService);
        lenient().when(usuarioService.getByNombreUsuario("u"))
            .thenReturn(Usuario.builder().idUsuario(9L).nombreUsuario("u").build());
    }

    @Test
    void listar_sinAutenticacion_pasaViewerNulo() {
        controller.listar("cosmiq", 1L, 2L, 0, 12, null);

        verify(galeriaService).listar("cosmiq", null, 1L, 2L, 0, 12);
    }

    @Test
    void listar_conViewerAutenticado_resuelveElId() {
        controller.listar("cosmiq", null, null, 0, 12, auth("u", true));

        verify(galeriaService).listar("cosmiq", 9L, null, null, 0, 12);
    }

    @Test
    void listar_visitanteAnonimo_pasaViewerNulo() {
        controller.listar("cosmiq", null, null, 0, 12, auth("anonymousUser", true));

        verify(galeriaService).listar("cosmiq", null, null, null, 0, 12);
    }

    @Test
    void preview_devuelveLaImagenLimpia() {
        EntregaArchivo archivo = new EntregaArchivo();
        archivo.setIdArchivo(4L);
        when(galeriaService.autorizarPreview(eq(4L), any())).thenReturn(archivo);
        when(imagenGaleriaService.abrirLimpia(archivo)).thenReturn(mock(Resource.class));

        ResponseEntity<Resource> respuesta = controller.preview(4L, auth("u", true));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getHeaders().getContentType())
            .isEqualTo(org.springframework.http.MediaType.parseMediaType("image/webp"));
        assertThat(respuesta.getHeaders().getFirst("Content-Disposition"))
            .contains("galeria-4.webp");
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Authentication auth(String nombre, boolean autenticado) {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(nombre);
        lenient().when(authentication.isAuthenticated()).thenReturn(autenticado);
        return authentication;
    }
}
