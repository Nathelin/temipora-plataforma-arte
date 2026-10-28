package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.EntregaArchivo;
import com.temipora.backend_springboot.modelo.EntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EntregaArchivoRepository;
import com.temipora.backend_springboot.repositorios.GaleriaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class GaleriaPublicaServiceTest {

    private static final Long CLIENTE = 1L;
    private static final Long ARTISTA = 2L;
    private static final Long ARCHIVO = 30L;

    @Mock
    private GaleriaRepository galeriaRepository;
    @Mock
    private EntregaArchivoRepository archivoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioService usuarioService;

    private GaleriaPublicaService service;
    private Pedido pedido;

    @BeforeEach
    void setUp() {
        service = new GaleriaPublicaService(
            galeriaRepository,
            archivoRepository,
            usuarioRepository,
            usuarioService);
        pedido = pedido();
    }

    @Test
    void preview_pedidoPublicoYAutorizado_noRequiereSesion() {
        pedido.getCliente().setPedidosPublicos(true);
        when(archivoRepository.buscarParaGaleria(ARCHIVO))
            .thenReturn(Optional.of(archivo(pedido)));

        assertThat(service.autorizarPreview(ARCHIVO, null)).isNotNull();
    }

    @Test
    void preview_clientePrivado_anonimoNoPuedeVer() {
        when(archivoRepository.buscarParaGaleria(ARCHIVO))
            .thenReturn(Optional.of(archivo(pedido)));

        assertThatThrownBy(() -> service.autorizarPreview(ARCHIVO, null))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void preview_clientePrivado_duenioPuedeVerSuGaleria() {
        when(archivoRepository.buscarParaGaleria(ARCHIVO))
            .thenReturn(Optional.of(archivo(pedido)));

        assertThat(service.autorizarPreview(ARCHIVO, CLIENTE)).isNotNull();
    }

    @Test
    void preview_pedidoOcultoPorCliente_anonimoNoPuedeVer() {
        pedido.getCliente().setPedidosPublicos(true);
        pedido.setPublicacionOcultadaCliente(true);
        when(archivoRepository.buscarParaGaleria(ARCHIVO))
            .thenReturn(Optional.of(archivo(pedido)));

        assertThatThrownBy(() -> service.autorizarPreview(ARCHIVO, null))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void preview_entregaNoFinal_noSeSirve() {
        pedido.getCliente().setPedidosPublicos(true);
        EntregaArchivo archivo = archivo(pedido);
        archivo.getEntrega().setEsFinal(false);
        when(archivoRepository.buscarParaGaleria(ARCHIVO))
            .thenReturn(Optional.of(archivo));

        assertThatThrownBy(() -> service.autorizarPreview(ARCHIVO, null))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void preview_pedidoSinAutorizacionDelArtista_noSeSirve() {
        pedido.getCliente().setPedidosPublicos(true);
        pedido.setPublicacionAutorizada(false);
        when(archivoRepository.buscarParaGaleria(ARCHIVO))
            .thenReturn(Optional.of(archivo(pedido)));

        assertThatThrownBy(() -> service.autorizarPreview(ARCHIVO, null))
            .isInstanceOf(NoSuchElementException.class);
    }

    private Pedido pedido() {
        Usuario cliente = new Usuario();
        cliente.setIdUsuario(CLIENTE);
        Usuario artista = new Usuario();
        artista.setIdUsuario(ARTISTA);

        Pedido pedido = new Pedido();
        pedido.setIdPedido(10L);
        pedido.setCliente(cliente);
        pedido.setArtista(artista);
        pedido.setEstado(EstadoPedido.FINALIZADO);
        pedido.setPublicacionAutorizada(true);
        pedido.setPublicacionOcultadaCliente(false);
        return pedido;
    }

    private EntregaArchivo archivo(Pedido pedido) {
        HitoPedido hito = new HitoPedido();
        hito.setIdHitoPedido(20L);
        hito.setPedido(pedido);

        EntregaHito entrega = new EntregaHito();
        entrega.setIdEntrega(25L);
        entrega.setHitoPedido(hito);
        entrega.setVigente(true);
        entrega.setEsFinal(true);
        entrega.setRetirada(false);

        EntregaArchivo archivo = new EntregaArchivo();
        archivo.setIdArchivo(ARCHIVO);
        archivo.setEntrega(entrega);
        archivo.setMime("image/png");
        archivo.setRetirado(false);
        return archivo;
    }
}
