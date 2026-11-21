package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.modelo.ApelacionSancion;
import com.temipora.backend_springboot.modelo.CatalogoPausadoDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.EstadoApelacion;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.repositorios.ApelacionSancionRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.PublicacionReventaRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;

@ExtendWith(MockitoExtension.class)
class SaludCuentaServiceTest {

    private static final Long ARTISTA = 7L;

    @Mock
    private SancionService sancionService;
    @Mock
    private ConfiguracionPedidoService configuracionPedidoService;
    @Mock
    private EntregaPendienteService entregaPendienteService;
    @Mock
    private ApelacionSancionRepository apelacionSancionRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ServicioRepository servicioRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private PublicacionReventaRepository publicacionReventaRepository;

    @InjectMocks
    private SaludCuentaService service;

    @BeforeEach
    void setUp() {
        ConfiguracionPedido config = new ConfiguracionPedido();
        config.setDiasGraciaEntrega(3);
        when(configuracionPedidoService.obtener()).thenReturn(config);
    }

    @Test
    void alDia_sinSancionNiMora() {
        when(sancionService.obtenerVigente(ARTISTA))
            .thenReturn(Optional.empty());

        SaludCuentaService.Salud salud = service.salud(ARTISTA);

        assertThat(salud.dto().getEstado()).isEqualTo("AL_DIA");
        assertThat(salud.dto().getDiasGraciaRestantes()).isNull();
        assertThat(salud.diasGraciaPorPedido()).isEmpty();
    }

    @Test
    void enGracia_cuandoElPlazoVencidoYQuedanDias() {
        Pedido pedido = pedido(10L);
        when(pedidoRepository
            .findByArtistaIdUsuarioAndEstadoInOrderByFechaCreacionDesc(
                eq(ARTISTA), any(), any()))
            .thenReturn(List.of(pedido));
        when(entregaPendienteService.plazoVigente(pedido))
            .thenReturn(LocalDate.now().minusDays(2).atTime(23, 59));
        when(sancionService.obtenerVigente(ARTISTA))
            .thenReturn(Optional.empty());

        SaludCuentaService.Salud salud = service.salud(ARTISTA);

        assertThat(salud.dto().getEstado()).isEqualTo("EN_GRACIA");
        assertThat(salud.dto().getDiasGraciaRestantes()).isEqualTo(1);
        assertThat(salud.diasGraciaPorPedido()).containsEntry(10L, 1);
    }

    @Test
    void noMarcaRiesgoSiElPlazoEstaLejos() {
        Pedido pedido = pedido(10L);
        when(pedidoRepository
            .findByArtistaIdUsuarioAndEstadoInOrderByFechaCreacionDesc(
                eq(ARTISTA), any(), any()))
            .thenReturn(List.of(pedido));
        when(entregaPendienteService.plazoVigente(pedido))
            .thenReturn(LocalDate.now().plusDays(2).atTime(23, 59));
        when(sancionService.obtenerVigente(ARTISTA))
            .thenReturn(Optional.empty());

        SaludCuentaService.Salud salud = service.salud(ARTISTA);

        assertThat(salud.dto().getEstado()).isEqualTo("AL_DIA");
        assertThat(salud.diasGraciaPorPedido()).isEmpty();
    }

    @Test
    void ventasSuspendidas_incluyeCatalogoPausadoYApelacion() {
        SancionCuenta sancion = sancion(
            AlcanceSancion.VENTAS, "No entregó a tiempo.");
        sancion.setFechaFin(LocalDateTime.now().plusDays(5));
        sancion.setAlcanceAfectado("servicios=1;productos=2;");
        when(sancionService.obtenerVigente(ARTISTA))
            .thenReturn(Optional.of(sancion));
        when(apelacionSancionRepository
            .findFirstBySancionIdSancionAndEstado(
                99L, EstadoApelacion.PENDIENTE))
            .thenReturn(Optional.of(new ApelacionSancion()));
        Servicio servicio = new Servicio();
        servicio.setIdServicio(1L);
        servicio.setNombre("Retrato");
        when(servicioRepository.findByIdServicio(1L))
            .thenReturn(Optional.of(servicio));
        Producto producto = new Producto();
        producto.setIdProducto(2L);
        producto.setTitulo("Poster");
        when(productoRepository.findById(2L))
            .thenReturn(Optional.of(producto));

        SaludCuentaService.Salud salud = service.salud(ARTISTA);

        assertThat(salud.dto().getEstado()).isEqualTo("VENTAS_SUSPENDIDAS");
        assertThat(salud.dto().getIdSancion()).isEqualTo(99L);
        assertThat(salud.dto().getMotivo()).isEqualTo("No entregó a tiempo.");
        assertThat(salud.dto().isApelacionPendiente()).isTrue();
        assertThat(salud.dto().getPausados()).hasSize(2);
        assertThat(salud.dto().getPausados().get(0).getTipo())
            .isEqualTo("SERVICIO");
        assertThat(salud.dto().getPausados().get(0).getNombre())
            .isEqualTo("Retrato");
        assertThat(salud.dto().getPausados().get(1).getTipo())
            .isEqualTo("PRODUCTO");
        assertThat(salud.dto().getPausados().get(1).getNombre())
            .isEqualTo("Poster");
    }

    @Test
    void limitadaEntregas_marcaElEstadoYLaFechaDeEscalada() {
        SancionCuenta sancion = sancion(
            AlcanceSancion.ENTREGAS, "No entregó dentro de la suspensión.");
        LocalDateTime escalada = LocalDateTime.now().minusDays(1);
        sancion.setFechaEscalada(escalada);
        when(sancionService.obtenerVigente(ARTISTA))
            .thenReturn(Optional.of(sancion));
        when(apelacionSancionRepository
            .findFirstBySancionIdSancionAndEstado(
                99L, EstadoApelacion.PENDIENTE))
            .thenReturn(Optional.empty());

        SaludCuentaService.Salud salud = service.salud(ARTISTA);

        assertThat(salud.dto().getEstado()).isEqualTo("LIMITADA_ENTREGAS");
        assertThat(salud.dto().getFechaEscalada()).isEqualTo(escalada);
        assertThat(salud.dto().getPausados()).isEmpty();
    }

    private SancionCuenta sancion(AlcanceSancion alcance, String motivo) {
        return SancionCuenta.builder()
            .idSancion(99L)
            .alcance(alcance)
            .motivo(motivo)
            .build();
    }

    private Pedido pedido(Long id) {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(id);
        return pedido;
    }
}
