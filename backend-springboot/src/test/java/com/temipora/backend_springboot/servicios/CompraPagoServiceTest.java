package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.Compra;
import com.temipora.backend_springboot.modelo.CompraDTO;
import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EstadoCompra;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoIntentoCobro;
import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.EstadoReserva;
import com.temipora.backend_springboot.modelo.IntentoCobro;
import com.temipora.backend_springboot.modelo.IntentoCobroGrupo;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.PagoCompraDTO;
import com.temipora.backend_springboot.modelo.ReservaStock;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CompraGrupoRepository;
import com.temipora.backend_springboot.repositorios.CompraRepository;
import com.temipora.backend_springboot.repositorios.IntentoCobroGrupoRepository;
import com.temipora.backend_springboot.repositorios.IntentoCobroRepository;
import com.temipora.backend_springboot.repositorios.ReservaStockRepository;
import com.temipora.backend_springboot.servicios.pagos.EstadoCobroProveedor;
import com.temipora.backend_springboot.servicios.pagos.ResultadoCobro;
import com.temipora.backend_springboot.servicios.pagos.producto.ProveedorPagoProducto;

@ExtendWith(MockitoExtension.class)
class CompraPagoServiceTest {

    @Mock private CompraRepository compraRepository;
    @Mock private CompraGrupoRepository compraGrupoRepository;
    @Mock private ReservaStockRepository reservaStockRepository;
    @Mock private IntentoCobroRepository intentoCobroRepository;
    @Mock private IntentoCobroGrupoRepository intentoCobroGrupoRepository;
    @Mock private CompraEntregaService compraEntregaService;
    @Mock private CompraService compraService;
    @Mock private UsuarioService usuarioService;
    @Mock private ProveedorPagoProducto proveedorPago;

    private CompraPagoService service;

    @BeforeEach
    void setUp() {
        service = new CompraPagoService(
            compraRepository,
            compraGrupoRepository,
            reservaStockRepository,
            intentoCobroRepository,
            intentoCobroGrupoRepository,
            compraEntregaService,
            compraService,
            usuarioService,
            proveedorPago,
            30);
    }

    @Test
    void iniciarPago_creaIntentoYDevuelveInstruccion() {
        Usuario cliente = usuario(1L, "cliente");
        Compra compra = compra(cliente);
        CompraGrupo grupo = grupo(compra);
        cuandoCompraYReserva(cliente, compra, grupo);
        when(intentoCobroGrupoRepository
            .findFirstByGrupoIdCompraGrupoAndEstadoOrderByIntentoCobroFechaCreacionDesc(
                20L, EstadoIntentoCobro.PENDIENTE))
            .thenReturn(Optional.empty());
        when(proveedorPago.nombre()).thenReturn("SIMULADO");
        when(intentoCobroRepository.save(any(IntentoCobro.class)))
            .thenAnswer(inv -> {
                IntentoCobro intento = inv.getArgument(0);
                if (intento.getIdIntentoCobro() == null) {
                    intento.setIdIntentoCobro(40L);
                }
                return intento;
            });
        when(proveedorPago.crearCobro(any(), anyList()))
            .thenReturn(new ResultadoCobro(
                "pref-1", "instruccion", LocalDateTime.now().plusMinutes(30)));

        PagoCompraDTO pago = service.iniciarPago("cliente", 10L, 20L);

        assertThat(pago.getIdIntentoCobro()).isEqualTo(40L);
        assertThat(pago.getInstruccion()).isEqualTo("instruccion");
        assertThat(pago.getMonto()).isEqualByComparingTo("100.00");
        verify(proveedorPago).crearCobro(any(), eq(List.of(grupo)));
    }

    @Test
    void iniciarPago_reutilizaIntentoPendienteConInstruccion() {
        Usuario cliente = usuario(1L, "cliente");
        Compra compra = compra(cliente);
        CompraGrupo grupo = grupo(compra);
        cuandoCompraYReserva(cliente, compra, grupo);
        IntentoCobro intento = intento(compra, grupo, EstadoIntentoCobro.PENDIENTE);
        intento.setInstruccion("https://checkout");
        IntentoCobroGrupo asignacion = intento.getGrupos().get(0);
        when(intentoCobroGrupoRepository
            .findFirstByGrupoIdCompraGrupoAndEstadoOrderByIntentoCobroFechaCreacionDesc(
                20L, EstadoIntentoCobro.PENDIENTE))
            .thenReturn(Optional.of(asignacion));

        PagoCompraDTO pago = service.iniciarPago("cliente", 10L, 20L);

        assertThat(pago.getInstruccion()).isEqualTo("https://checkout");
        verify(proveedorPago, never()).crearCobro(any(), anyList());
    }

    @Test
    void aplicarEstadoProveedor_rechazado_actualizaEstadosSinEntregar() {
        Compra compra = compra(usuario(1L, "cliente"));
        grupo(compra);
        IntentoCobro intento = intento(
            compra, null, EstadoIntentoCobro.PENDIENTE);
        when(intentoCobroRepository.findByIdIntentoCobroForUpdate(40L))
            .thenReturn(Optional.of(intento));

        service.aplicarEstadoProveedor(40L,
            new EstadoCobroProveedor(
                EstadoPago.RECHAZADO, "pago-1", "rejected", "Rechazado", null));

        assertThat(intento.getEstado()).isEqualTo(EstadoIntentoCobro.RECHAZADO);
        assertThat(intento.getGrupos().get(0).getEstado())
            .isEqualTo(EstadoIntentoCobro.RECHAZADO);
        verify(compraEntregaService, never()).confirmarPago(any());
    }

    @Test
    void aplicarEstadoProveedor_aprobado_confirmaLaEntrega() {
        Compra compra = compra(usuario(1L, "cliente"));
        grupo(compra);
        IntentoCobro intento = intento(
            compra, null, EstadoIntentoCobro.PENDIENTE);
        when(intentoCobroRepository.findByIdIntentoCobroForUpdate(40L))
            .thenReturn(Optional.of(intento));

        service.aplicarEstadoProveedor(40L,
            new EstadoCobroProveedor(
                EstadoPago.APROBADO, "pago-1", "approved", "Aprobado", null));

        assertThat(intento.getEstado()).isEqualTo(EstadoIntentoCobro.APROBADO);
        assertThat(intento.getFechaAprobacion()).isNotNull();
        verify(compraEntregaService).confirmarPago(intento);
    }

    @Test
    void sincronizar_consultaElIntentoPendienteYAplicaElEstado() {
        Usuario cliente = usuario(1L, "cliente");
        Compra compra = compra(cliente);
        grupo(compra);
        IntentoCobro intento = intento(compra, null, EstadoIntentoCobro.PENDIENTE);
        IntentoCobroGrupo asignacion = intento.getGrupos().get(0);
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(compraRepository.findByIdCompraAndClienteIdUsuario(10L, 1L))
            .thenReturn(Optional.of(compra));
        when(intentoCobroGrupoRepository
            .findFirstByGrupoIdCompraGrupoAndEstadoOrderByIntentoCobroFechaCreacionDesc(
                20L, EstadoIntentoCobro.PENDIENTE))
            .thenReturn(Optional.of(asignacion));
        when(proveedorPago.consultarEstado(intento))
            .thenReturn(Optional.of(new EstadoCobroProveedor(
                EstadoPago.APROBADO, "pago-1", "approved", "Aprobado", null)));
        when(intentoCobroRepository.findByIdIntentoCobroForUpdate(40L))
            .thenReturn(Optional.of(intento));
        when(compraService.obtener("cliente", 10L))
            .thenReturn(CompraDTO.builder().idCompra(10L).build());

        CompraDTO dto = service.sincronizar("cliente", 10L);

        assertThat(dto.getIdCompra()).isEqualTo(10L);
        verify(compraEntregaService).confirmarPago(intento);
    }

    @Test
    void rechazarSimulado_marcaElIntentoComoRechazado() {
        Usuario cliente = usuario(1L, "cliente");
        Compra compra = compra(cliente);
        grupo(compra);
        IntentoCobro intento = intento(
            compra, null, EstadoIntentoCobro.PENDIENTE);
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(intentoCobroRepository.findById(40L))
            .thenReturn(Optional.of(intento));
        when(intentoCobroRepository.findByIdIntentoCobroForUpdate(40L))
            .thenReturn(Optional.of(intento));
        when(compraService.obtener("cliente", 10L))
            .thenReturn(CompraDTO.builder().idCompra(10L).build());

        CompraDTO dto = service.rechazarSimulado("cliente", 40L);

        assertThat(dto.getIdCompra()).isEqualTo(10L);
        assertThat(intento.getEstado()).isEqualTo(EstadoIntentoCobro.RECHAZADO);
        verify(compraEntregaService, never()).confirmarPago(any());
    }

    private void cuandoCompraYReserva(
            Usuario cliente, Compra compra, CompraGrupo grupo) {
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(compraRepository.findByIdCompraAndClienteIdUsuario(10L, 1L))
            .thenReturn(Optional.of(compra));
        when(compraGrupoRepository.findByIdCompraGrupoAndCompraIdCompra(20L, 10L))
            .thenReturn(Optional.of(grupo));
        ReservaStock reserva = new ReservaStock();
        reserva.setEstado(EstadoReserva.ACTIVA);
        when(reservaStockRepository.findByCompraItemIdCompraItem(30L))
            .thenReturn(Optional.of(reserva));
    }

    private Compra compra(Usuario cliente) {
        Moneda moneda = moneda();
        Compra compra = new Compra();
        compra.setIdCompra(10L);
        compra.setCliente(cliente);
        compra.setMoneda(moneda);
        compra.setEstado(EstadoCompra.PENDIENTE_PAGO);
        compra.setSubtotal(new BigDecimal("100.00"));
        compra.setComisionTotal(new BigDecimal("10.00"));
        compra.setTotal(new BigDecimal("100.00"));
        return compra;
    }

    private CompraGrupo grupo(Compra compra) {
        CompraGrupo grupo = new CompraGrupo();
        grupo.setIdCompraGrupo(20L);
        grupo.setArtista(usuario(2L, "artista"));
        grupo.setMoneda(moneda());
        grupo.setSubtotal(new BigDecimal("100.00"));
        grupo.setComision(new BigDecimal("10.00"));
        grupo.setNetoArtista(new BigDecimal("90.00"));
        grupo.setEstado(EstadoCompraGrupo.PENDIENTE);
        CompraItem item = new CompraItem();
        item.setIdCompraItem(30L);
        item.setTituloProducto("Pieza");
        item.setNombreVariante("Única");
        item.setCantidad(1);
        item.setPrecioCobro(new BigDecimal("100.00"));
        grupo.agregarItem(item);
        compra.agregarGrupo(grupo);
        return grupo;
    }

    private IntentoCobro intento(
            Compra compra,
            CompraGrupo grupo,
            EstadoIntentoCobro estado) {
        CompraGrupo grupoReal = grupo != null ? grupo : compra.getGrupos().get(0);
        IntentoCobro intento = new IntentoCobro();
        intento.setIdIntentoCobro(40L);
        intento.setCompra(compra);
        intento.setEstado(estado);
        intento.setMonto(new BigDecimal("100.00"));
        intento.setMoneda(compra.getMoneda());
        intento.setProveedor("SIMULADO");
        intento.setExternalReference("COMPRA-10");
        intento.setFechaCreacion(LocalDateTime.now().minusMinutes(5));
        IntentoCobroGrupo asignacion = new IntentoCobroGrupo();
        asignacion.setGrupo(grupoReal);
        asignacion.setMonto(new BigDecimal("100.00"));
        asignacion.setComision(new BigDecimal("10.00"));
        asignacion.setNeto(new BigDecimal("90.00"));
        asignacion.setEstado(estado);
        intento.agregarGrupo(asignacion);
        return intento;
    }

    private Usuario usuario(Long id, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombreUsuario(nombre);
        return usuario;
    }

    private Moneda moneda() {
        Moneda moneda = new Moneda();
        moneda.setIdMoneda(1L);
        moneda.setCodigo("ARS");
        return moneda;
    }
}
