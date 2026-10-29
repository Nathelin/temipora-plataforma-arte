package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.EntregaArchivo;
import com.temipora.backend_springboot.modelo.EntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.FiltroGaleriaDTO;
import com.temipora.backend_springboot.modelo.GaleriaPedidosDTO;
import com.temipora.backend_springboot.modelo.GaleriaPiezaDTO;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.ItemSolicitud;
import com.temipora.backend_springboot.modelo.ModoRecepcionSolicitud;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Servicio;
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
    @Mock
    private ReseniaPublicaService reseniaPublicaService;

    private GaleriaPublicaService service;
    private Pedido pedido;

    @BeforeEach
    void setUp() {
        service = new GaleriaPublicaService(
            galeriaRepository,
            archivoRepository,
            usuarioRepository,
            usuarioService,
            reseniaPublicaService);
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

    @Test
    void listar_galeriaPrivadaDeOtroUsuario_devuelveVacia() {
        pedido.getCliente().setPedidosPublicos(false);
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(pedido.getCliente());

        GaleriaPedidosDTO resultado = service.listar(
            "cliente", ARTISTA, null, null, -1, 999);

        assertThat(resultado.isVisible()).isFalse();
        assertThat(resultado.isEsDuenio()).isFalse();
        assertThat(resultado.getPiezas().getContenido()).isEmpty();
        assertThat(resultado.getPiezas().getPagina()).isZero();
        assertThat(resultado.getPiezas().getTamanio()).isEqualTo(999);
        assertThat(resultado.getServicios()).isEmpty();
        assertThat(resultado.getArtistas()).isEmpty();
        verifyNoInteractions(galeriaRepository, archivoRepository);
    }

    @Test
    void listar_duenioConPieza_armaPiezaConArchivosYFiltros() {
        Usuario cliente = pedido.getCliente();
        cliente.setPedidosPublicos(false);
        Servicio servicio = servicio(7L, "Ilustración");
        pedido.setServicio(servicio);
        ItemSolicitud item = item(21L, "Dibujo 1");
        EntregaArchivo archivo = archivo(pedido);
        archivo.setItemSolicitud(item);
        archivo.setAncho(800);
        archivo.setAlto(600);
        archivo.setOrden(2);

        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(galeriaRepository.findPaginaGaleria(
                eq(CLIENTE), eq(EstadoPedido.FINALIZADO), eq(true), eq(CLIENTE),
                eq(false), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<Object[]>(
                List.<Object[]>of(new Object[] { item }), PageRequest.of(0, 12), 1));
        when(archivoRepository.listarGaleriaPorItems(List.of(21L)))
            .thenReturn(List.of(archivo));
        when(galeriaRepository.serviciosDisponibles(
                eq(CLIENTE), eq(EstadoPedido.FINALIZADO), eq(true), eq(CLIENTE), eq(false)))
            .thenReturn(List.of(servicio));
        when(galeriaRepository.artistasDisponibles(
                eq(CLIENTE), eq(EstadoPedido.FINALIZADO), eq(true), eq(CLIENTE), eq(false)))
            .thenReturn(List.of(pedido.getArtista()));

        GaleriaPedidosDTO resultado = service.listar(
            "cliente", CLIENTE, null, null, 0, 12);

        assertThat(resultado.isEsDuenio()).isTrue();
        assertThat(resultado.getPiezas().getContenido()).hasSize(1);
        GaleriaPiezaDTO pieza = resultado.getPiezas().getContenido().get(0);
        assertThat(pieza.getIdPieza()).isEqualTo(21L);
        assertThat(pieza.getNombreDibujo()).isEqualTo("Dibujo 1");
        assertThat(pieza.getIdServicio()).isEqualTo(7L);
        assertThat(pieza.getNombreServicio()).isEqualTo("Ilustración");
        assertThat(pieza.isServicioDisponible()).isTrue();
        assertThat(pieza.getIdPedido()).isEqualTo(10L);
        assertThat(pieza.getOcultoPorCliente()).isFalse();
        assertThat(pieza.getArchivos()).hasSize(1);
        assertThat(pieza.getArchivos().get(0).getUrlPreview())
            .isEqualTo("/galeria/archivos/30/preview");
        assertThat(pieza.getArchivos().get(0).getAncho()).isEqualTo(800);
        assertThat(pieza.getArchivos().get(0).getAlto()).isEqualTo(600);
        assertThat(pieza.getArchivos().get(0).getOrden()).isEqualTo(2);
        assertThat(resultado.getServicios())
            .extracting(FiltroGaleriaDTO::getNombre)
            .containsExactly("Ilustración");
        assertThat(resultado.getArtistas())
            .extracting(FiltroGaleriaDTO::getNombreUsuario)
            .containsExactly("artista");
    }

    @Test
    void listar_visitanteConPreferenciaOcultar_ocultaSensibles() {
        Usuario cliente = pedido.getCliente();
        cliente.setPedidosPublicos(true);
        Servicio servicio = servicio(7L, "Ilustración");
        servicio.setContenidoSensible(true);
        servicio.setModoRecepcion(ModoRecepcionSolicitud.SOLO_ESPERA);
        pedido.setServicio(servicio);
        ItemSolicitud item = item(21L, "Dibujo 1");
        EntregaArchivo archivo = archivo(pedido);
        archivo.setItemSolicitud(item);

        Usuario visitante = new Usuario();
        visitante.setIdUsuario(ARTISTA);
        visitante.setPreferenciaSensible(EPreferenciaContenidoSensible.OCULTAR);

        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(usuarioRepository.findByIdUsuario(ARTISTA))
            .thenReturn(Optional.of(visitante));
        when(galeriaRepository.findPaginaGaleria(
                eq(CLIENTE), eq(EstadoPedido.FINALIZADO), eq(false), eq(ARTISTA),
                eq(true), eq(7L), eq(ARTISTA), any(Pageable.class)))
            .thenReturn(new PageImpl<Object[]>(
                List.<Object[]>of(new Object[] { item }), PageRequest.of(0, 12), 1));
        when(archivoRepository.listarGaleriaPorItems(List.of(21L)))
            .thenReturn(List.of(archivo));
        when(galeriaRepository.serviciosDisponibles(
                eq(CLIENTE), eq(EstadoPedido.FINALIZADO), eq(false), eq(ARTISTA), eq(true)))
            .thenReturn(List.of(servicio));
        when(galeriaRepository.artistasDisponibles(
                eq(CLIENTE), eq(EstadoPedido.FINALIZADO), eq(false), eq(ARTISTA), eq(true)))
            .thenReturn(List.of(pedido.getArtista()));

        GaleriaPedidosDTO resultado = service.listar(
            "cliente", ARTISTA, 7L, ARTISTA, 0, 12);

        GaleriaPiezaDTO pieza = resultado.getPiezas().getContenido().get(0);
        assertThat(resultado.isEsDuenio()).isFalse();
        assertThat(pieza.isContenidoSensible()).isTrue();
        assertThat(pieza.isRequiereAdvertencia()).isFalse();
        assertThat(pieza.isServicioDisponible()).isTrue();
        assertThat(pieza.getIdPedido()).isNull();
        assertThat(pieza.getOcultoPorCliente()).isNull();
    }

    @Test
    void listar_piezaSinArchivos_seOmiteYNormalizaPaginacion() {
        Usuario cliente = pedido.getCliente();
        cliente.setPedidosPublicos(true);
        pedido.setServicio(servicio(7L, "Ilustración"));
        ItemSolicitud item = item(21L, "Dibujo 1");

        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(galeriaRepository.findPaginaGaleria(
                eq(CLIENTE), eq(EstadoPedido.FINALIZADO), eq(false), isNull(),
                eq(false), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<Object[]>(
                List.<Object[]>of(new Object[] { item }), PageRequest.of(0, 50), 1));
        when(archivoRepository.listarGaleriaPorItems(List.of(21L)))
            .thenReturn(List.of());
        when(galeriaRepository.serviciosDisponibles(
                eq(CLIENTE), eq(EstadoPedido.FINALIZADO), eq(false), isNull(), eq(false)))
            .thenReturn(List.of());
        when(galeriaRepository.artistasDisponibles(
                eq(CLIENTE), eq(EstadoPedido.FINALIZADO), eq(false), isNull(), eq(false)))
            .thenReturn(List.of());

        GaleriaPedidosDTO resultado = service.listar(
            "cliente", null, null, null, -5, 999);

        assertThat(resultado.getPiezas().getContenido()).isEmpty();
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(galeriaRepository).findPaginaGaleria(
            eq(CLIENTE), eq(EstadoPedido.FINALIZADO), eq(false), isNull(),
            eq(false), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void listar_sinPiezas_noConsultaArchivos() {
        Usuario cliente = pedido.getCliente();
        cliente.setPedidosPublicos(true);
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(galeriaRepository.findPaginaGaleria(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), any(),
                any(Pageable.class)))
            .thenReturn(new PageImpl<Object[]>(List.of(), PageRequest.of(0, 12), 0));
        when(galeriaRepository.serviciosDisponibles(
                any(), any(), anyBoolean(), any(), anyBoolean()))
            .thenReturn(List.of());
        when(galeriaRepository.artistasDisponibles(
                any(), any(), anyBoolean(), any(), anyBoolean()))
            .thenReturn(List.of());

        GaleriaPedidosDTO resultado = service.listar(
            "cliente", null, null, null, 0, 12);

        assertThat(resultado.getPiezas().getContenido()).isEmpty();
        verifyNoInteractions(archivoRepository);
    }

    @Test
    void listar_artistaSinNombreDisplay_usaNombreUsuarioEnElFiltro() {
        Usuario cliente = pedido.getCliente();
        cliente.setPedidosPublicos(true);
        pedido.getArtista().setNombreDisplay("  ");
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(galeriaRepository.findPaginaGaleria(
                any(), any(), anyBoolean(), any(), anyBoolean(), any(), any(),
                any(Pageable.class)))
            .thenReturn(new PageImpl<Object[]>(List.of(), PageRequest.of(0, 12), 0));
        when(galeriaRepository.serviciosDisponibles(
                any(), any(), anyBoolean(), any(), anyBoolean()))
            .thenReturn(List.of());
        when(galeriaRepository.artistasDisponibles(
                any(), any(), anyBoolean(), any(), anyBoolean()))
            .thenReturn(List.of(pedido.getArtista()));

        GaleriaPedidosDTO resultado = service.listar(
            "cliente", null, null, null, 0, 12);

        assertThat(resultado.getArtistas()).hasSize(1);
        assertThat(resultado.getArtistas().get(0).getNombre())
            .isEqualTo("artista");
        assertThat(resultado.getArtistas().get(0).getNombreUsuario())
            .isEqualTo("artista");
    }

    private Pedido pedido() {
        Usuario cliente = new Usuario();
        cliente.setIdUsuario(CLIENTE);
        cliente.setNombreUsuario("cliente");
        Usuario artista = new Usuario();
        artista.setIdUsuario(ARTISTA);
        artista.setNombreUsuario("artista");

        Pedido pedido = new Pedido();
        pedido.setIdPedido(10L);
        pedido.setCliente(cliente);
        pedido.setArtista(artista);
        pedido.setEstado(EstadoPedido.FINALIZADO);
        pedido.setPublicacionAutorizada(true);
        pedido.setPublicacionOcultadaCliente(false);
        return pedido;
    }

    private ItemSolicitud item(Long id, String nombre) {
        ItemSolicitud item = new ItemSolicitud();
        item.setIdItemSolicitud(id);
        item.setNombre(nombre);
        item.setOrden(0);
        return item;
    }

    private Servicio servicio(Long id, String nombre) {
        Servicio servicio = new Servicio();
        servicio.setIdServicio(id);
        servicio.setNombre(nombre);
        return servicio;
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
