package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.temipora.backend_springboot.modelo.AccionSolicitud;
import com.temipora.backend_springboot.modelo.CierreSustitucionDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionSustitucion;
import com.temipora.backend_springboot.modelo.CrearSustitucionRequest;
import com.temipora.backend_springboot.modelo.DisparadorSustitucion;
import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.EstadoSustitucion;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.ModoRecepcionSolicitud;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.MotivoSustitucion;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.Sustitucion;
import com.temipora.backend_springboot.modelo.SustitucionDTO;
import com.temipora.backend_springboot.modelo.SustitucionOpcion;
import com.temipora.backend_springboot.modelo.SustitucionResumenDTO;
import com.temipora.backend_springboot.modelo.TipoAccionSolicitud;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.AccionSolicitudRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.SustitucionOpcionRepository;
import com.temipora.backend_springboot.repositorios.SustitucionRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @Test
    void crear_generaEventoYFiltraCandidatos() {
        ConfiguracionSustitucion config = new ConfiguracionSustitucion();
        when(configuracionService.obtener()).thenReturn(config);

        Usuario cliente = cliente();
        cliente.setPreferenciaSensible(EPreferenciaContenidoSensible.OCULTAR);
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(cliente));

        Servicio origen = servicioCompleto(
            10L, 99L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 1);
        when(servicioRepository.findByIdServicio(10L)).thenReturn(Optional.of(origen));
        when(colaSolicitudesService.cuposOcupados(10L)).thenReturn(1);

        Servicio valido = servicioCompleto(
            20L, 30L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 2);
        Servicio sinId = servicioCompleto(
            null, 34L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 2);
        Servicio sinArtista = servicioCompleto(
            80L, null, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 2);
        Servicio inactivo = servicioCompleto(
            70L, 32L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 2);
        inactivo.getArtista().setEstadoCuenta(false);
        Servicio propio = servicioCompleto(
            60L, 1L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 2);
        Servicio conSolicitud = servicioCompleto(
            40L, 33L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 2);
        Servicio sensible = servicioCompleto(
            50L, 31L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 2);
        sensible.setContenidoSensible(true);

        when(servicioService.candidatosDisponibles()).thenReturn(List.of(
            sinId, origen, sinArtista, valido, inactivo, propio, conSolicitud, sensible));
        when(solicitudRepository.serviciosConSolicitudActiva(eq(1L), anyList()))
            .thenReturn(List.of(40L));

        Moneda moneda = moneda();
        when(monedaService.predeterminada()).thenReturn(moneda);

        List<Servicio> capturados = new ArrayList<>();
        when(recomendacionService.similares(
                eq(origen), anyList(), eq(1L), eq(moneda), any()))
            .thenAnswer(inv -> {
                capturados.addAll(inv.getArgument(1));
                return List.of(new RecomendacionService.ServicioPuntuado(
                    valido, 0.9, "Mismo estilo"));
            });

        when(sustitucionRepository.save(any(Sustitucion.class)))
            .thenAnswer(inv -> {
                Sustitucion sustitucion = inv.getArgument(0);
                sustitucion.setIdSustitucion(77L);
                return sustitucion;
            });

        ServicioDTO validoDto = servicioDto(20L);
        validoDto.setDisponibleAhora(true);
        when(servicioService.convertirParaVisor(anyList(), eq(1L)))
            .thenReturn(Map.of(20L, validoDto, 10L, servicioDto(10L)));

        SustitucionDTO dto = service.crear(1L, request(10L));

        assertThat(capturados).extracting(Servicio::getIdServicio).containsExactly(20L);
        assertThat(dto.getIdSustitucion()).isEqualTo(77L);
        assertThat(dto.getMotivo()).isEqualTo("SIN_CUPOS");
        assertThat(dto.isPuedeQuedarEnCola()).isTrue();
        assertThat(dto.getCuposDisponibles()).isZero();
        assertThat(dto.getCuposTotales()).isEqualTo(1);
        assertThat(dto.getServicioOrigen().getIdServicio()).isEqualTo(10L);
        assertThat(dto.getOpciones()).hasSize(1);
        assertThat(dto.getOpciones().get(0).getIdServicio()).isEqualTo(20L);
        assertThat(dto.getOpciones().get(0).getScore()).isEqualTo(0.9);
        assertThat(dto.getOpciones().get(0).isDisponible()).isTrue();

        ArgumentCaptor<Sustitucion> captor = ArgumentCaptor.forClass(Sustitucion.class);
        verify(sustitucionRepository).save(captor.capture());
        Sustitucion guardada = captor.getValue();
        assertThat(guardada.getDisparador()).isEqualTo(DisparadorSustitucion.DETALLE);
        assertThat(guardada.getCantidadOpciones()).isEqualTo(1);
        assertThat(guardada.getOpciones()).hasSize(1);
        assertThat(guardada.getCliente().getIdUsuario()).isEqualTo(1L);
        assertThat(guardada.getArtistaOrigen().getIdUsuario()).isEqualTo(99L);
        assertThat(guardada.getOpciones().get(0).getPrecioMomento())
            .isEqualByComparingTo("100.00");
    }

    @Test
    void crear_conProcesoDeshabilitado_derivaMotivos() {
        ConfiguracionSustitucion config = new ConfiguracionSustitucion();
        config.setHabilitado(false);
        when(configuracionService.obtener()).thenReturn(config);

        Map<Long, Servicio> servicios = new HashMap<>();
        servicios.put(1L, servicioCompleto(
            1L, 99L, EstadoServicio.ARCHIVADO, ModoRecepcionSolicitud.ABIERTA, 1));
        servicios.put(2L, servicioCompleto(
            2L, 99L, EstadoServicio.PAUSADO, ModoRecepcionSolicitud.ABIERTA, 1));
        servicios.put(3L, servicioCompleto(
            3L, 99L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.CERRADA, 1));
        servicios.put(4L, servicioCompleto(
            4L, 99L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.SOLO_ESPERA, 1));
        servicios.put(5L, servicioCompleto(
            5L, 99L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 1));
        servicios.put(6L, servicioCompleto(
            6L, 99L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 1));

        when(servicioRepository.findByIdServicio(anyLong()))
            .thenAnswer(inv -> Optional.ofNullable(servicios.get(inv.getArgument(0))));
        when(colaSolicitudesService.cuposOcupados(anyLong())).thenReturn(0);
        when(colaSolicitudesService.cuposOcupados(5L)).thenReturn(1);

        assertThat(service.crear(1L, request(1L)).getMotivo()).isEqualTo("ARCHIVADO");
        assertThat(service.crear(1L, request(2L)).getMotivo()).isEqualTo("PAUSADO");
        assertThat(service.crear(1L, request(3L)).getMotivo()).isEqualTo("CERRADA");
        assertThat(service.crear(1L, request(4L)).getMotivo()).isEqualTo("SOLO_ESPERA");
        SustitucionDTO sinCupos = service.crear(1L, request(5L));
        assertThat(sinCupos.getMotivo()).isEqualTo("SIN_CUPOS");
        assertThat(sinCupos.getMensaje()).contains("no tiene cupos");
        assertThat(sinCupos.isPuedeQuedarEnCola()).isFalse();
        assertThat(sinCupos.getCuposDisponibles()).isZero();
        SustitucionDTO disponible = service.crear(1L, request(6L));
        assertThat(disponible.getMotivo()).isNull();
        assertThat(disponible.getMensaje()).isNull();
        assertThat(disponible.getOpciones()).isEmpty();
        assertThat(disponible.isPuedeQuedarEnCola()).isFalse();
        assertThat(disponible.getCuposDisponibles()).isEqualTo(1);
    }

    @Test
    void crear_sinServicioOrigen_lanzaError() {
        assertThatThrownBy(() -> service.crear(1L, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("servicio de origen");
        assertThatThrownBy(() -> service.crear(1L, new CrearSustitucionRequest()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("servicio de origen");
    }

    @Test
    void crear_servicioInexistente_lanzaError() {
        when(configuracionService.obtener()).thenReturn(new ConfiguracionSustitucion());
        when(servicioRepository.findByIdServicio(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(1L, request(99L)))
            .isInstanceOf(java.util.NoSuchElementException.class)
            .hasMessageContaining("99");
    }

    @Test
    void crear_reutilizaEventoReciente() {
        when(configuracionService.obtener()).thenReturn(new ConfiguracionSustitucion());

        Servicio origen = servicioCompleto(
            10L, 99L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 1);
        when(servicioRepository.findByIdServicio(10L)).thenReturn(Optional.of(origen));
        when(colaSolicitudesService.cuposOcupados(10L)).thenReturn(1);

        Servicio candidato = servicioCompleto(
            20L, 30L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 2);
        Sustitucion evento = evento(
            77L, cliente(), origen, MotivoSustitucion.SIN_CUPOS,
            EstadoSustitucion.ABIERTA);
        evento.agregarOpcion(opcion(100L, candidato, 0));

        when(sustitucionRepository.recientes(
                eq(1L), eq(10L), eq(MotivoSustitucion.SIN_CUPOS),
                eq(EstadoSustitucion.ABIERTA), any(LocalDateTime.class), any()))
            .thenReturn(List.of(evento));
        when(opcionRepository.findBySustitucionIdSustitucionOrderByOrdenAsc(77L))
            .thenReturn(List.of(evento.getOpciones().get(0)));
        when(servicioService.convertirParaVisor(anyList(), any()))
            .thenReturn(Map.of(20L, servicioDto(20L), 10L, servicioDto(10L)));

        SustitucionDTO dto = service.crear(1L, request(10L));

        assertThat(dto.getIdSustitucion()).isEqualTo(77L);
        assertThat(dto.getMotivo()).isEqualTo("SIN_CUPOS");
        assertThat(dto.getCuposDisponibles()).isZero();
        assertThat(dto.getOpciones()).hasSize(1);
        assertThat(dto.getOpciones().get(0).getIdServicio()).isEqualTo(20L);
        verify(recomendacionService, never())
            .similares(any(), anyList(), any(), any(), any());
        verify(sustitucionRepository, never()).save(any());
    }

    @Test
    void resumen_calculaLaTasa() {
        when(sustitucionRepository.count()).thenReturn(10L);
        when(sustitucionRepository.countByEstado(EstadoSustitucion.RESUELTA_ALTERNATIVA))
            .thenReturn(4L);
        when(sustitucionRepository.countByEstado(EstadoSustitucion.RESUELTA_COLA))
            .thenReturn(2L);
        when(sustitucionRepository.countByEstado(EstadoSustitucion.DESCARTADA))
            .thenReturn(1L);
        when(sustitucionRepository.countByEstado(EstadoSustitucion.ABIERTA))
            .thenReturn(3L);

        SustitucionResumenDTO resumen = service.resumen();

        assertThat(resumen.getTotalEventos()).isEqualTo(10L);
        assertThat(resumen.getResueltasConAlternativa()).isEqualTo(4L);
        assertThat(resumen.getResueltasConCola()).isEqualTo(2L);
        assertThat(resumen.getDescartadas()).isEqualTo(1L);
        assertThat(resumen.getAbiertas()).isEqualTo(3L);
        assertThat(resumen.getTasaSustitucion()).isEqualTo(66.67);
    }

    @Test
    void resumen_sinResueltas_tasaCero() {
        SustitucionResumenDTO resumen = service.resumen();

        assertThat(resumen.getTotalEventos()).isZero();
        assertThat(resumen.getTasaSustitucion()).isZero();
    }

    @Test
    void obtener_noExiste_lanzaError() {
        when(sustitucionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L, 1L))
            .isInstanceOf(java.util.NoSuchElementException.class)
            .hasMessageContaining("99");
    }

    @Test
    void obtener_ajena_lanzaError() {
        Sustitucion evento = evento(
            77L, clienteCon(2L), servicio(10L, EstadoServicio.PUBLICADO),
            MotivoSustitucion.SIN_CUPOS, EstadoSustitucion.ABIERTA);
        when(sustitucionRepository.findById(77L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> service.obtener(77L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no es tuya");
    }

    @Test
    void obtener_propia_devuelveDto() {
        Servicio origen = servicioCompleto(
            10L, 99L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 1);
        Sustitucion evento = evento(
            77L, cliente(), origen, MotivoSustitucion.SIN_CUPOS,
            EstadoSustitucion.ABIERTA);
        when(sustitucionRepository.findById(77L)).thenReturn(Optional.of(evento));
        when(opcionRepository.findBySustitucionIdSustitucionOrderByOrdenAsc(77L))
            .thenReturn(List.of());
        when(servicioService.convertirParaVisor(anyList(), any()))
            .thenReturn(Map.of(10L, servicioDto(10L)));

        SustitucionDTO dto = service.obtener(77L, 1L);

        assertThat(dto.getIdSustitucion()).isEqualTo(77L);
        assertThat(dto.getMotivo()).isEqualTo("SIN_CUPOS");
        assertThat(dto.isPuedeQuedarEnCola()).isTrue();
        assertThat(dto.getServicioOrigen().getIdServicio()).isEqualTo(10L);
        assertThat(dto.getOpciones()).isEmpty();
    }

    @Test
    void descartar_marcaYGuarda() {
        Servicio origen = servicio(10L, EstadoServicio.PUBLICADO);
        Sustitucion evento = evento(
            77L, cliente(), origen, MotivoSustitucion.SIN_CUPOS,
            EstadoSustitucion.ABIERTA);
        when(sustitucionRepository.findById(77L)).thenReturn(Optional.of(evento));
        when(sustitucionRepository.save(any(Sustitucion.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        service.descartar(77L, 1L);

        assertThat(evento.getEstado()).isEqualTo(EstadoSustitucion.DESCARTADA);
        assertThat(evento.getFechaResolucion()).isNotNull();
        verify(sustitucionRepository).save(evento);
    }

    @Test
    void descartar_yaDescartada_noGuarda() {
        Servicio origen = servicio(10L, EstadoServicio.PUBLICADO);
        Sustitucion evento = evento(
            77L, cliente(), origen, MotivoSustitucion.SIN_CUPOS,
            EstadoSustitucion.DESCARTADA);
        when(sustitucionRepository.findById(77L)).thenReturn(Optional.of(evento));

        service.descartar(77L, 1L);

        verify(sustitucionRepository, never()).save(any());
    }

    @Test
    void elegir_cerrada_lanzaConflicto() {
        Sustitucion evento = evento(
            77L, cliente(), servicio(10L, EstadoServicio.PUBLICADO),
            MotivoSustitucion.SIN_CUPOS, EstadoSustitucion.DESCARTADA);
        when(sustitucionRepository.findById(77L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> service.elegir(77L, 1L, 20L))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("ya está cerrada");
    }

    @Test
    void elegir_opcionInexistente_lanzaError() {
        Sustitucion evento = evento(
            77L, cliente(), servicio(10L, EstadoServicio.PUBLICADO),
            MotivoSustitucion.SIN_CUPOS, EstadoSustitucion.ABIERTA);
        when(sustitucionRepository.findById(77L)).thenReturn(Optional.of(evento));
        when(opcionRepository.findBySustitucionIdSustitucionAndServicioIdServicio(77L, 20L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.elegir(77L, 1L, 20L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no forma parte");
    }

    @Test
    void elegir_alternativaSinCupo_lanzaConflicto() {
        Servicio origen = servicio(10L, EstadoServicio.PUBLICADO);
        Servicio candidato = servicioCompleto(
            20L, 30L, EstadoServicio.PAUSADO, ModoRecepcionSolicitud.ABIERTA, 2);
        Sustitucion evento = evento(
            77L, cliente(), origen, MotivoSustitucion.SIN_CUPOS,
            EstadoSustitucion.ABIERTA);
        SustitucionOpcion opcion = opcion(100L, candidato, 0);
        evento.agregarOpcion(opcion);
        when(sustitucionRepository.findById(77L)).thenReturn(Optional.of(evento));
        when(opcionRepository.findBySustitucionIdSustitucionAndServicioIdServicio(77L, 20L))
            .thenReturn(Optional.of(opcion));

        assertThatThrownBy(() -> service.elegir(77L, 1L, 20L))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("ya no tiene cupos");
    }

    @Test
    void elegir_marcaLaOpcionElegida() {
        Servicio origen = servicioCompleto(
            10L, 99L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 1);
        Servicio candidato = servicioCompleto(
            20L, 30L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 2);
        Sustitucion evento = evento(
            77L, cliente(), origen, MotivoSustitucion.SIN_CUPOS,
            EstadoSustitucion.ABIERTA);
        SustitucionOpcion opcion = opcion(100L, candidato, 0);
        evento.agregarOpcion(opcion);

        when(sustitucionRepository.findById(77L)).thenReturn(Optional.of(evento));
        when(opcionRepository.findBySustitucionIdSustitucionAndServicioIdServicio(77L, 20L))
            .thenReturn(Optional.of(opcion));
        when(colaSolicitudesService.cuposDisponibles(candidato)).thenReturn(1);
        when(sustitucionRepository.save(any(Sustitucion.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(opcionRepository.findBySustitucionIdSustitucionOrderByOrdenAsc(77L))
            .thenReturn(List.of(opcion));
        when(servicioService.convertirParaVisor(anyList(), any()))
            .thenReturn(Map.of(20L, servicioDto(20L), 10L, servicioDto(10L)));

        SustitucionDTO dto = service.elegir(77L, 1L, 20L);

        assertThat(evento.getEstado()).isEqualTo(EstadoSustitucion.ALTERNATIVA_ELEGIDA);
        assertThat(evento.getServicioElegido().getIdServicio()).isEqualTo(20L);
        assertThat(opcion.isElegida()).isTrue();
        assertThat(dto.getIdSustitucion()).isEqualTo(77L);
        assertThat(dto.getOpciones()).hasSize(1);
        assertThat(dto.getOpciones().get(0).getIdServicio()).isEqualTo(20L);
        verify(sustitucionRepository).save(evento);
    }

    @Test
    void resolverPorSolicitud_sinId_noHaceNada() {
        service.resolverPorSolicitud(null, 1L, 5L, 10L);

        verifyNoInteractions(sustitucionRepository);
    }

    @Test
    void resolverPorSolicitud_noExiste_noHaceNada() {
        when(sustitucionRepository.findById(77L)).thenReturn(Optional.empty());

        service.resolverPorSolicitud(77L, 1L, 5L, 10L);

        verify(sustitucionRepository, never()).save(any());
    }

    @Test
    void resolverPorSolicitud_yaResuelta_noHaceNada() {
        Sustitucion evento = evento(
            77L, cliente(), servicio(10L, EstadoServicio.PUBLICADO),
            MotivoSustitucion.SIN_CUPOS, EstadoSustitucion.RESUELTA_ALTERNATIVA);
        when(sustitucionRepository.findById(77L)).thenReturn(Optional.of(evento));

        service.resolverPorSolicitud(77L, 1L, 5L, 10L);

        verify(sustitucionRepository, never()).save(any());
    }

    @Test
    void resolverPorSolicitud_ajena_noHaceNada() {
        Sustitucion evento = evento(
            77L, clienteCon(2L), servicio(10L, EstadoServicio.PUBLICADO),
            MotivoSustitucion.SIN_CUPOS, EstadoSustitucion.ABIERTA);
        when(sustitucionRepository.findById(77L)).thenReturn(Optional.of(evento));

        service.resolverPorSolicitud(77L, 1L, 5L, 10L);

        verify(sustitucionRepository, never()).save(any());
    }

    @Test
    void resolverPorSolicitud_siQuedaEnCola() {
        Servicio origen = servicio(10L, EstadoServicio.PUBLICADO);
        Solicitud solicitud = solicitud(5L, cliente(), origen, EstadoSolicitud.PENDIENTE);
        Sustitucion evento = evento(
            77L, cliente(), origen, MotivoSustitucion.SIN_CUPOS,
            EstadoSustitucion.ABIERTA);
        when(sustitucionRepository.findById(77L)).thenReturn(Optional.of(evento));
        when(solicitudRepository.findByIdSolicitud(5L)).thenReturn(Optional.of(solicitud));

        service.resolverPorSolicitud(77L, 1L, 5L, 10L);

        assertThat(evento.getEstado()).isEqualTo(EstadoSustitucion.RESUELTA_COLA);
        assertThat(evento.getSolicitud()).isSameAs(solicitud);
        assertThat(evento.getFechaResolucion()).isNotNull();
        verify(sustitucionRepository).save(evento);
    }

    @Test
    void resolverPorSolicitud_siEligeAlternativa() {
        Servicio origen = servicio(10L, EstadoServicio.PUBLICADO);
        Servicio candidato = servicio(20L, EstadoServicio.PUBLICADO);
        Solicitud solicitud = solicitud(5L, cliente(), origen, EstadoSolicitud.PENDIENTE);
        Sustitucion evento = evento(
            77L, cliente(), origen, MotivoSustitucion.SIN_CUPOS,
            EstadoSustitucion.ABIERTA);
        when(sustitucionRepository.findById(77L)).thenReturn(Optional.of(evento));
        when(servicioRepository.findByIdServicio(20L)).thenReturn(Optional.of(candidato));
        when(solicitudRepository.findByIdSolicitud(5L)).thenReturn(Optional.of(solicitud));

        service.resolverPorSolicitud(77L, 1L, 5L, 20L);

        assertThat(evento.getEstado()).isEqualTo(EstadoSustitucion.RESUELTA_ALTERNATIVA);
        assertThat(evento.getServicioElegido()).isSameAs(candidato);
        assertThat(evento.getSolicitud()).isSameAs(solicitud);
        verify(sustitucionRepository).save(evento);
    }

    @Test
    void cierreDePedidoInexistente_lanzaError() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detalleCierrePorPedido(99L, 1L))
            .isInstanceOf(java.util.NoSuchElementException.class)
            .hasMessageContaining("99");
    }

    @Test
    void cierreDePedidoAjeno_lanzaError() {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(7L);
        pedido.setCliente(clienteCon(2L));
        pedido.setEstado(EstadoPedido.IMPAGO);
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.detalleCierrePorPedido(7L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No podés ver");
    }

    @Test
    void cierreDePedidoImpagoConHitoVencido() {
        Servicio origen = servicioCompleto(
            10L, 99L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 1);
        LocalDateTime creacion = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime limite = LocalDateTime.of(2026, 1, 2, 9, 0);
        Pedido pedido = new Pedido();
        pedido.setIdPedido(7L);
        pedido.setCliente(cliente());
        pedido.setEstado(EstadoPedido.IMPAGO);
        pedido.setServicio(origen);
        pedido.setFechaCreacion(creacion);
        HitoPedido hito = new HitoPedido();
        hito.setEstadoPago(EstadoPagoHito.VENCIDO);
        hito.setFechaLimitePago(limite);
        pedido.agregarHito(hito);

        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido));
        when(configuracionService.obtener()).thenReturn(configuracion(false));
        when(servicioService.convertirParaVisor(anyList(), any()))
            .thenReturn(Map.of(10L, servicioDto(10L)));

        CierreSustitucionDTO detalle = service.detalleCierrePorPedido(7L, 1L);

        assertThat(detalle.getContexto()).isEqualTo("PEDIDO");
        assertThat(detalle.getMotivo()).isEqualTo("PEDIDO_IMPAGO");
        assertThat(detalle.getFecha()).isEqualTo(limite);
        assertThat(detalle.getServicio().getIdServicio()).isEqualTo(10L);
        assertThat(detalle.isProcesoHabilitado()).isFalse();
        assertThat(detalle.getOpciones()).isEmpty();
    }

    @Test
    void cierreDePedidoImpagoSinHitoVencidoUsaLaFechaDeCreacion() {
        Servicio origen = servicioCompleto(
            10L, 99L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 1);
        LocalDateTime creacion = LocalDateTime.of(2026, 1, 3, 9, 0);
        Pedido pedido = new Pedido();
        pedido.setIdPedido(7L);
        pedido.setCliente(cliente());
        pedido.setEstado(EstadoPedido.IMPAGO);
        pedido.setServicio(origen);
        pedido.setFechaCreacion(creacion);

        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido));
        when(configuracionService.obtener()).thenReturn(configuracion(false));
        when(servicioService.convertirParaVisor(anyList(), any()))
            .thenReturn(Map.of(10L, servicioDto(10L)));

        CierreSustitucionDTO detalle = service.detalleCierrePorPedido(7L, 1L);

        assertThat(detalle.getFecha()).isEqualTo(creacion);
        assertThat(detalle.getMotivo()).isEqualTo("PEDIDO_IMPAGO");
    }

    @Test
    void cierreDeSolicitudVencida_derivaElVencimiento() {
        Servicio origen = servicioCompleto(
            10L, 99L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 1);
        Solicitud solicitud = solicitud(5L, cliente(), origen, EstadoSolicitud.VENCIDA);
        when(solicitudRepository.findByIdSolicitud(5L)).thenReturn(Optional.of(solicitud));
        when(configuracionService.obtener()).thenReturn(configuracion(false));
        when(servicioService.convertirParaVisor(anyList(), any()))
            .thenReturn(Map.of(10L, servicioDto(10L)));

        CierreSustitucionDTO detalle = service.detalleCierrePorSolicitud(5L, 1L);

        assertThat(detalle.getMotivo()).isEqualTo("PRESUPUESTO_VENCIDO");
        assertThat(detalle.getFecha()).isEqualTo(solicitud.getFechaActualizacion());
    }

    @Test
    void cierreDeSolicitudConPresupuestoRetirado_derivaElRetiro() {
        Servicio origen = servicioCompleto(
            10L, 99L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 1);
        Solicitud solicitud = solicitud(5L, cliente(), origen, EstadoSolicitud.RECHAZADA);
        LocalDateTime fechaAccion = LocalDateTime.of(2026, 2, 1, 12, 0);
        AccionSolicitud accion = new AccionSolicitud();
        accion.setTipoAccion(TipoAccionSolicitud.RETIRO_PRESUPUESTO);
        accion.setFecha(fechaAccion);

        when(solicitudRepository.findByIdSolicitud(5L)).thenReturn(Optional.of(solicitud));
        when(accionSolicitudRepository
            .findFirstBySolicitudIdSolicitudOrderByFechaDescIdAccionSolicitudDesc(5L))
            .thenReturn(Optional.of(accion));
        when(configuracionService.obtener()).thenReturn(configuracion(false));
        when(servicioService.convertirParaVisor(anyList(), any()))
            .thenReturn(Map.of(10L, servicioDto(10L)));

        CierreSustitucionDTO detalle = service.detalleCierrePorSolicitud(5L, 1L);

        assertThat(detalle.getMotivo()).isEqualTo("PRESUPUESTO_RETIRADO");
        assertThat(detalle.getFecha()).isEqualTo(fechaAccion);
    }

    @Test
    void cierreDeSolicitudRechazadaSinAccion_derivaElRechazo() {
        Servicio origen = servicioCompleto(
            10L, 99L, EstadoServicio.PUBLICADO, ModoRecepcionSolicitud.ABIERTA, 1);
        Solicitud solicitud = solicitud(5L, cliente(), origen, EstadoSolicitud.RECHAZADA);
        when(solicitudRepository.findByIdSolicitud(5L)).thenReturn(Optional.of(solicitud));
        when(configuracionService.obtener()).thenReturn(configuracion(false));
        when(servicioService.convertirParaVisor(anyList(), any()))
            .thenReturn(Map.of(10L, servicioDto(10L)));

        CierreSustitucionDTO detalle = service.detalleCierrePorSolicitud(5L, 1L);

        assertThat(detalle.getMotivo()).isEqualTo("SOLICITUD_RECHAZADA");
        assertThat(detalle.getTitulo()).isEqualTo("El artista rechazó tu solicitud");
    }

    @Test
    void cierreDeSolicitudSinServicio_devuelveResumenSinAlternativas() {
        Solicitud solicitud = solicitud(5L, cliente(), null, EstadoSolicitud.RECHAZADA);
        when(solicitudRepository.findByIdSolicitud(5L)).thenReturn(Optional.of(solicitud));
        when(configuracionService.obtener()).thenReturn(configuracion(false));

        CierreSustitucionDTO detalle = service.detalleCierrePorSolicitud(5L, 1L);

        assertThat(detalle.getMotivo()).isEqualTo("SOLICITUD_RECHAZADA");
        assertThat(detalle.getServicio()).isNull();
        assertThat(detalle.isProcesoHabilitado()).isFalse();
        assertThat(detalle.getOpciones()).isEmpty();
    }

    private Usuario cliente() {
        return clienteCon(1L);
    }

    private Usuario clienteCon(Long id) {
        Usuario cliente = new Usuario();
        cliente.setIdUsuario(id);
        cliente.setNombreUsuario("cliente" + id);
        return cliente;
    }

    private Servicio servicio(Long id, EstadoServicio estado) {
        Servicio servicio = new Servicio();
        servicio.setIdServicio(id);
        servicio.setEstado(estado);
        servicio.setCupos(1);
        return servicio;
    }

    private Servicio servicioCompleto(
            Long id,
            Long idArtista,
            EstadoServicio estado,
            ModoRecepcionSolicitud modo,
            int cupos) {
        Servicio servicio = servicio(id, estado);
        servicio.setModoRecepcion(modo);
        servicio.setCupos(cupos);
        servicio.setPrecioBase(new BigDecimal("100.00"));
        servicio.setMoneda(moneda());
        if (idArtista != null) {
            Usuario artista = new Usuario();
            artista.setIdUsuario(idArtista);
            artista.setNombreUsuario("artista" + idArtista);
            artista.setEstadoCuenta(true);
            servicio.setArtista(artista);
        }
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

    private Sustitucion evento(
            Long id,
            Usuario cliente,
            Servicio origen,
            MotivoSustitucion motivo,
            EstadoSustitucion estado) {
        Sustitucion sustitucion = new Sustitucion();
        sustitucion.setIdSustitucion(id);
        sustitucion.setCliente(cliente);
        sustitucion.setServicioOrigen(origen);
        sustitucion.setMotivo(motivo);
        sustitucion.setEstado(estado);
        sustitucion.setDisparador(DisparadorSustitucion.DETALLE);
        return sustitucion;
    }

    private SustitucionOpcion opcion(Long id, Servicio servicio, int orden) {
        SustitucionOpcion opcion = new SustitucionOpcion();
        opcion.setIdSustitucionOpcion(id);
        opcion.setServicio(servicio);
        opcion.setArtista(servicio.getArtista());
        opcion.setOrden(orden);
        opcion.setScore(new BigDecimal("0.9000"));
        opcion.setMotivo("Mismo estilo");
        opcion.setPrecioMomento(servicio.getPrecioBase());
        opcion.setMoneda(servicio.getMoneda());
        return opcion;
    }

    private ConfiguracionSustitucion configuracion(boolean habilitado) {
        ConfiguracionSustitucion config = new ConfiguracionSustitucion();
        config.setHabilitado(habilitado);
        return config;
    }

    private CrearSustitucionRequest request(Long idServicio) {
        CrearSustitucionRequest request = new CrearSustitucionRequest();
        request.setIdServicioOrigen(idServicio);
        return request;
    }

    private Moneda moneda() {
        return Moneda.builder()
            .idMoneda(1L)
            .codigo("ARS")
            .nombre("Peso")
            .build();
    }

    private ServicioDTO servicioDto(Long id) {
        return ServicioDTO.builder()
            .idServicio(id)
            .nombre("Servicio")
            .build();
    }
}
