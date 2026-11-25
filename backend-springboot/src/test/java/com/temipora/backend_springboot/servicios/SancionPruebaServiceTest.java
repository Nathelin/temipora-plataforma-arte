package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoSancion;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.HitoPresupuesto;
import com.temipora.backend_springboot.modelo.OrigenSancion;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.SaludCuentaDTO;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.TipoHito;
import com.temipora.backend_springboot.modelo.TipoSancion;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SancionPruebaServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private SancionCuentaRepository sancionRepository;

    @Mock
    private MoraService moraService;

    @Mock
    private SancionIncumplimientoService sancionIncumplimientoService;

    @Mock
    private SancionAutomaticaService sancionAutomaticaService;

    @Mock
    private ConfiguracionPedidoService configuracionPedidoService;

    @Spy
    private EntregaPendienteService entregaPendienteService =
        new EntregaPendienteService();

    @Mock
    private SaludCuentaService saludCuentaService;

    @InjectMocks
    private SancionPruebaService service;

    @Test
    void procesar_ejecutaElCicloYDevuelveResumen() {
        Usuario usuario = artista();
        prepararEstado(usuario, null, List.of());
        when(moraService.detectar()).thenReturn(2);
        when(sancionIncumplimientoService.procesar())
            .thenReturn(new SancionIncumplimientoService.Resumen(1, 2, 3, 4));

        SancionPruebaService.ResultadoPrueba resultado =
            service.procesar("ArtistaMoroso");

        assertThat(resultado.ciclo().morasRegistradas()).isEqualTo(2);
        assertThat(resultado.ciclo().sancionesCuradas()).isEqualTo(1);
        assertThat(resultado.ciclo().sancionesEscaladas()).isEqualTo(2);
        assertThat(resultado.ciclo().nivel1Aplicados()).isEqualTo(3);
        assertThat(resultado.ciclo().pedidosCerrados()).isEqualTo(4);
        assertThat(resultado.estado().usuario()).isEqualTo("ArtistaMoroso");
    }

    @Test
    void prepararNivel1_reabreElPedidoYAplicaElCiclo() {
        Usuario usuario = artista();
        Pedido pedido = pedido();
        SancionCuenta sancion = sancionEntregas();
        prepararEstado(usuario, sancion, List.of(pedido));
        when(sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
                usuario.getIdUsuario(), EstadoSancion.ACTIVA))
            .thenReturn(Optional.empty(), Optional.of(sancion));
        when(pedidoRepository.save(any(Pedido.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(moraService.detectar()).thenReturn(1);
        when(sancionIncumplimientoService.procesar())
            .thenReturn(new SancionIncumplimientoService.Resumen(0, 0, 1, 0));

        SancionPruebaService.ResultadoPrueba resultado =
            service.prepararNivel1("ArtistaMoroso");

        HitoPedido hito = pedido.getHitos().get(0);
        assertThat(pedido.getLimiteEntrega()).isBefore(LocalDate.now());
        assertThat(pedido.getMoraDesde()).isNotNull();
        assertThat(hito.getEstadoPago()).isEqualTo(EstadoPagoHito.PAGADO);
        assertThat(hito.getEstadoEntrega())
            .isEqualTo(EstadoEntregaHito.NO_INICIADO);
        assertThat(resultado.mensaje()).contains("Nivel 1 aplicado");
    }

    @Test
    void forzarNivel2_venceLaVentanaYEscala() {
        Usuario usuario = artista();
        Pedido pedido = pedido();
        SancionCuenta sancion = sancionEntregas();
        sancion.setPedido(pedido);
        sancion.setHitoPedido(pedido.getHitos().get(0));
        sancion.setFechaFin(LocalDateTime.now().plusDays(5));
        prepararEstado(usuario, sancion, List.of(pedido));
        when(sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
                usuario.getIdUsuario(), EstadoSancion.ACTIVA))
            .thenReturn(Optional.of(sancion));
        when(sancionRepository.save(any(SancionCuenta.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(sancionIncumplimientoService.procesar()).thenAnswer(inv -> {
            sancion.setAlcance(AlcanceSancion.ACCESO);
            return new SancionIncumplimientoService.Resumen(0, 1, 0, 0);
        });

        SancionPruebaService.ResultadoPrueba resultado =
            service.forzarNivel2("ArtistaMoroso");

        assertThat(sancion.getFechaFin()).isBefore(LocalDateTime.now());
        assertThat(resultado.mensaje()).contains("Nivel 2 aplicado");
        assertThat(resultado.ciclo().sancionesEscaladas()).isEqualTo(1);
    }

    @Test
    void simularAprobacion_apruebaElHitoYReincorpora() {
        Usuario usuario = artista();
        Pedido pedido = pedido();
        SancionCuenta sancion = sancionEntregas();
        sancion.setPedido(pedido);
        sancion.setHitoPedido(pedido.getHitos().get(0));
        prepararEstado(usuario, null, List.of(pedido));
        when(sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
                usuario.getIdUsuario(), EstadoSancion.ACTIVA))
            .thenReturn(Optional.of(sancion), Optional.empty(),
                Optional.empty());
        when(pedidoRepository.save(any(Pedido.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(moraService.detectar()).thenReturn(0);
        when(sancionIncumplimientoService.procesar())
            .thenReturn(new SancionIncumplimientoService.Resumen(1, 0, 0, 0));

        SancionPruebaService.ResultadoPrueba resultado =
            service.simularAprobacion("ArtistaMoroso");

        assertThat(pedido.getHitos().get(0).getEstadoEntrega())
            .isEqualTo(EstadoEntregaHito.APROBADO);
        assertThat(resultado.mensaje()).contains("mora curada");
    }

    @Test
    void revertirTodo_revierteYDejaElPedidoConPlazoFuturo() {
        Usuario usuario = artista();
        Pedido pedido = pedido();
        pedido.setLimiteEntrega(LocalDate.now().minusDays(5));
        pedido.setMoraDesde(LocalDateTime.now().minusDays(5));
        pedido.getHitos().get(0)
            .setEstadoEntrega(EstadoEntregaHito.APROBADO);
        SancionCuenta sancion = sancionEntregas();
        sancion.setPedido(pedido);
        prepararEstado(usuario, null, List.of(pedido));
        when(sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
                usuario.getIdUsuario(), EstadoSancion.ACTIVA))
            .thenReturn(Optional.of(sancion), Optional.empty());
        when(pedidoRepository.save(any(Pedido.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SancionPruebaService.ResultadoPrueba resultado =
            service.revertirTodo("ArtistaMoroso");

        verify(sancionAutomaticaService).revertirYRestaurar(
            eq(sancion.getIdSancion()), anyString());
        assertThat(pedido.getLimiteEntrega()).isAfter(LocalDate.now());
        assertThat(pedido.getMoraDesde()).isNull();
        assertThat(pedido.getHitos().get(0).getEstadoEntrega())
            .isEqualTo(EstadoEntregaHito.NO_INICIADO);
        assertThat(resultado.mensaje()).contains("reabierto");
    }

    private void prepararEstado(
            Usuario usuario, SancionCuenta sancion, List<Pedido> pedidos) {
        when(usuarioRepository.findByNombreUsuarioIgnoreCase(
                anyString())).thenReturn(Optional.of(usuario));
        when(configuracionPedidoService.obtener()).thenReturn(configuracion());
        when(pedidoRepository
            .findByArtistaIdUsuarioAndEstadoInOrderByFechaCreacionDesc(
                anyLong(), any(), any()))
            .thenReturn(pedidos);
        when(sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
                anyLong(), any())).thenReturn(Optional.ofNullable(sancion));
        when(saludCuentaService.salud(anyLong())).thenReturn(
            new SaludCuentaService.Salud(
                SaludCuentaDTO.builder().pausados(List.of()).build(),
                Map.of()));
    }

    private Usuario artista() {
        return Usuario.builder()
            .idUsuario(880L)
            .nombreUsuario("ArtistaMoroso")
            .correo("artistamoroso@gmail.com")
            .rol(Rol.builder().nombre(ERol.ARTISTA).build())
            .build();
    }

    private ConfiguracionPedido configuracion() {
        ConfiguracionPedido config = new ConfiguracionPedido();
        config.setDiasGraciaEntrega(3);
        config.setDiasSancionVentas(14);
        config.setDiasCierreTrasBloqueo(30);
        return config;
    }

    private Pedido pedido() {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(31L);
        pedido.setEstado(EstadoPedido.EN_CURSO);
        pedido.setFechaCreacion(LocalDateTime.now().minusDays(10));
        pedido.setFechaInicio(LocalDate.now().minusDays(10));
        pedido.setLimiteEntrega(LocalDate.now().plusDays(3));
        pedido.setArtista(artista());
        pedido.setCliente(Usuario.builder()
            .idUsuario(7L)
            .nombreUsuario("Cliente2")
            .build());
        Servicio servicio = new Servicio();
        servicio.setIdServicio(22L);
        servicio.setNombre("Retrato angelical");
        pedido.setServicio(servicio);

        HitoPresupuesto presupuesto = new HitoPresupuesto();
        presupuesto.setOrden(0);
        presupuesto.setTipo(TipoHito.SENA);
        presupuesto.setNombre("Seña");
        presupuesto.setRequiereEntrega(true);

        HitoPedido hito = new HitoPedido();
        hito.setIdHitoPedido(53L);
        hito.setHitoPresupuesto(presupuesto);
        hito.setOrden(0);
        hito.setEstadoPago(EstadoPagoHito.PAGADO);
        hito.setEstadoEntrega(EstadoEntregaHito.NO_INICIADO);
        pedido.agregarHito(hito);
        return pedido;
    }

    private SancionCuenta sancionEntregas() {
        return SancionCuenta.builder()
            .idSancion(4L)
            .usuario(artista())
            .tipo(TipoSancion.SUSPENSION)
            .alcance(AlcanceSancion.ENTREGAS)
            .origen(OrigenSancion.AUTOMATICA)
            .estado(EstadoSancion.ACTIVA)
            .motivo("No entregó la seña.")
            .fechaInicio(LocalDateTime.now().minusDays(2))
            .fechaFin(LocalDateTime.now().plusDays(12))
            .build();
    }
}
