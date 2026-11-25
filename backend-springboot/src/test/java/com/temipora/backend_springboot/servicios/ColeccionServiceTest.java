package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.temipora.backend_springboot.modelo.ColeccionProductoDTO;
import com.temipora.backend_springboot.modelo.ColeccionProductosDTO;
import com.temipora.backend_springboot.modelo.ColeccionVisibilidadDTO;
import com.temipora.backend_springboot.modelo.Compra;
import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.ContenidoSensible;
import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoEntregaItem;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.OrigenCompraItem;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.ReseniaPublicaResumenDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CompraItemRepository;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ColeccionServiceTest {

    @Mock private CompraItemRepository compraItemRepository;
    @Mock private EventoLedgerRepository eventoLedgerRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private CompraService compraService;
    @Mock private ReseniaPublicaService reseniaPublicaService;

    private ColeccionService service;

    @BeforeEach
    void setUp() {
        service = new ColeccionService(
            compraItemRepository, eventoLedgerRepository, usuarioRepository,
            usuarioService, compraService, reseniaPublicaService);
    }

    @Test
    void listarDelDuenioIncluyeSugerenciaYPropiedadesCompletas() {
        Usuario duenio = usuario(1L, "duenio");
        duenio.setColeccionPublica(false);
        when(usuarioService.getByNombreUsuario("duenio")).thenReturn(duenio);

        CompraItem completo = itemCompleto(100L, 10L, 500L);
        CompraItem sinProducto = new CompraItem();
        sinProducto.setIdCompraItem(101L);
        when(compraItemRepository.findColeccion(
            eq(1L), eq(true), eq(false), eq(null),
            eq(EstadoCompraGrupo.PAGADO), eq(EstadoEntregaItem.CANCELADO),
            any())).thenReturn(paginaDe(completo, sinProducto));
        when(eventoLedgerRepository.findVarianteIdsConLedger(anyCollection()))
            .thenReturn(Set.of(500L));
        ReseniaPublicaResumenDTO resenia =
            ReseniaPublicaResumenDTO.builder().anonima(true).build();
        when(reseniaPublicaService.resumenesDeProductosPorAutor(
            anyList(), eq(1L))).thenReturn(Map.of(10L, resenia));
        when(compraService.imagenDeItem(any())).thenReturn("imagen.png");
        Usuario sinDisplay = usuario(5L, "artista");
        sinDisplay.setNombreDisplay(" ");
        Usuario conDisplay = usuario(6L, "artista2");
        conDisplay.setNombreDisplay("La Artista");
        when(compraItemRepository.artistasDeColeccion(
            1L, true, false, EstadoCompraGrupo.PAGADO,
            EstadoEntregaItem.CANCELADO))
            .thenReturn(List.of(sinDisplay, conDisplay));
        when(compraItemRepository.contarColeccionables(
            1L, EstadoCompraGrupo.PAGADO, EstadoEntregaItem.CANCELADO))
            .thenReturn(2L);

        ColeccionProductosDTO coleccion =
            service.listar("duenio", 1L, null, 0, 10);

        assertThat(coleccion.isVisible()).isFalse();
        assertThat(coleccion.isEsDuenio()).isTrue();
        assertThat(coleccion.getSugerenciaPendiente()).isTrue();
        assertThat(coleccion.getProductos().getContenido()).hasSize(2);
        assertThat(coleccion.getProductos().getTotalElementos()).isEqualTo(2);
        assertThat(coleccion.getArtistas()).hasSize(2);
        assertThat(coleccion.getArtistas().get(0).getNombre())
            .isEqualTo("artista");
        assertThat(coleccion.getArtistas().get(1).getNombre())
            .isEqualTo("La Artista");

        ColeccionProductoDTO producto = coleccion.getProductos()
            .getContenido().get(0);
        assertThat(producto.getIdCompraItem()).isEqualTo(100L);
        assertThat(producto.getIdCompra()).isEqualTo(70L);
        assertThat(producto.getIdProducto()).isEqualTo(10L);
        assertThat(producto.getIdVariante()).isEqualTo(500L);
        assertThat(producto.getImagenUrl()).isEqualTo("imagen.png");
        assertThat(producto.getFechaCompra())
            .isEqualTo(LocalDateTime.of(2026, 11, 1, 9, 0));
        assertThat(producto.getFechaEntrega())
            .isEqualTo(LocalDateTime.of(2026, 11, 5, 9, 0));
        assertThat(producto.isEsReventa()).isTrue();
        assertThat(producto.isEsPiezaUnica()).isTrue();
        assertThat(producto.getEstadoProducto()).isEqualTo("DISPONIBLE");
        assertThat(producto.isProductoDisponible()).isTrue();
        assertThat(producto.isContenidoSensible()).isTrue();
        assertThat(producto.getContenidosSensibles())
            .containsExactly("DESNUDOS");
        assertThat(producto.isRequiereAdvertencia()).isFalse();
        assertThat(producto.getOculto()).isTrue();
        assertThat(producto.getResenia()).isSameAs(resenia);
        assertThat(producto.getArtista().getNombreUsuario())
            .isEqualTo("artista");

        ColeccionProductoDTO vacio = coleccion.getProductos()
            .getContenido().get(1);
        assertThat(vacio.getIdProducto()).isNull();
        assertThat(vacio.getIdCompra()).isNull();
        assertThat(vacio.isProductoDisponible()).isFalse();
        assertThat(vacio.isEsPiezaUnica()).isFalse();
        assertThat(vacio.getContenidosSensibles()).isEmpty();
        assertThat(vacio.getOculto()).isFalse();
    }

    @Test
    void listarPublicaSinDuenioAnonimizaReseniasYAdvierte() {
        Usuario duenio = usuario(1L, "duenio");
        duenio.setColeccionPublica(true);
        when(usuarioService.getByNombreUsuario("duenio")).thenReturn(duenio);
        Usuario viewer = usuario(2L, "viewer");
        viewer.setPreferenciaSensible(EPreferenciaContenidoSensible.ADVERTIR);
        when(usuarioRepository.findByIdUsuario(2L))
            .thenReturn(Optional.of(viewer));

        CompraItem completo = itemCompleto(100L, 10L, 500L);
        when(compraItemRepository.findColeccion(
            eq(1L), eq(false), eq(false), eq(4L),
            eq(EstadoCompraGrupo.PAGADO), eq(EstadoEntregaItem.CANCELADO),
            any())).thenReturn(paginaDe(completo));
        when(eventoLedgerRepository.findVarianteIdsConLedger(anyCollection()))
            .thenReturn(Set.of());
        when(reseniaPublicaService.resumenesDeProductosPorAutor(
            anyList(), eq(1L))).thenReturn(Map.of(
                10L, ReseniaPublicaResumenDTO.builder().anonima(true).build()));
        when(compraService.imagenDeItem(any())).thenReturn("imagen.png");
        when(compraItemRepository.artistasDeColeccion(
            1L, false, false, EstadoCompraGrupo.PAGADO,
            EstadoEntregaItem.CANCELADO)).thenReturn(List.of());

        ColeccionProductosDTO coleccion =
            service.listar("duenio", 2L, 4L, 0, 10);

        assertThat(coleccion.isVisible()).isTrue();
        assertThat(coleccion.isEsDuenio()).isFalse();
        assertThat(coleccion.getSugerenciaPendiente()).isNull();
        ColeccionProductoDTO producto = coleccion.getProductos()
            .getContenido().get(0);
        assertThat(producto.getResenia()).isNull();
        assertThat(producto.isRequiereAdvertencia()).isTrue();
        assertThat(producto.getOculto()).isNull();
        assertThat(producto.isEsPiezaUnica()).isFalse();
    }

    @Test
    void listarOcultaSensiblesSegunLaPreferenciaDelViewer() {
        Usuario duenio = usuario(1L, "duenio");
        duenio.setColeccionPublica(true);
        when(usuarioService.getByNombreUsuario("duenio")).thenReturn(duenio);
        Usuario viewer = usuario(3L, "viewer");
        viewer.setPreferenciaSensible(EPreferenciaContenidoSensible.OCULTAR);
        when(usuarioRepository.findByIdUsuario(3L))
            .thenReturn(Optional.of(viewer));
        when(compraItemRepository.findColeccion(
            eq(1L), eq(false), eq(true), eq(null),
            eq(EstadoCompraGrupo.PAGADO), eq(EstadoEntregaItem.CANCELADO),
            any())).thenReturn(paginaDe());
        when(reseniaPublicaService.resumenesDeProductosPorAutor(
            anyList(), eq(1L))).thenReturn(Map.of());
        when(compraItemRepository.artistasDeColeccion(
            1L, false, true, EstadoCompraGrupo.PAGADO,
            EstadoEntregaItem.CANCELADO)).thenReturn(List.of());

        ColeccionProductosDTO coleccion =
            service.listar("duenio", 3L, null, -1, 500);

        assertThat(coleccion.getProductos().getContenido()).isEmpty();
        assertThat(coleccion.getProductos().getPagina()).isZero();
        assertThat(coleccion.getProductos().getTamanio()).isEqualTo(10);
        verify(compraItemRepository).findColeccion(
            eq(1L), eq(false), eq(true), eq(null),
            eq(EstadoCompraGrupo.PAGADO), eq(EstadoEntregaItem.CANCELADO),
            any());
    }

    @Test
    void listarSinViewerUsaAdvertirPorDefecto() {
        Usuario duenio = usuario(1L, "duenio");
        duenio.setColeccionPublica(true);
        when(usuarioService.getByNombreUsuario("duenio")).thenReturn(duenio);
        CompraItem completo = itemCompleto(100L, 10L, 500L);
        when(compraItemRepository.findColeccion(
            eq(1L), eq(false), eq(false), eq(null),
            eq(EstadoCompraGrupo.PAGADO), eq(EstadoEntregaItem.CANCELADO),
            any())).thenReturn(paginaDe(completo));
        when(eventoLedgerRepository.findVarianteIdsConLedger(anyCollection()))
            .thenReturn(Set.of(500L));
        when(reseniaPublicaService.resumenesDeProductosPorAutor(
            anyList(), eq(1L))).thenReturn(Map.of());
        when(compraService.imagenDeItem(any())).thenReturn("imagen.png");
        when(compraItemRepository.artistasDeColeccion(
            1L, false, false, EstadoCompraGrupo.PAGADO,
            EstadoEntregaItem.CANCELADO)).thenReturn(List.of());

        ColeccionProductosDTO coleccion =
            service.listar("duenio", null, null, 0, 10);

        assertThat(coleccion.getProductos().getContenido().get(0)
            .isRequiereAdvertencia()).isTrue();
        verify(usuarioRepository, never()).findByIdUsuario(any());
    }

    @Test
    void listarPrivadaSinSerDuenioDevuelveVacia() {
        Usuario duenio = usuario(1L, "duenio");
        duenio.setColeccionPublica(false);
        when(usuarioService.getByNombreUsuario("duenio")).thenReturn(duenio);

        ColeccionProductosDTO coleccion =
            service.listar("duenio", null, null, -3, 0);

        assertThat(coleccion.isVisible()).isFalse();
        assertThat(coleccion.isEsDuenio()).isFalse();
        assertThat(coleccion.getSugerenciaPendiente()).isNull();
        assertThat(coleccion.getArtistas()).isEmpty();
        assertThat(coleccion.getProductos().getContenido()).isEmpty();
        assertThat(coleccion.getProductos().getPagina()).isZero();
        assertThat(coleccion.getProductos().getTamanio()).isEqualTo(1);
        assertThat(coleccion.getProductos().getTotalElementos()).isZero();
    }

    @Test
    void marcarSugerenciaVistaSoloGuardaLaPrimeraVez() {
        Usuario nuevo = usuario(1L, "nuevo");
        Usuario yaVisto = usuario(2L, "visto");
        yaVisto.setColeccionSugeridaEn(LocalDateTime.of(2026, 11, 1, 10, 0));
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(nuevo));
        when(usuarioRepository.findByIdUsuario(2L))
            .thenReturn(Optional.of(yaVisto));

        service.marcarSugerenciaVista(1L);
        service.marcarSugerenciaVista(2L);

        assertThat(nuevo.getColeccionSugeridaEn()).isNotNull();
        verify(usuarioRepository).save(nuevo);
        verify(usuarioRepository, never()).save(yaVisto);
    }

    @Test
    void marcarSugerenciaVistaConUsuarioInexistenteFalla() {
        when(usuarioRepository.findByIdUsuario(9L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarSugerenciaVista(9L))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void alternarOcultoActualizaLaVisibilidadDelItemPropio() {
        CompraItem item = itemCompleto(100L, 10L, 500L);
        when(compraItemRepository.findById(100L))
            .thenReturn(Optional.of(item));

        ColeccionVisibilidadDTO oculto =
            service.alternarOculto(100L, 1L, true);

        assertThat(oculto.getIdCompraItem()).isEqualTo(100L);
        assertThat(oculto.isOculto()).isTrue();
        assertThat(item.isOcultoColeccion()).isTrue();
        assertThat(item.getOcultoColeccionEn()).isNotNull();

        ColeccionVisibilidadDTO visible =
            service.alternarOculto(100L, 1L, false);

        assertThat(visible.isOculto()).isFalse();
        assertThat(item.isOcultoColeccion()).isFalse();
        assertThat(item.getOcultoColeccionEn()).isNull();
        verify(compraItemRepository, org.mockito.Mockito.times(2))
            .save(item);
    }

    @Test
    void alternarOcultoValidaQueLaCompraSeaDelUsuario() {
        CompraItem ajeno = itemCompleto(100L, 10L, 500L);
        ajeno.getGrupo().getCompra().getCliente().setIdUsuario(2L);
        CompraItem sinGrupo = new CompraItem();
        when(compraItemRepository.findById(100L))
            .thenReturn(Optional.of(ajeno));
        when(compraItemRepository.findById(101L))
            .thenReturn(Optional.of(sinGrupo));

        assertThatThrownBy(() -> service.alternarOculto(100L, 1L, true))
            .isInstanceOf(AccesoDenegadoException.class)
            .hasMessageContaining("no es tuya");
        assertThatThrownBy(() -> service.alternarOculto(101L, 1L, true))
            .isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> service.alternarOculto(999L, 1L, true))
            .isInstanceOf(NoSuchElementException.class);
    }

    private static Usuario usuario(Long id, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombreUsuario(nombre);
        usuario.setNombreDisplay(nombre);
        return usuario;
    }

    private static CompraItem itemCompleto(
            Long idItem, Long idProducto, Long idVariante) {
        Producto producto = new Producto();
        producto.setIdProducto(idProducto);
        producto.setTitulo("Pieza");
        producto.setEstado(EstadoProducto.DISPONIBLE);
        producto.setContenidoSensible(true);
        producto.setContenidosSensibles(Set.of(ContenidoSensible.DESNUDOS));
        producto.setArtista(usuario(5L, "artista"));

        ProductoVariante variante = new ProductoVariante();
        variante.setIdVariante(idVariante);

        Compra compra = new Compra();
        compra.setIdCompra(70L);
        compra.setCliente(usuario(1L, "duenio"));
        CompraGrupo grupo = new CompraGrupo();
        grupo.setCompra(compra);
        grupo.setFechaPago(LocalDateTime.of(2026, 11, 1, 9, 0));

        CompraItem item = new CompraItem();
        item.setIdCompraItem(idItem);
        item.setProducto(producto);
        item.setVariante(variante);
        item.setGrupo(grupo);
        item.setTituloProducto("Pieza");
        item.setNombreVariante("Edición");
        item.setOrigen(OrigenCompraItem.REVENTA);
        item.setFechaEntrega(LocalDateTime.of(2026, 11, 5, 9, 0));
        item.setOcultoColeccion(true);
        return item;
    }

    @SafeVarargs
    private static PageImpl<CompraItem> paginaDe(CompraItem... items) {
        return new PageImpl<>(
            List.of(items), PageRequest.of(0, 10), items.length);
    }
}
