package com.temipora.backend_springboot.controladores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.temipora.backend_springboot.modelo.EntregaArchivo;
import com.temipora.backend_springboot.modelo.EntregaHito;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.TipoDescarga;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.AccesoDenegadoException;
import com.temipora.backend_springboot.servicios.DescargaService;
import com.temipora.backend_springboot.servicios.EntregaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArchivoPedidoControllerTest {

    @Mock
    private EntregaService entregaService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private DescargaService descargaService;

    private ArchivoPedidoController controller;

    @BeforeEach
    void setUp() {
        controller = new ArchivoPedidoController(
            entregaService, usuarioService, descargaService);
    }

    @Test
    void original_comoArtista_devuelveElArchivo() {
        EntregaArchivo archivo = archivo("imagen/png");
        when(entregaService.obtenerArchivo(10L, 20L, 30L)).thenReturn(archivo);
        when(entregaService.puedeVerOriginal(archivo, true, false)).thenReturn(true);
        when(entregaService.abrir(archivo)).thenReturn(mock(Resource.class));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        ResponseEntity<Resource> respuesta = controller.original(
            10L, 20L, 30L, auth(2L, "ROLE_ARTISTA"), request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getHeaders().getFirst("Content-Disposition"))
            .contains("attachment");
        verify(descargaService).registrar(
            eq(10L), eq(2L), eq(30L), eq(TipoDescarga.ORIGINAL), eq("127.0.0.1"));
    }

    @Test
    void original_sinPermiso_lanzaAccesoDenegado() {
        EntregaArchivo archivo = archivo("imagen/png");
        when(entregaService.obtenerArchivo(10L, 20L, 30L)).thenReturn(archivo);
        when(entregaService.puedeVerOriginal(archivo, false, false)).thenReturn(false);

        assertThatThrownBy(() -> controller.original(
            10L, 20L, 30L, auth(3L, "ROLE_CLIENTE"), mock(HttpServletRequest.class)))
            .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void original_mimeInvalido_usaOctetStream() {
        EntregaArchivo archivo = archivo("esto no es un mime");
        when(entregaService.obtenerArchivo(10L, 20L, 30L)).thenReturn(archivo);
        when(entregaService.puedeVerOriginal(archivo, true, false)).thenReturn(true);
        when(entregaService.abrir(archivo)).thenReturn(mock(Resource.class));

        ResponseEntity<Resource> respuesta = controller.original(
            10L, 20L, 30L, auth(2L, "ROLE_ARTISTA"), mock(HttpServletRequest.class));

        assertThat(respuesta.getHeaders().getContentType())
            .isEqualTo(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
    }

    @Test
    void original_visitanteAjeno_lanzaAccesoDenegado() {
        EntregaArchivo archivo = archivo("imagen/png");
        when(entregaService.obtenerArchivo(10L, 20L, 30L)).thenReturn(archivo);

        assertThatThrownBy(() -> controller.original(
            10L, 20L, 30L, auth(99L, "ROLE_CLIENTE"), mock(HttpServletRequest.class)))
            .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void original_archivoRetiradoParaNoAdmin_lanzaNoSuchElement() {
        EntregaArchivo archivo = archivo("imagen/png");
        archivo.setRetirado(true);
        when(entregaService.obtenerArchivo(10L, 20L, 30L)).thenReturn(archivo);

        assertThatThrownBy(() -> controller.original(
            10L, 20L, 30L, auth(2L, "ROLE_ARTISTA"), mock(HttpServletRequest.class)))
            .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void preview_devuelveElPreviewInline() {
        EntregaArchivo archivo = archivo("imagen/png");
        when(entregaService.obtenerArchivo(10L, 20L, 30L)).thenReturn(archivo);
        when(entregaService.abrirPreview(archivo)).thenReturn(mock(Resource.class));

        ResponseEntity<Resource> respuesta = controller.preview(
            10L, 20L, 30L, auth(3L, "ROLE_CLIENTE"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getHeaders().getContentType())
            .isEqualTo(org.springframework.http.MediaType.parseMediaType("image/webp"));
    }

    @Test
    void zip_comoArtista_escribeElZip() throws Exception {
        EntregaService.EntregaConArchivos entrega = entregaConArchivos(20L, 5L);
        when(entregaService.obtenerEntrega(10L, 20L)).thenReturn(entrega);
        when(entregaService.puedeVerOriginal(any(HitoPedido.class), eq(true), eq(false)))
            .thenReturn(true);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));

        controller.zip(10L, 5L, 20L, auth(2L, "ROLE_ARTISTA"), request, response);

        verify(descargaService).registrar(
            eq(10L), eq(2L), eq(null), eq(TipoDescarga.ZIP), eq("127.0.0.1"));
        verify(entregaService).escribirZip(eq(entrega), any());
    }

    @Test
    void zip_hitoQueNoCorresponde_lanzaNoSuchElement() {
        when(entregaService.obtenerEntrega(10L, 20L)).thenReturn(entregaConArchivos(20L, 5L));

        assertThatThrownBy(() -> controller.zip(
            10L, 99L, 20L, auth(2L, "ROLE_ARTISTA"),
            mock(HttpServletRequest.class), mock(HttpServletResponse.class)))
            .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void zip_usuarioAjeno_lanzaAccesoDenegado() {
        when(entregaService.obtenerEntrega(10L, 20L)).thenReturn(entregaConArchivos(20L, 5L));

        assertThatThrownBy(() -> controller.zip(
            10L, 5L, 20L, auth(99L, "ROLE_CLIENTE"),
            mock(HttpServletRequest.class), mock(HttpServletResponse.class)))
            .isInstanceOf(AccesoDenegadoException.class);
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

    private EntregaArchivo archivo(String mime) {
        EntregaArchivo archivo = new EntregaArchivo();
        archivo.setIdArchivo(30L);
        archivo.setNombreOriginal("archivo.png");
        archivo.setMime(mime);
        archivo.setEntrega(entrega(20L, 5L));
        return archivo;
    }

    private EntregaHito entrega(Long idEntrega, Long idHito) {
        EntregaHito entrega = new EntregaHito();
        entrega.setIdEntrega(idEntrega);
        entrega.setNumero(1);
        entrega.setVigente(true);
        entrega.setHitoPedido(hito(idHito));
        return entrega;
    }

    private HitoPedido hito(Long idHito) {
        HitoPedido hito = new HitoPedido();
        hito.setIdHitoPedido(idHito);
        Pedido pedido = new Pedido();
        pedido.setIdPedido(10L);
        pedido.setArtista(Usuario.builder().idUsuario(2L).nombreUsuario("artista").build());
        pedido.setCliente(Usuario.builder().idUsuario(3L).nombreUsuario("cliente").build());
        hito.setPedido(pedido);
        return hito;
    }

    private EntregaService.EntregaConArchivos entregaConArchivos(
            Long idEntrega, Long idHito) {
        return new EntregaService.EntregaConArchivos(
            entrega(idEntrega, idHito), List.of());
    }
}
