package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.AmbitoResenia;
import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.ConfiguracionResenia;
import com.temipora.backend_springboot.modelo.CriterioResenia;
import com.temipora.backend_springboot.modelo.EscalaCriterio;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoResenia;
import com.temipora.backend_springboot.modelo.EtiquetaReputacion;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ReputacionClienteDTO;
import com.temipora.backend_springboot.modelo.Resenia;
import com.temipora.backend_springboot.modelo.ReseniaDTO;
import com.temipora.backend_springboot.modelo.ReseniaEstadoDTO;
import com.temipora.backend_springboot.modelo.ReseniaRequest;
import com.temipora.backend_springboot.modelo.ReseniaValorRequest;
import com.temipora.backend_springboot.modelo.ReseniaVersion;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CompraItemRepository;
import com.temipora.backend_springboot.repositorios.CriterioReseniaRepository;
import com.temipora.backend_springboot.repositorios.OpcionCriterioRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.ReseniaRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ReseniaServiceTest {

    @Mock
    private ReseniaRepository reseniaRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CompraItemRepository compraItemRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CriterioReseniaRepository criterioRepository;

    @Mock
    private OpcionCriterioRepository opcionRepository;

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private ConfiguracionReseniaService configuracionService;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private ReseniaService reseniaService;

    // -----------------------------------------------------------------
    // crear
    // -----------------------------------------------------------------

    @Test
    void crear_clienteEnPedidoFinalizado_creaVersionUnoYNotificaAlArtista() {
        Pedido pedido = pedido(EstadoPedido.FINALIZADO, LocalDateTime.now().minusDays(1));
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));
        when(reseniaRepository.existsByPedidoIdPedidoAndAutorIdUsuario(100L, 10L))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(10L))
            .thenReturn(Optional.of(pedido.getCliente()));
        when(criterioRepository
            .findByActivoTrueAndAmbitoOrderByOrdenAscNombreAsc(AmbitoResenia.SERVICIO))
            .thenReturn(List.of(criterio(1L, AmbitoResenia.SERVICIO,
                EscalaCriterio.ESTRELLAS, true)));
        when(reseniaRepository.save(any(Resenia.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ReseniaDTO dto = reseniaService.crear(100L, 10L,
            request(5, List.of(valor(1L, 4)), List.of()));

        assertThat(dto.getDireccion()).isEqualTo(AmbitoResenia.SERVICIO);
        assertThat(dto.getNumeroVersion()).isEqualTo(1);
        assertThat(dto.getValores()).hasSize(1);
        assertThat(dto.getValores().get(0).getValor()).isEqualTo(4);
        assertThat(dto.isEditada()).isFalse();

        verify(notificacionService).crearParaUsuario(
            eq(20L), eq("RESENIA_RECIBIDA"), anyString(), anyString(),
            eq(100L), eq("PEDIDO"));
    }

    @Test
    void crear_sinDescripcion_permitePorqueLaCalificacionAlcanza() {
        Pedido pedido = pedido(EstadoPedido.FINALIZADO, LocalDateTime.now().minusDays(1));
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));
        when(reseniaRepository.existsByPedidoIdPedidoAndAutorIdUsuario(100L, 10L))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(10L))
            .thenReturn(Optional.of(pedido.getCliente()));
        when(criterioRepository
            .findByActivoTrueAndAmbitoOrderByOrdenAscNombreAsc(AmbitoResenia.SERVICIO))
            .thenReturn(List.of(criterio(1L, AmbitoResenia.SERVICIO,
                EscalaCriterio.ESTRELLAS, true)));
        when(reseniaRepository.save(any(Resenia.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ReseniaRequest request = request(4, List.of(valor(1L, 4)), List.of());
        request.setDescripcion(null);

        ReseniaDTO dto = reseniaService.crear(100L, 10L, request);

        assertThat(dto.getDescripcion()).isNull();
        assertThat(dto.getCalificacionGeneral()).isEqualTo(4);
    }

    @Test
    void crear_artistaUsaCriteriosDelClienteYNoNotifica() {
        Pedido pedido = pedido(EstadoPedido.FINALIZADO, LocalDateTime.now().minusDays(1));
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));
        when(reseniaRepository.existsByPedidoIdPedidoAndAutorIdUsuario(100L, 20L))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(20L))
            .thenReturn(Optional.of(pedido.getArtista()));
        when(criterioRepository
            .findByActivoTrueAndAmbitoOrderByOrdenAscNombreAsc(AmbitoResenia.CLIENTE))
            .thenReturn(List.of(criterio(2L, AmbitoResenia.CLIENTE,
                EscalaCriterio.ESTRELLAS, true)));
        when(reseniaRepository.save(any(Resenia.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ReseniaDTO dto = reseniaService.crear(100L, 20L,
            request(4, List.of(valor(2L, 5)), List.of()));

        assertThat(dto.getDireccion()).isEqualTo(AmbitoResenia.CLIENTE);
        verify(notificacionService, never()).crearParaUsuario(
            any(), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void crear_pedidoNoFinalizado_lanzaError() {
        Pedido pedido = pedido(EstadoPedido.EN_CURSO, null);
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> reseniaService.crear(100L, 10L,
                request(5, List.of(), List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("finalizado");
    }

    @Test
    void crear_fueraDeLaVentana_lanzaError() {
        Pedido pedido = pedido(EstadoPedido.FINALIZADO, LocalDateTime.now().minusDays(40));
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));

        assertThatThrownBy(() -> reseniaService.crear(100L, 10L,
                request(5, List.of(), List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("venció");
    }

    @Test
    void crear_reseniaDuplicada_lanzaError() {
        Pedido pedido = pedido(EstadoPedido.FINALIZADO, LocalDateTime.now().minusDays(1));
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));
        when(reseniaRepository.existsByPedidoIdPedidoAndAutorIdUsuario(100L, 10L))
            .thenReturn(true);

        assertThatThrownBy(() -> reseniaService.crear(100L, 10L,
                request(5, List.of(), List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ya dejaste una reseña");
    }

    @Test
    void crear_sinCriterioObligatorio_lanzaError() {
        Pedido pedido = pedido(EstadoPedido.FINALIZADO, LocalDateTime.now().minusDays(1));
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));
        when(reseniaRepository.existsByPedidoIdPedidoAndAutorIdUsuario(100L, 10L))
            .thenReturn(false);
        when(criterioRepository
            .findByActivoTrueAndAmbitoOrderByOrdenAscNombreAsc(AmbitoResenia.SERVICIO))
            .thenReturn(List.of(criterio(1L, AmbitoResenia.SERVICIO,
                EscalaCriterio.ESTRELLAS, true)));

        assertThatThrownBy(() -> reseniaService.crear(100L, 10L,
                request(5, List.of(), List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Completá el criterio");
    }

    @Test
    void crear_criterioDeOtroAmbito_lanzaError() {
        Pedido pedido = pedido(EstadoPedido.FINALIZADO, LocalDateTime.now().minusDays(1));
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));
        when(reseniaRepository.existsByPedidoIdPedidoAndAutorIdUsuario(100L, 10L))
            .thenReturn(false);
        when(criterioRepository
            .findByActivoTrueAndAmbitoOrderByOrdenAscNombreAsc(AmbitoResenia.SERVICIO))
            .thenReturn(List.of());

        assertThatThrownBy(() -> reseniaService.crear(100L, 10L,
                request(5, List.of(valor(99L, 5)), List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no pertenece");
    }

    // -----------------------------------------------------------------
    // editar
    // -----------------------------------------------------------------

    @Test
    void editar_dentroDeLaVentana_agregaVersionDosYConservaLaOriginal() {
        Pedido pedido = pedido(EstadoPedido.FINALIZADO, LocalDateTime.now().minusDays(2));
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));

        Resenia existente = reseniaExistente(pedido, 1);
        existente.setFechaCreacion(LocalDateTime.now().minusDays(1));
        when(reseniaRepository.findForUpdateByPedidoAndAutor(100L, 10L))
            .thenReturn(Optional.of(existente));
        when(criterioRepository
            .findByActivoTrueAndAmbitoOrderByOrdenAscNombreAsc(AmbitoResenia.SERVICIO))
            .thenReturn(List.of(criterio(1L, AmbitoResenia.SERVICIO,
                EscalaCriterio.ESTRELLAS, true)));
        when(reseniaRepository.save(any(Resenia.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ReseniaDTO dto = reseniaService.editar(100L, 10L,
            request(3, List.of(valor(1L, 3)), List.of()));

        assertThat(dto.isEditada()).isTrue();
        assertThat(dto.getEdiciones()).isEqualTo(1);
        assertThat(dto.getCalificacionGeneral()).isEqualTo(3);
        assertThat(dto.getHistorial()).hasSize(2);
        assertThat(dto.getHistorial().get(0).getDescripcion()).isEqualTo("Descripción 1");
        assertThat(dto.getHistorial().get(0).getCalificacionGeneral()).isEqualTo(5);
        assertThat(existente.getVersiones().get(0).getDescripcion()).isEqualTo("Descripción 1");
    }

    @Test
    void editar_porSegundaVez_lanzaError() {
        Pedido pedido = pedido(EstadoPedido.FINALIZADO, LocalDateTime.now().minusDays(2));
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));

        Resenia existente = reseniaExistente(pedido, 2);
        when(reseniaRepository.findForUpdateByPedidoAndAutor(100L, 10L))
            .thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> reseniaService.editar(100L, 10L,
                request(3, List.of(), List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("única edición");
    }

    @Test
    void editar_fueraDeLaVentana_lanzaError() {
        Pedido pedido = pedido(EstadoPedido.FINALIZADO, LocalDateTime.now().minusDays(10));
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));

        Resenia existente = reseniaExistente(pedido, 1);
        existente.setFechaCreacion(LocalDateTime.now().minusDays(5));
        when(reseniaRepository.findForUpdateByPedidoAndAutor(100L, 10L))
            .thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> reseniaService.editar(100L, 10L,
                request(3, List.of(), List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("venció");
    }

    @Test
    void editar_sinResenia_lanzaNoSuchElement() {
        Pedido pedido = pedido(EstadoPedido.FINALIZADO, LocalDateTime.now().minusDays(2));
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));
        when(reseniaRepository.findForUpdateByPedidoAndAutor(100L, 10L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reseniaService.editar(100L, 10L,
                request(3, List.of(), List.of())))
            .isInstanceOf(NoSuchElementException.class);
    }

    // -----------------------------------------------------------------
    // eliminar
    // -----------------------------------------------------------------

    @Test
    void eliminar_marcaBajaLogicaYConservaElHistorial() {
        Pedido pedido = pedido(EstadoPedido.FINALIZADO, LocalDateTime.now().minusDays(2));
        Resenia existente = reseniaExistente(pedido, 1);
        when(reseniaRepository.findForUpdateByPedidoAndAutor(100L, 10L))
            .thenReturn(Optional.of(existente));
        when(reseniaRepository.save(any(Resenia.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        reseniaService.eliminar(100L, 10L);

        assertThat(existente.getEstado()).isEqualTo(EstadoResenia.ELIMINADA);
        assertThat(existente.getEliminadaEn()).isNotNull();
        assertThat(existente.getVersiones()).hasSize(1);
    }

    @Test
    void eliminar_reseniaAjena_oInexistente_lanzaNoSuchElement() {
        when(reseniaRepository.findForUpdateByPedidoAndAutor(100L, 77L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reseniaService.eliminar(100L, 77L))
            .isInstanceOf(NoSuchElementException.class);
    }

    // -----------------------------------------------------------------
    // estado
    // -----------------------------------------------------------------

    @Test
    void estado_clienteNoRecibeLaReseniaDelArtista() {
        Pedido pedido = pedido(EstadoPedido.FINALIZADO, LocalDateTime.now().minusDays(1));
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));
        when(reseniaRepository.findByPedidoIdPedidoAndAutorIdUsuario(100L, 10L))
            .thenReturn(Optional.empty());

        ReseniaEstadoDTO estado = reseniaService.estado(100L, 10L);

        assertThat(estado.getDireccion()).isEqualTo(AmbitoResenia.SERVICIO);
        assertThat(estado.isPuedeCrear()).isTrue();
        assertThat(estado.getReseniaRecibida()).isNull();
        assertThat(estado.getFechaLimiteCreacion()).isNotNull();
    }

    @Test
    void estado_artistaRecibeLaReseniaDelCliente() {
        Pedido pedido = pedido(EstadoPedido.FINALIZADO, LocalDateTime.now().minusDays(1));
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));
        when(reseniaRepository.findByPedidoIdPedidoAndAutorIdUsuario(100L, 20L))
            .thenReturn(Optional.empty());
        when(reseniaRepository.findByPedidoIdPedidoAndAutorIdUsuario(100L, 10L))
            .thenReturn(Optional.of(reseniaExistente(pedido, 1)));

        ReseniaEstadoDTO estado = reseniaService.estado(100L, 20L);

        assertThat(estado.getDireccion()).isEqualTo(AmbitoResenia.CLIENTE);
        assertThat(estado.getReseniaRecibida()).isNotNull();
        assertThat(estado.getReseniaRecibida().getCalificacionGeneral()).isEqualTo(5);
    }

    @Test
    void estado_visitanteAjeno_lanzaAccesoDenegado() {
        Pedido pedido = pedido(EstadoPedido.FINALIZADO, LocalDateTime.now().minusDays(1));
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> reseniaService.estado(100L, 99L))
            .isInstanceOf(AccesoDenegadoException.class);
    }

    // -----------------------------------------------------------------
    // reputación
    // -----------------------------------------------------------------

    @Test
    void reputacion_sinRelacionConElCliente_lanzaAccesoDenegado() {
        when(usuarioRepository.findByIdUsuario(30L))
            .thenReturn(Optional.of(usuario(30L, "cliente30")));
        when(solicitudRepository
            .existsByArtistaIdUsuarioAndClienteIdUsuario(20L, 30L))
            .thenReturn(false);
        when(pedidoRepository
            .existsByArtistaIdUsuarioAndClienteIdUsuario(20L, 30L))
            .thenReturn(false);

        assertThatThrownBy(() -> reseniaService.reputacionCliente(30L, 20L))
            .isInstanceOf(AccesoDenegadoException.class)
            .hasMessageContaining("hayas trabajado");
    }

    @Test
    void reputacion_bajoElMinimo_devuelveSinDatos() {
        when(usuarioRepository.findByIdUsuario(30L))
            .thenReturn(Optional.of(usuario(30L, "cliente30")));
        when(solicitudRepository
            .existsByArtistaIdUsuarioAndClienteIdUsuario(20L, 30L))
            .thenReturn(true);
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));
        when(reseniaRepository.contarReseniasDeCliente(30L, EstadoResenia.PUBLICADA))
            .thenReturn(2L);
        when(reseniaRepository.promedioReseniasDeCliente(30L, EstadoResenia.PUBLICADA))
            .thenReturn(5.0);

        ReputacionClienteDTO dto = reseniaService.reputacionCliente(30L, 20L);

        assertThat(dto.getEtiqueta()).isEqualTo(EtiquetaReputacion.SIN_DATOS);
        assertThat(dto.isHayDatos()).isFalse();
        assertThat(dto.getPromedio()).isNull();
        assertThat(dto.getCantidadResenias()).isEqualTo(2);
    }

    @Test
    void reputacion_promedioAlto_devuelveExcelente() {
        when(usuarioRepository.findByIdUsuario(30L))
            .thenReturn(Optional.of(usuario(30L, "cliente30")));
        when(solicitudRepository
            .existsByArtistaIdUsuarioAndClienteIdUsuario(20L, 30L))
            .thenReturn(true);
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));
        when(reseniaRepository.contarReseniasDeCliente(30L, EstadoResenia.PUBLICADA))
            .thenReturn(4L);
        when(reseniaRepository.promedioReseniasDeCliente(30L, EstadoResenia.PUBLICADA))
            .thenReturn(4.75);

        ReputacionClienteDTO dto = reseniaService.reputacionCliente(30L, 20L);

        assertThat(dto.getEtiqueta()).isEqualTo(EtiquetaReputacion.EXCELENTE);
        assertThat(dto.isHayDatos()).isTrue();
        assertThat(dto.getPromedio()).isEqualByComparingTo("4.75");
    }

    @Test
    void reputacion_promedioBajo_devuelveConReservas() {
        when(usuarioRepository.findByIdUsuario(30L))
            .thenReturn(Optional.of(usuario(30L, "cliente30")));
        when(pedidoRepository
            .existsByArtistaIdUsuarioAndClienteIdUsuario(20L, 30L))
            .thenReturn(true);
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));
        when(reseniaRepository.contarReseniasDeCliente(30L, EstadoResenia.PUBLICADA))
            .thenReturn(3L);
        when(reseniaRepository.promedioReseniasDeCliente(30L, EstadoResenia.PUBLICADA))
            .thenReturn(2.5);

        ReputacionClienteDTO dto = reseniaService.reputacionCliente(30L, 20L);

        assertThat(dto.getEtiqueta()).isEqualTo(EtiquetaReputacion.REGULAR);
    }

    // -----------------------------------------------------------------
    // producto
    // -----------------------------------------------------------------

    @Test
    void crearProducto_compraPagada_creaReseniaYNotificaAlArtista() {
        Producto producto = producto(50L, "Obra original");
        CompraItem compra = compra(LocalDateTime.now().minusDays(1));
        when(productoRepository.findByIdProducto(50L))
            .thenReturn(Optional.of(producto));
        when(compraItemRepository
            .findFirstByProductoIdProductoAndGrupoCompraClienteIdUsuarioAndGrupoEstadoOrderByGrupoFechaPagoDesc(
                50L, 10L, EstadoCompraGrupo.PAGADO))
            .thenReturn(Optional.of(compra));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));
        when(reseniaRepository.existsByProductoIdProductoAndAutorIdUsuario(50L, 10L))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(10L))
            .thenReturn(Optional.of(usuario(10L, "cliente10")));
        when(criterioRepository
            .findByActivoTrueAndAmbitoOrderByOrdenAscNombreAsc(AmbitoResenia.PRODUCTO))
            .thenReturn(List.of(criterio(3L, AmbitoResenia.PRODUCTO,
                EscalaCriterio.ESTRELLAS, true)));
        when(reseniaRepository.save(any(Resenia.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ReseniaDTO dto = reseniaService.crearProducto(50L, 10L,
            request(5, List.of(valor(3L, 5)), List.of()));

        assertThat(dto.getDireccion()).isEqualTo(AmbitoResenia.PRODUCTO);
        assertThat(dto.getIdProducto()).isEqualTo(50L);
        assertThat(dto.getNombreProducto()).isEqualTo("Obra original");
        assertThat(dto.getNombreVariante()).isEqualTo("Original");
        assertThat(dto.isCompraVerificada()).isTrue();

        verify(notificacionService).crearParaUsuario(
            eq(20L), eq("RESENIA_PRODUCTO_RECIBIDA"), anyString(), anyString(),
            eq(50L), eq("PRODUCTO"));
    }

    @Test
    void crearProducto_sinCompraPagada_lanzaError() {
        when(productoRepository.findByIdProducto(50L))
            .thenReturn(Optional.of(producto(50L, "Obra original")));
        when(compraItemRepository
            .findFirstByProductoIdProductoAndGrupoCompraClienteIdUsuarioAndGrupoEstadoOrderByGrupoFechaPagoDesc(
                50L, 10L, EstadoCompraGrupo.PAGADO))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reseniaService.crearProducto(50L, 10L,
                request(5, List.of(), List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("compraron");
    }

    @Test
    void crearProducto_fueraDeLaVentana_lanzaError() {
        when(productoRepository.findByIdProducto(50L))
            .thenReturn(Optional.of(producto(50L, "Obra original")));
        when(compraItemRepository
            .findFirstByProductoIdProductoAndGrupoCompraClienteIdUsuarioAndGrupoEstadoOrderByGrupoFechaPagoDesc(
                50L, 10L, EstadoCompraGrupo.PAGADO))
            .thenReturn(Optional.of(compra(LocalDateTime.now().minusDays(40))));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));

        assertThatThrownBy(() -> reseniaService.crearProducto(50L, 10L,
                request(5, List.of(), List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("venció");
    }

    @Test
    void estadoProducto_compradorDentroDeVentana_permiteCrear() {
        when(productoRepository.findByIdProducto(50L))
            .thenReturn(Optional.of(producto(50L, "Obra original")));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));
        when(reseniaRepository
            .findByProductoIdProductoAndAutorIdUsuario(50L, 10L))
            .thenReturn(Optional.empty());
        when(compraItemRepository
            .findFirstByProductoIdProductoAndGrupoCompraClienteIdUsuarioAndGrupoEstadoOrderByGrupoFechaPagoDesc(
                50L, 10L, EstadoCompraGrupo.PAGADO))
            .thenReturn(Optional.of(compra(LocalDateTime.now().minusDays(1))));

        ReseniaEstadoDTO estado = reseniaService.estadoProducto(50L, 10L);

        assertThat(estado.getDireccion()).isEqualTo(AmbitoResenia.PRODUCTO);
        assertThat(estado.isPuedeCrear()).isTrue();
        assertThat(estado.getFechaLimiteCreacion()).isNotNull();
    }

    @Test
    void estadoProducto_sinCompra_noPermiteCrear() {
        when(productoRepository.findByIdProducto(50L))
            .thenReturn(Optional.of(producto(50L, "Obra original")));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));
        when(reseniaRepository
            .findByProductoIdProductoAndAutorIdUsuario(50L, 10L))
            .thenReturn(Optional.empty());
        when(compraItemRepository
            .findFirstByProductoIdProductoAndGrupoCompraClienteIdUsuarioAndGrupoEstadoOrderByGrupoFechaPagoDesc(
                50L, 10L, EstadoCompraGrupo.PAGADO))
            .thenReturn(Optional.empty());

        ReseniaEstadoDTO estado = reseniaService.estadoProducto(50L, 10L);

        assertThat(estado.isPuedeCrear()).isFalse();
        assertThat(estado.getMotivoNoDisponible()).contains("compraron");
    }

    @Test
    void crearProducto_contenidoSensible_permiteMostrarElNombre() {
        Producto producto = producto(50L, "Obra sensible");
        producto.setContenidoSensible(true);
        when(productoRepository.findByIdProducto(50L))
            .thenReturn(Optional.of(producto));
        when(compraItemRepository
            .findFirstByProductoIdProductoAndGrupoCompraClienteIdUsuarioAndGrupoEstadoOrderByGrupoFechaPagoDesc(
                50L, 10L, EstadoCompraGrupo.PAGADO))
            .thenReturn(Optional.of(compra(LocalDateTime.now().minusDays(1))));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));
        when(reseniaRepository.existsByProductoIdProductoAndAutorIdUsuario(50L, 10L))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(10L))
            .thenReturn(Optional.of(usuario(10L, "cliente10")));
        when(criterioRepository
            .findByActivoTrueAndAmbitoOrderByOrdenAscNombreAsc(AmbitoResenia.PRODUCTO))
            .thenReturn(List.of());
        when(reseniaRepository.save(any(Resenia.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ReseniaRequest request = request(5, List.of(), List.of());
        request.setMostrarNombre(true);

        ReseniaDTO dto = reseniaService.crearProducto(50L, 10L, request);

        assertThat(dto.isMostrarNombre()).isTrue();
    }

    @Test
    void crearProducto_conVariasVariantes_guardaElResumenDeVariantes() {
        Producto producto = producto(50L, "Obra original");
        CompraItem compra = compra(LocalDateTime.now().minusDays(1));
        CompraItem otra = compra(LocalDateTime.now().minusDays(2));
        otra.setNombreVariante("Edición limitada");
        when(productoRepository.findByIdProducto(50L))
            .thenReturn(Optional.of(producto));
        when(compraItemRepository
            .findFirstByProductoIdProductoAndGrupoCompraClienteIdUsuarioAndGrupoEstadoOrderByGrupoFechaPagoDesc(
                50L, 10L, EstadoCompraGrupo.PAGADO))
            .thenReturn(Optional.of(compra));
        when(compraItemRepository.findPagadosDeProducto(
            50L, 10L, EstadoCompraGrupo.PAGADO))
            .thenReturn(List.of(compra, otra));
        when(configuracionService.obtener()).thenReturn(config(30, 3, 3));
        when(reseniaRepository.existsByProductoIdProductoAndAutorIdUsuario(50L, 10L))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(10L))
            .thenReturn(Optional.of(usuario(10L, "cliente10")));
        when(criterioRepository
            .findByActivoTrueAndAmbitoOrderByOrdenAscNombreAsc(AmbitoResenia.PRODUCTO))
            .thenReturn(List.of());
        when(reseniaRepository.save(any(Resenia.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ReseniaDTO dto = reseniaService.crearProducto(50L, 10L,
            request(5, List.of(), List.of()));

        assertThat(dto.getCantidadVariantesCompradas()).isEqualTo(2);
        assertThat(dto.getVariantesCompradas())
            .contains("Original")
            .contains("Edición limitada");
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private void stubCrearBase(Pedido pedido, ConfiguracionResenia config) {
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(configuracionService.obtener()).thenReturn(config);
        when(reseniaRepository.existsByPedidoIdPedidoAndAutorIdUsuario(100L, 10L))
            .thenReturn(false);
        when(criterioRepository
            .findByActivoTrueAndAmbitoOrderByOrdenAscNombreAsc(AmbitoResenia.SERVICIO))
            .thenReturn(List.of(criterio(1L, AmbitoResenia.SERVICIO,
                EscalaCriterio.ESTRELLAS, true)));
    }

    private Pedido pedido(EstadoPedido estado, LocalDateTime finalizadoEn) {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(100L);
        pedido.setEstado(estado);
        pedido.setCliente(usuario(10L, "cliente10"));
        pedido.setArtista(usuario(20L, "artista20"));
        pedido.setServicio(servicio("Retrato digital"));
        pedido.setFinalizadoEn(finalizadoEn);
        return pedido;
    }

    private Producto producto(Long id, String titulo) {
        Producto producto = new Producto();
        producto.setIdProducto(id);
        producto.setTitulo(titulo);
        producto.setArtista(usuario(20L, "artista20"));
        return producto;
    }

    private CompraItem compra(LocalDateTime fechaPago) {
        CompraGrupo grupo = new CompraGrupo();
        grupo.setEstado(EstadoCompraGrupo.PAGADO);
        grupo.setFechaPago(fechaPago);
        CompraItem item = new CompraItem();
        item.setGrupo(grupo);
        item.setNombreVariante("Original");
        return item;
    }

    private Servicio servicio(String nombre) {
        Servicio servicio = new Servicio();
        servicio.setNombre(nombre);
        return servicio;
    }

    private Usuario usuario(Long id, String nombreUsuario) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombreUsuario(nombreUsuario);
        return usuario;
    }

    private Resenia reseniaExistente(Pedido pedido, int versiones) {
        Resenia resenia = new Resenia();
        resenia.setIdResenia(7L);
        resenia.setPedido(pedido);
        resenia.setAutor(pedido.getCliente());
        resenia.setEstado(EstadoResenia.PUBLICADA);
        resenia.setFechaCreacion(LocalDateTime.now().minusDays(1));
        for (int numero = 1; numero <= versiones; numero++) {
            ReseniaVersion version = new ReseniaVersion();
            version.setNumero(numero);
            version.setDescripcion("Descripción " + numero);
            version.setCalificacionGeneral(numero == 1 ? 5 : 3);
            version.setFechaVersion(LocalDateTime.now());
            resenia.agregarVersion(version);
        }
        return resenia;
    }

    private CriterioResenia criterio(
            Long id, AmbitoResenia ambito, EscalaCriterio escala, boolean obligatorio) {
        CriterioResenia criterio = new CriterioResenia();
        criterio.setIdCriterioResenia(id);
        criterio.setNombre("Criterio " + id);
        criterio.setAmbito(ambito);
        criterio.setEscala(escala);
        criterio.setObligatorio(obligatorio);
        criterio.setActivo(true);
        return criterio;
    }

    private ConfiguracionResenia config(
            int ventana, int edicion, int minimo) {
        ConfiguracionResenia config = new ConfiguracionResenia();
        config.setIdConfiguracion(ConfiguracionResenia.ID_UNICO);
        config.setDiasVentanaCreacion(ventana);
        config.setDiasEdicion(edicion);
        config.setMinimoReseniasEtiqueta(minimo);
        config.setUmbralExcelente(new BigDecimal("4.50"));
        config.setUmbralBuena(new BigDecimal("3.50"));
        return config;
    }

    private ReseniaRequest request(
            Integer calificacion, List<ReseniaValorRequest> valores, List<Long> opciones) {
        ReseniaRequest request = new ReseniaRequest();
        request.setDescripcion("La entrega llegó en fecha y con buena calidad.");
        request.setCalificacionGeneral(calificacion);
        request.setValores(valores);
        request.setOpciones(opciones);
        return request;
    }

    private ReseniaValorRequest valor(Long idCriterio, Integer valor) {
        ReseniaValorRequest item = new ReseniaValorRequest();
        item.setIdCriterio(idCriterio);
        item.setValor(valor);
        return item;
    }
}
