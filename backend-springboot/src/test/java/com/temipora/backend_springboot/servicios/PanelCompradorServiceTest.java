package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.Compra;
import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.CompraResumenDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionResenia;
import com.temipora.backend_springboot.modelo.ContadorMensajesDTO;
import com.temipora.backend_springboot.modelo.ContadoresComprasDTO;
import com.temipora.backend_springboot.modelo.DeudaClienteDTO;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.MiEstadoCuentaDTO;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.PanelCompradorResumenDTO;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.SolicitudResumenDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CompraItemRepository;
import com.temipora.backend_springboot.repositorios.CompraRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;

@ExtendWith(MockitoExtension.class)
class PanelCompradorServiceTest {

    @Mock private PedidoService pedidoService;
    @Mock private CompraService compraService;
    @Mock private SolicitudServicioService solicitudServicioService;
    @Mock private MoraClienteService moraClienteService;
    @Mock private ConversacionService conversacionService;
    @Mock private ConfiguracionReseniaService configuracionReseniaService;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private CompraItemRepository compraItemRepository;
    @Mock private CompraRepository compraRepository;

    private PanelCompradorService service;

    @BeforeEach
    void setUp() {
        service = new PanelCompradorService(
            pedidoService, compraService, solicitudServicioService,
            moraClienteService, conversacionService,
            configuracionReseniaService, pedidoRepository,
            compraItemRepository, compraRepository);
    }

    @Test
    void resumenReuneMetricasDineroYListasDelComprador() {
        actividad(
            new BigDecimal("150.00"),
            2, 1, 3, 4, 5);
        when(compraService.contadores("cliente")).thenReturn(
            ContadoresComprasDTO.builder().pendientes(2).build());
        when(compraService.listar("cliente", 0, 5, "pendientes"))
            .thenReturn(paginaDeCompras());
        when(compraRepository.sumarPendienteGrupos(
            1L, EstadoCompraGrupo.PENDIENTE))
            .thenReturn(new BigDecimal("40.00"));

        LocalDateTime ahora = LocalDateTime.now();
        SolicitudResumenDTO porVencer = solicitud(ahora.plusHours(10));
        SolicitudResumenDTO lejano = solicitud(ahora.plusHours(100));
        SolicitudResumenDTO sinVencimiento = solicitud(null);
        when(solicitudServicioService.listarEnviadas(
            1L, com.temipora.backend_springboot.modelo.EstadoSolicitud
                .PRESUPUESTADA))
            .thenReturn(List.of(lejano, sinVencimiento, porVencer));

        ConfiguracionResenia config = new ConfiguracionResenia();
        config.setDiasVentanaCreacion(30);
        when(configuracionReseniaService.obtener()).thenReturn(config);
        when(pedidoRepository.findFinalizadosSinReseniaCliente(
            eq(1L), anyList(), any()))
            .thenReturn(List.of(pedido(11L, ahora.minusDays(2))));
        CompraItem item = item(77L, "Lámina", ahora.minusDays(3), 55L);
        CompraItem repetido = item(77L, "Lámina", ahora.minusDays(3), 55L);
        CompraItem sinProducto = item(null, "Suelto", ahora.minusDays(3), 55L);
        CompraItem sinGrupo = item(78L, "Pieza", null, null);
        when(compraItemRepository.findPagadosSinReseniaProducto(
            eq(1L), eq(EstadoCompraGrupo.PAGADO), any()))
            .thenReturn(List.of(item, repetido, sinProducto, sinGrupo));
        when(compraService.imagenDeItem(any())).thenReturn("img.png");
        when(moraClienteService.estadoCuenta(1L)).thenReturn(
            MiEstadoCuentaDTO.builder()
                .sancionado(false)
                .deudas(List.of())
                .build());
        when(conversacionService.contador(1L)).thenReturn(
            ContadorMensajesDTO.builder().noLeidos(3).build());

        PanelCompradorResumenDTO resumen = service.resumen(1L, "cliente");

        assertThat(resumen.getMetricas().getPedidosEnCurso()).isEqualTo(2);
        assertThat(resumen.getMetricas().getPedidosPendientesPago())
            .isEqualTo(1);
        assertThat(resumen.getMetricas().getEntregasPorAprobar()).isEqualTo(3);
        assertThat(resumen.getMetricas().getPagosPendientes()).isEqualTo(4);
        assertThat(resumen.getMetricas().getPagosVencidos()).isEqualTo(5);
        assertThat(resumen.getMetricas().getComprasPendientes()).isEqualTo(2);
        assertThat(resumen.getMetricas().getPresupuestosRecibidos())
            .isEqualTo(3);
        assertThat(resumen.getMetricas().getPresupuestosPorVencer())
            .isEqualTo(1);
        assertThat(resumen.getMetricas().getReseniasPendientes())
            .isEqualTo(3);

        assertThat(resumen.getDinero().getPorPagarServicios())
            .isEqualByComparingTo("150.00");
        assertThat(resumen.getDinero().getPorPagarCompras())
            .isEqualByComparingTo("40.00");
        assertThat(resumen.getDinero().getPorPagarTotal())
            .isEqualByComparingTo("190.00");
        assertThat(resumen.getDinero().getMoneda()).isEqualTo("ARS");
        assertThat(resumen.getMensajesNoLeidos()).isEqualTo(3);
        assertThat(resumen.getEstadoCuenta()).isNull();
        assertThat(resumen.getComprasPendientes()).hasSize(1);

        assertThat(resumen.getPresupuestosRecibidos()).hasSize(3);
        assertThat(resumen.getPresupuestosRecibidos().get(0)
            .getFechaVencimiento()).isEqualTo(porVencer.getFechaVencimiento());
        assertThat(resumen.getPresupuestosRecibidos().get(2)
            .getFechaVencimiento()).isNull();

        assertThat(resumen.getReseniasPorDejar()).hasSize(3);
        assertThat(resumen.getReseniasPorDejar().get(0).getTipo())
            .isEqualTo("PRODUCTO");
        assertThat(resumen.getReseniasPorDejar().get(0).getImagenUrl())
            .isEqualTo("img.png");
        assertThat(resumen.getReseniasPorDejar().get(1).getTipo())
            .isEqualTo("PEDIDO");
        assertThat(resumen.getReseniasPorDejar().get(2).getFechaLimite())
            .isNull();
    }

    @Test
    void resumenConDatosVaciosUsaCerosYListasVacias() {
        cuandoActividadVacia();
        when(compraRepository.sumarPendienteGrupos(
            1L, EstadoCompraGrupo.PENDIENTE)).thenReturn(null);
        when(configuracionReseniaService.obtener()).thenReturn(null);
        when(moraClienteService.estadoCuenta(1L)).thenReturn(null);
        when(conversacionService.contador(1L))
            .thenReturn(ContadorMensajesDTO.builder().build());

        PanelCompradorResumenDTO resumen = service.resumen(1L, "cliente");

        assertThat(resumen.getDinero().getPorPagarServicios())
            .isEqualByComparingTo("0.00");
        assertThat(resumen.getDinero().getPorPagarCompras())
            .isEqualByComparingTo("0.00");
        assertThat(resumen.getDinero().getPorPagarTotal())
            .isEqualByComparingTo("0.00");
        assertThat(resumen.getMetricas().getReseniasPendientes()).isZero();
        assertThat(resumen.getReseniasPorDejar()).isEmpty();
        assertThat(resumen.getPresupuestosRecibidos()).isEmpty();
        assertThat(resumen.getEstadoCuenta()).isNull();
        assertThat(resumen.getMensajesNoLeidos()).isZero();
        assertThat(resumen.getComprasPendientes()).isEmpty();
    }

    @Test
    void resumenMuestraElEstadoDeCuentaSiHayDeudasOSancion() {
        cuandoActividadVacia();
        MiEstadoCuentaDTO conDeuda = MiEstadoCuentaDTO.builder()
            .sancionado(false)
            .deudas(List.of(DeudaClienteDTO.builder().idPedido(9L).build()))
            .totalAdeudado(new BigDecimal("25.00"))
            .build();
        when(moraClienteService.estadoCuenta(1L)).thenReturn(conDeuda);
        when(conversacionService.contador(1L))
            .thenReturn(ContadorMensajesDTO.builder().noLeidos(1).build());
        when(configuracionReseniaService.obtener()).thenReturn(null);

        PanelCompradorResumenDTO resumen = service.resumen(1L, "cliente");

        assertThat(resumen.getEstadoCuenta()).isSameAs(conDeuda);
        assertThat(resumen.getMensajesNoLeidos()).isEqualTo(1);
    }

    private void cuandoActividadVacia() {
        actividad(null, 0, 0, 0, 0, 0);
        when(compraService.contadores("cliente"))
            .thenReturn(ContadoresComprasDTO.builder().build());
        when(compraService.listar("cliente", 0, 5, "pendientes"))
            .thenReturn(PaginaDTO.<CompraResumenDTO>builder()
                .contenido(List.of())
                .build());
        when(solicitudServicioService.listarEnviadas(
            1L, com.temipora.backend_springboot.modelo.EstadoSolicitud
                .PRESUPUESTADA))
            .thenReturn(List.of());
    }

    private void actividad(
            BigDecimal porPagar,
            long enCurso, long pendientesPago, long entregas,
            long pagosPendientes, long pagosVencidos) {
        PedidoService.ActividadComprador actividad =
            new PedidoService.ActividadComprador(
                List.of(), List.of(), enCurso, pendientesPago, entregas,
                pagosPendientes, pagosVencidos, porPagar, "ARS");
        lenient().when(pedidoService.actividadComprador(1L))
            .thenReturn(actividad);
    }

    private static PaginaDTO<CompraResumenDTO> paginaDeCompras() {
        return PaginaDTO.<CompraResumenDTO>builder()
            .contenido(List.of(CompraResumenDTO.builder()
                .idCompra(55L)
                .build()))
            .build();
    }

    private static SolicitudResumenDTO solicitud(LocalDateTime vencimiento) {
        return SolicitudResumenDTO.builder()
            .fechaVencimiento(vencimiento)
            .build();
    }

    private static Pedido pedido(Long id, LocalDateTime finalizadoEn) {
        Servicio servicio = new Servicio();
        servicio.setNombre("Retrato");
        Usuario artista = new Usuario();
        artista.setNombreUsuario("artista");
        Pedido pedido = new Pedido();
        pedido.setIdPedido(id);
        pedido.setServicio(servicio);
        pedido.setArtista(artista);
        pedido.setFinalizadoEn(finalizadoEn);
        return pedido;
    }

    private static CompraItem item(
            Long idProducto, String titulo, LocalDateTime fechaPago,
            Long idCompra) {
        CompraItem item = new CompraItem();
        item.setTituloProducto(titulo);
        if (idProducto != null) {
            Producto producto = new Producto();
            producto.setIdProducto(idProducto);
            Usuario artista = new Usuario();
            artista.setNombreUsuario("vendedor");
            producto.setArtista(artista);
            item.setProducto(producto);
        }
        if (fechaPago != null) {
            Compra compra = new Compra();
            compra.setIdCompra(idCompra);
            CompraGrupo grupo = new CompraGrupo();
            grupo.setFechaPago(fechaPago);
            grupo.setCompra(compra);
            item.setGrupo(grupo);
        }
        return item;
    }
}
