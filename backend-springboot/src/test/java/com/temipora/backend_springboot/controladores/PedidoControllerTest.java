package com.temipora.backend_springboot.controladores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.temipora.backend_springboot.modelo.PublicacionPedidoRequest;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.ConstanciaService;
import com.temipora.backend_springboot.servicios.DescargaService;
import com.temipora.backend_springboot.servicios.PedidoService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PedidoControllerTest {

    @Mock
    private PedidoService pedidoService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private ConstanciaService constanciaService;

    @Mock
    private DescargaService descargaService;

    private PedidoController controller;

    @BeforeEach
    void setUp() {
        controller = new PedidoController(
            pedidoService, usuarioService, constanciaService, descargaService);
    }

    @Test
    void obtener_delegaEnElServicio() {
        controller.obtener(10L, auth(2L, "ROLE_CLIENTE"));

        verify(pedidoService).obtener(10L, 2L, false);
    }

    @Test
    void obtener_comoAdmin_pasaEsAdminTrue() {
        controller.obtener(10L, auth(1L, "ROLE_ADMIN"));

        verify(pedidoService).obtener(10L, 1L, true);
    }

    @Test
    void constancia_generaElPdfYLoRegistra() {
        when(constanciaService.generar(any())).thenReturn(new byte[] {1, 2, 3});
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        ResponseEntity<byte[]> respuesta = controller.constancia(
            10L, auth(2L, "ROLE_CLIENTE"), request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(3);
        assertThat(respuesta.getHeaders().getFirst("Content-Disposition"))
            .contains("constancia-pedido-10.pdf");
        verify(descargaService).registrar(
            eq(10L), eq(2L), eq(null), any(), eq("127.0.0.1"));
    }

    @Test
    void recibidos_construyeElFiltroYDelega() {
        controller.recibidos(null, null, null, null, null, 0, 20,
            auth(2L, "ROLE_ARTISTA"));

        verify(pedidoService).listarRecibidos(eq(2L), any(), eq(0), eq(20));
    }

    @Test
    void enviados_construyeElFiltroYDelega() {
        controller.enviados(null, null, null, null, null, 0, 20,
            auth(2L, "ROLE_CLIENTE"));

        verify(pedidoService).listarEnviados(eq(2L), any(), eq(0), eq(20));
    }

    @Test
    void contadores_bandejaRecibidos() {
        controller.contadores("recibidos", auth(2L, "ROLE_ARTISTA"));

        verify(pedidoService).contadores(2L, true);
    }

    @Test
    void contadores_bandejaEnviados() {
        controller.contadores("enviados", auth(2L, "ROLE_CLIENTE"));

        verify(pedidoService).contadores(2L, false);
    }

    @Test
    void pagarHito_delegaEnElServicio() {
        controller.pagarHito(10L, 5L, auth(2L, "ROLE_CLIENTE"));

        verify(pedidoService).pagarHito(10L, 5L, 2L);
    }

    @Test
    void aprobar_delegaEnElServicio() {
        controller.aprobar(10L, 5L, auth(2L, "ROLE_CLIENTE"));

        verify(pedidoService).aprobar(10L, 5L, 2L);
    }

    @Test
    void solicitarRevision_delegaEnElServicio() {
        controller.solicitarRevision(10L, 5L, null, auth(2L, "ROLE_CLIENTE"));

        verify(pedidoService).solicitarRevision(eq(10L), eq(5L), eq(2L), eq(null));
    }

    @Test
    void entregar_delegaEnElServicio() {
        controller.entregar(10L, 5L, null, List.of(), auth(2L, "ROLE_ARTISTA"));

        verify(pedidoService).entregar(eq(10L), eq(5L), eq(2L), eq(null), eq(List.of()));
    }

    @Test
    void cancelar_delegaEnElServicio() {
        controller.cancelar(10L, auth(2L, "ROLE_CLIENTE"));

        verify(pedidoService).cancelar(10L, 2L);
    }

    @Test
    void autorizarPublicacion_pasaElFlag() {
        PublicacionPedidoRequest request = new PublicacionPedidoRequest();
        request.setAutoriza(true);

        controller.autorizarPublicacion(10L, request, auth(2L, "ROLE_ARTISTA"));

        verify(pedidoService).autorizarPublicacion(10L, 2L, true);
    }

    @Test
    void ocultarPublicacion_pasaElFlag() {
        PublicacionPedidoRequest request = new PublicacionPedidoRequest();
        request.setOcultar(true);

        controller.ocultarPublicacion(10L, request, auth(2L, "ROLE_CLIENTE"));

        verify(pedidoService).ocultarPublicacion(10L, 2L, true);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Authentication auth(Long idUsuario, String... roles) {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn("u");
        List<GrantedAuthority> autoridades = new ArrayList<>();
        for (String rol : roles) {
            autoridades.add(new SimpleGrantedAuthority(rol));
        }
        doReturn(autoridades).when(authentication).getAuthorities();
        lenient().when(usuarioService.getByNombreUsuario("u"))
            .thenReturn(Usuario.builder().idUsuario(idUsuario).nombreUsuario("u").build());
        return authentication;
    }
}
