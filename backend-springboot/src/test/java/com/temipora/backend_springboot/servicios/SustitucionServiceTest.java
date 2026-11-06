package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.CierreSustitucionDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionSustitucion;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.AccionSolicitudRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.SustitucionOpcionRepository;
import com.temipora.backend_springboot.repositorios.SustitucionRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class SustitucionServiceTest {

    @Mock
    private SustitucionRepository sustitucionRepository;
    @Mock
    private SustitucionOpcionRepository opcionRepository;
    @Mock
    private ServicioRepository servicioRepository;
    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private AccionSolicitudRepository accionSolicitudRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ServicioService servicioService;
    @Mock
    private RecomendacionService recomendacionService;
    @Mock
    private ColaSolicitudesService colaSolicitudesService;
    @Mock
    private ConfiguracionSustitucionService configuracionService;
    @Mock
    private MonedaService monedaService;

    @InjectMocks
    private SustitucionService service;

    @Test
    void cierreDeSolicitudConServicioArchivado_derivaLaBaja() {
        Usuario cliente = cliente();
        Servicio servicio = servicio(10L, EstadoServicio.ARCHIVADO);
        Solicitud solicitud = solicitud(5L, cliente, servicio, EstadoSolicitud.RECHAZADA);

        when(solicitudRepository.findByIdSolicitud(5L)).thenReturn(Optional.of(solicitud));
        when(accionSolicitudRepository
            .findFirstBySolicitudIdSolicitudOrderByFechaDescIdAccionSolicitudDesc(5L))
            .thenReturn(Optional.empty());
        when(configuracionService.obtener()).thenReturn(configuracion(false));
        when(servicioService.convertirParaVisor(anyList(), any()))
            .thenReturn(Map.of(10L, servicioDto(10L)));

        CierreSustitucionDTO detalle = service.detalleCierrePorSolicitud(5L, 1L);

        assertThat(detalle.getMotivo()).isEqualTo("SERVICIO_DADO_DE_BAJA");
        assertThat(detalle.getTitulo()).isEqualTo("El servicio ya no está disponible");
        assertThat(detalle.getIdSolicitud()).isEqualTo(5L);
        assertThat(detalle.isProcesoHabilitado()).isFalse();
        assertThat(detalle.getOpciones()).isEmpty();
    }

    @Test
    void cierreDeSolicitudActiva_lanzaConflicto() {
        Usuario cliente = cliente();
        Servicio servicio = servicio(10L, EstadoServicio.PUBLICADO);
        Solicitud solicitud = solicitud(5L, cliente, servicio, EstadoSolicitud.PENDIENTE);
        when(solicitudRepository.findByIdSolicitud(5L)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.detalleCierrePorSolicitud(5L, 1L))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("todavía está activa");
    }

    @Test
    void cierreDeSolicitudAjena_lanzaError() {
        Usuario cliente = cliente();
        Servicio servicio = servicio(10L, EstadoServicio.PUBLICADO);
        Solicitud solicitud = solicitud(5L, cliente, servicio, EstadoSolicitud.RECHAZADA);
        when(solicitudRepository.findByIdSolicitud(5L)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.detalleCierrePorSolicitud(5L, 99L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No podés ver el resultado");
    }

    @Test
    void cierreDePedidoQueNoEstaImpago_lanzaConflicto() {
        Usuario cliente = cliente();
        Pedido pedido = new Pedido();
        pedido.setIdPedido(7L);
        pedido.setCliente(cliente);
        pedido.setEstado(EstadoPedido.EN_CURSO);
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.detalleCierrePorPedido(7L, 1L))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("no está cerrado por falta de pago");
    }

    private Usuario cliente() {
        Usuario cliente = new Usuario();
        cliente.setIdUsuario(1L);
        cliente.setNombreUsuario("cliente");
        return cliente;
    }

    private Servicio servicio(Long id, EstadoServicio estado) {
        Servicio servicio = new Servicio();
        servicio.setIdServicio(id);
        servicio.setEstado(estado);
        servicio.setCupos(1);
        return servicio;
    }

    private Solicitud solicitud(
            Long id,
            Usuario cliente,
            Servicio servicio,
            EstadoSolicitud estado) {
        Solicitud solicitud = new Solicitud();
        solicitud.setIdSolicitud(id);
        solicitud.setCliente(cliente);
        solicitud.setServicio(servicio);
        solicitud.setEstado(estado);
        solicitud.setFechaActualizacion(LocalDateTime.now());
        return solicitud;
    }

    private ConfiguracionSustitucion configuracion(boolean habilitado) {
        ConfiguracionSustitucion config = new ConfiguracionSustitucion();
        config.setHabilitado(habilitado);
        return config;
    }

    private ServicioDTO servicioDto(Long id) {
        return ServicioDTO.builder()
            .idServicio(id)
            .nombre("Servicio")
            .build();
    }
}
