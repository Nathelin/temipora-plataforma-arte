package com.temipora.backend_springboot.controladores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.temipora.backend_springboot.modelo.Compra;
import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EstadoEntregaItem;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.TipoEntrega;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CompraItemRepository;
import com.temipora.backend_springboot.servicios.AccesoDenegadoException;
import com.temipora.backend_springboot.servicios.AlmacenPrivadoService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@ExtendWith(MockitoExtension.class)
class ArchivoCompraControllerTest {

    @Mock private CompraItemRepository compraItemRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private AlmacenPrivadoService almacen;
    @Mock private Authentication authentication;

    @InjectMocks private ArchivoCompraController controller;

    @Test
    void archivo_compradorConPagoEntregado_devuelveElArchivo() {
        Usuario cliente = usuario(1L, "cliente");
        CompraItem item = itemDigital(10L, cliente, "productos/x.jpg");
        when(authentication.getName()).thenReturn("cliente");
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(compraItemRepository
            .findByIdCompraItemAndGrupoCompraClienteIdUsuario(5L, 1L))
            .thenReturn(Optional.of(item));
        when(almacen.abrir("productos/x.jpg"))
            .thenReturn(new ByteArrayResource("abc".getBytes(StandardCharsets.UTF_8)));

        ResponseEntity<?> respuesta = controller.archivo(10L, 5L, authentication);

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(respuesta.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
        assertThat(respuesta.getHeaders().getFirst("Content-Disposition"))
            .contains("attachment");
    }

    @Test
    void archivo_ajeno_lanzaAccesoDenegado() {
        when(authentication.getName()).thenReturn("otro");
        when(usuarioService.getByNombreUsuario("otro")).thenReturn(usuario(2L, "otro"));
        when(compraItemRepository
            .findByIdCompraItemAndGrupoCompraClienteIdUsuario(5L, 2L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.archivo(10L, 5L, authentication))
            .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void archivo_pagoPendiente_lanzaAccesoDenegado() {
        Usuario cliente = usuario(1L, "cliente");
        CompraItem item = itemDigital(10L, cliente, "productos/x.jpg");
        item.setEstadoEntrega(EstadoEntregaItem.PENDIENTE);
        when(authentication.getName()).thenReturn("cliente");
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(compraItemRepository
            .findByIdCompraItemAndGrupoCompraClienteIdUsuario(5L, 1L))
            .thenReturn(Optional.of(item));

        assertThatThrownBy(() -> controller.archivo(10L, 5L, authentication))
            .isInstanceOf(AccesoDenegadoException.class)
            .hasMessageContaining("acredita");
    }

    @Test
    void archivo_compraDistinta_lanzaAccesoDenegado() {
        Usuario cliente = usuario(1L, "cliente");
        CompraItem item = itemDigital(10L, cliente, "productos/x.jpg");
        when(authentication.getName()).thenReturn("cliente");
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(compraItemRepository
            .findByIdCompraItemAndGrupoCompraClienteIdUsuario(5L, 1L))
            .thenReturn(Optional.of(item));

        assertThatThrownBy(() -> controller.archivo(11L, 5L, authentication))
            .isInstanceOf(AccesoDenegadoException.class)
            .hasMessageContaining("acceso");
    }

    @Test
    void archivo_webp_devuelveMediaTypeWebp() {
        Usuario cliente = usuario(1L, "cliente");
        CompraItem item = itemDigital(10L, cliente, "productos/x.webp");
        when(authentication.getName()).thenReturn("cliente");
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(compraItemRepository
            .findByIdCompraItemAndGrupoCompraClienteIdUsuario(5L, 1L))
            .thenReturn(Optional.of(item));
        when(almacen.abrir("productos/x.webp"))
            .thenReturn(new ByteArrayResource("abc".getBytes(StandardCharsets.UTF_8)));

        ResponseEntity<?> respuesta = controller.archivo(10L, 5L, authentication);

        assertThat(respuesta.getHeaders().getContentType().toString())
            .isEqualTo("image/webp");
    }

    @Test
    void archivo_fisica_lanzaAccesoDenegado() {
        Usuario cliente = usuario(1L, "cliente");
        CompraItem item = itemDigital(10L, cliente, "productos/x.jpg");
        item.setTipoEntrega(TipoEntrega.FISICA);
        when(authentication.getName()).thenReturn("cliente");
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(compraItemRepository
            .findByIdCompraItemAndGrupoCompraClienteIdUsuario(5L, 1L))
            .thenReturn(Optional.of(item));

        assertThatThrownBy(() -> controller.archivo(10L, 5L, authentication))
            .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void archivo_sinClave_lanzaAccesoDenegado() {
        Usuario cliente = usuario(1L, "cliente");
        CompraItem item = itemDigital(10L, cliente, null);
        when(authentication.getName()).thenReturn("cliente");
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(compraItemRepository
            .findByIdCompraItemAndGrupoCompraClienteIdUsuario(5L, 1L))
            .thenReturn(Optional.of(item));

        assertThatThrownBy(() -> controller.archivo(10L, 5L, authentication))
            .isInstanceOf(AccesoDenegadoException.class)
            .hasMessageContaining("archivo original");
    }

    private CompraItem itemDigital(Long idCompra, Usuario cliente, String clave) {
        Compra compra = new Compra();
        compra.setIdCompra(idCompra);
        compra.setCliente(cliente);
        CompraGrupo grupo = new CompraGrupo();
        grupo.setCompra(compra);
        ProductoVariante variante = new ProductoVariante();
        variante.setClaveArchivoOriginal(clave);
        CompraItem item = new CompraItem();
        item.setIdCompraItem(5L);
        item.setGrupo(grupo);
        item.setVariante(variante);
        item.setTipoEntrega(TipoEntrega.DIGITAL);
        item.setEstadoEntrega(EstadoEntregaItem.ENTREGADO);
        item.setTituloProducto("Pieza");
        item.setNombreVariante("Única");
        return item;
    }

    private Usuario usuario(Long id, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombreUsuario(nombre);
        return usuario;
    }
}
