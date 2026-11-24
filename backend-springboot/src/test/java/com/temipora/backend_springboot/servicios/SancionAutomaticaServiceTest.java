package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoPublicacionReventa;
import com.temipora.backend_springboot.modelo.EstadoSancion;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.HitoPresupuesto;
import com.temipora.backend_springboot.modelo.OrigenSancion;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.PublicacionReventa;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.PublicacionReventaRepository;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;

@ExtendWith(MockitoExtension.class)
class SancionAutomaticaServiceTest {

    @Mock
    private SancionService sancionService;

    @Mock
    private SancionCuentaRepository sancionRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private PublicacionReventaRepository reventaRepository;

    @Mock
    private NotificacionService notificacionService;

    @Spy
    private EntregaPendienteService entregaPendienteService =
        new EntregaPendienteService();

    @InjectMocks
    private SancionAutomaticaService service;

    @Test
    void aplicarNivel1_pausaElCatalogoYGuardaElAlcanceAfectado() {
        Pedido pedido = pedido();
        Servicio servicio = servicio(1L, EstadoServicio.PUBLICADO);
        Producto producto = producto(2L, EstadoProducto.DISPONIBLE);
        PublicacionReventa reventa = reventa(3L, EstadoPublicacionReventa.PUBLICADA);
        SancionCuenta sancion = sancionAutomatica();

        when(sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
                eq(7L), eq(EstadoSancion.ACTIVA)))
            .thenReturn(Optional.empty());
        when(servicioRepository.findByArtistaIdUsuarioAndEstado(
                7L, EstadoServicio.PUBLICADO)).thenReturn(List.of(servicio));
        when(productoRepository.findByArtistaIdUsuarioAndEstadoOrderByTituloAsc(
                7L, EstadoProducto.DISPONIBLE)).thenReturn(List.of(producto));
        when(reventaRepository.findByVendedorIdUsuarioAndEstado(
                7L, EstadoPublicacionReventa.PUBLICADA)).thenReturn(List.of(reventa));
        when(sancionService.aplicarSistema(
                anyLong(), any(), any(), anyString(), any(), anyString(),
                any(), any(), any()))
            .thenReturn(sancion);
        when(sancionRepository.save(any(SancionCuenta.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SancionCuenta resultado = service.aplicarNivel1(pedido, 14);

        assertThat(resultado.getAlcanceAfectado())
            .isEqualTo("servicios=1;productos=2;reventas=3;");
        assertThat(servicio.getEstado()).isEqualTo(EstadoServicio.PAUSADO);
        assertThat(producto.getEstado()).isEqualTo(EstadoProducto.PAUSADO);
        assertThat(reventa.getEstado())
            .isEqualTo(EstadoPublicacionReventa.SUSPENDIDA);
        verify(notificacionService).crearParaUsuario(
            eq(7L), eq("CUENTA_SUSPENDIDA_VENTAS"), any(), any(),
            eq(10L), eq("PEDIDO"));
    }

    @Test
    void aplicarNivel1_conSancionActiva_noTocaElCatalogo() {
        Pedido pedido = pedido();
        SancionCuenta existente = sancionAutomatica();

        when(sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
                eq(7L), eq(EstadoSancion.ACTIVA)))
            .thenReturn(Optional.of(existente));

        SancionCuenta resultado = service.aplicarNivel1(pedido, 14);

        assertThat(resultado).isSameAs(existente);
        verifyNoInteractions(servicioRepository, productoRepository, reventaRepository);
        verify(sancionService, never()).aplicarSistema(
            anyLong(), any(), any(), anyString(), any(), anyString(),
            any(), any(), any());
    }

    @Test
    void escalarNivel2_escalaYNotifica() {
        Pedido pedido = pedido();
        SancionCuenta escalada = sancionAutomatica();

        when(sancionService.escalarSistema(
                eq(99L), any(), anyString(), anyString(), any(), any(), any()))
            .thenReturn(escalada);

        SancionCuenta resultado = service.escalarNivel2(sancionAutomatica(), pedido);

        assertThat(resultado).isSameAs(escalada);
        verify(sancionService).escalarSistema(
            eq(99L),
            eq(AlcanceSancion.ENTREGAS),
            anyString(),
            eq("AUTO:ENTREGA:10:NIVEL2"),
            eq(pedido),
            any(),
            eq(pedido.getMoraDesde()));
        verify(notificacionService).crearParaUsuario(
            eq(7L), eq("CUENTA_RESTRINGIDA_ENTREGAS"), any(), any(),
            eq(10L), eq("PEDIDO"));
    }

    @Test
    void curar_revierteYRestauraSoloLoQuePausoLaSancion() {
        SancionCuenta sancion = sancionAutomatica();
        sancion.setAlcanceAfectado("servicios=1;productos=2;reventas=3;");
        Servicio servicio = servicio(1L, EstadoServicio.PAUSADO);
        Producto producto = producto(2L, EstadoProducto.PAUSADO);
        PublicacionReventa reventa = reventa(3L, EstadoPublicacionReventa.SUSPENDIDA);

        when(sancionService.revertirSistema(eq(99L), anyString()))
            .thenReturn(sancion);
        when(servicioRepository.findByIdServicio(1L))
            .thenReturn(Optional.of(servicio));
        when(productoRepository.findById(2L))
            .thenReturn(Optional.of(producto));
        when(reventaRepository.findById(3L))
            .thenReturn(Optional.of(reventa));
        when(sancionRepository.save(any(SancionCuenta.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        service.curar(sancion);

        assertThat(servicio.getEstado()).isEqualTo(EstadoServicio.PUBLICADO);
        assertThat(producto.getEstado()).isEqualTo(EstadoProducto.DISPONIBLE);
        assertThat(reventa.getEstado())
            .isEqualTo(EstadoPublicacionReventa.PUBLICADA);
        assertThat(sancion.getAlcanceAfectado()).isNull();
    }

    @Test
    void restaurarEfectos_noReactivaLoQueYaEstabaArchivado() {
        SancionCuenta sancion = sancionAutomatica();
        sancion.setAlcanceAfectado("servicios=1;");
        Servicio servicio = servicio(1L, EstadoServicio.ARCHIVADO);
        when(servicioRepository.findByIdServicio(1L))
            .thenReturn(Optional.of(servicio));
        when(sancionRepository.save(any(SancionCuenta.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        service.restaurarEfectos(sancion);

        assertThat(servicio.getEstado()).isEqualTo(EstadoServicio.ARCHIVADO);
        verify(servicioRepository, never()).save(any());
    }

    private SancionCuenta sancionAutomatica() {
        return SancionCuenta.builder()
            .idSancion(99L)
            .usuario(Usuario.builder().idUsuario(7L).build())
            .origen(OrigenSancion.AUTOMATICA)
            .alcance(AlcanceSancion.VENTAS)
            .tipo(com.temipora.backend_springboot.modelo.TipoSancion.SUSPENSION)
            .motivo("motivo")
            .fechaInicio(LocalDateTime.now().minusDays(5))
            .estado(EstadoSancion.ACTIVA)
            .build();
    }

    private Pedido pedido() {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(10L);
        pedido.setEstado(EstadoPedido.EN_CURSO);
        pedido.setArtista(Usuario.builder().idUsuario(7L).build());
        pedido.setMoraDesde(LocalDateTime.now().minusDays(5));

        HitoPresupuesto presupuesto = new HitoPresupuesto();
        presupuesto.setOrden(1);
        presupuesto.setTipo(TipoHito.CUOTA);
        presupuesto.setNombre("Etapa 1");
        presupuesto.setRequiereEntrega(true);

        HitoPedido hito = new HitoPedido();
        hito.setIdHitoPedido(20L);
        hito.setHitoPresupuesto(presupuesto);
        hito.setOrden(1);
        hito.setEstadoPago(EstadoPagoHito.PAGADO);
        hito.setEstadoEntrega(EstadoEntregaHito.NO_INICIADO);
        pedido.agregarHito(hito);
        return pedido;
    }

    private Servicio servicio(Long id, EstadoServicio estado) {
        Servicio servicio = new Servicio();
        servicio.setIdServicio(id);
        servicio.setEstado(estado);
        return servicio;
    }

    private Producto producto(Long id, EstadoProducto estado) {
        Producto producto = new Producto();
        producto.setIdProducto(id);
        producto.setEstado(estado);
        return producto;
    }

    private PublicacionReventa reventa(
            Long id, EstadoPublicacionReventa estado) {
        PublicacionReventa reventa = new PublicacionReventa();
        reventa.setIdPublicacionReventa(id);
        reventa.setEstado(estado);
        return reventa;
    }
}
