package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.temipora.backend_springboot.modelo.ArancelEstimadoDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionComision;
import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.ConfiguracionPresupuesto;
import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.ETiempoEntregaUnidad;
import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.HitoPresupuestoRequest;
import com.temipora.backend_springboot.modelo.ItemPresupuestoDTO;
import com.temipora.backend_springboot.modelo.ItemPresupuestoRequest;
import com.temipora.backend_springboot.modelo.ItemSolicitud;
import com.temipora.backend_springboot.modelo.ItemSolicitudDerecho;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Presupuesto;
import com.temipora.backend_springboot.modelo.PresupuestoDTO;
import com.temipora.backend_springboot.modelo.PresupuestoRequest;
import com.temipora.backend_springboot.modelo.RecargoPresupuestoDTO;
import com.temipora.backend_springboot.modelo.RecargoPresupuestoRequest;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.SolicitudDTO;
import com.temipora.backend_springboot.modelo.TipoRecargo;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.PresupuestoRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.servicios.pagos.ArancelEstimadoService;

@ExtendWith(MockitoExtension.class)
class PresupuestoServiceTest {

    private static final Long CLIENTE = 1L;
    private static final Long ARTISTA = 2L;
    private static final Long SERVICIO = 7L;
    private static final Long SOLICITUD = 11L;
    private static final Long ITEM_1 = 21L;
    private static final Long ITEM_2 = 22L;

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private PresupuestoRepository presupuestoRepository;

    @Mock
    private ColaSolicitudesService colaSolicitudesService;

    @Mock
    private ListaEsperaService listaEsperaService;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private ConfiguracionPresupuestoService configuracionPresupuestoService;

    @Mock
    private ComisionService comisionService;

    @Mock
    private ConfiguracionPedidoService configuracionPedidoService;

    @Mock
    private MonedaVistaService monedaVistaService;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private ArancelEstimadoService arancelEstimadoService;

    @InjectMocks
    private PresupuestoService presupuestoService;

    private Usuario cliente;
    private Usuario artista;
    private Servicio servicio;
    private Moneda moneda;
    private ConfiguracionPresupuesto configuracion;
    private Solicitud solicitud;

    @BeforeEach
    void setUp() {
        cliente = new Usuario();
        cliente.setIdUsuario(CLIENTE);
        cliente.setNombreUsuario("cliente");

        artista = new Usuario();
        artista.setIdUsuario(ARTISTA);
        artista.setNombreUsuario("artista");

        moneda = Moneda.builder()
            .idMoneda(3L)
            .codigo("ARS")
            .nombre("Peso argentino")
            .build();

        servicio = new Servicio();
        servicio.setIdServicio(SERVICIO);
        servicio.setNombre("Ilustración");
        servicio.setArtista(artista);
        servicio.setEstado(EstadoServicio.PUBLICADO);
        servicio.setCupos(3);
        servicio.setPrecioBase(new BigDecimal("100.00"));
        servicio.setMoneda(moneda);

        configuracion = new ConfiguracionPresupuesto();
        configuracion.setVigenciaHoras(72);

        solicitud = crearSolicitud(EstadoSolicitud.PENDIENTE);
    }

    @Test
    void crear_sinRecargos_calculaBasePorCantidad() {
        prepararCreacion();

        SolicitudDTO resultado = presupuestoService.crear(
            SOLICITUD, ARTISTA, request());

        assertThat(resultado.getEstado()).isEqualTo(EstadoSolicitud.PRESUPUESTADA);
        assertThat(resultado.getPresupuesto().getPrecioBaseTotal())
            .isEqualByComparingTo("200.00");
        assertThat(resultado.getPresupuesto().getPrecioFinal())
            .isEqualByComparingTo("200.00");
        assertThat(resultado.getPresupuesto().getItems()).hasSize(2);
        assertThat(resultado.getPresupuesto().getItems().get(0).getPrecioFinal())
            .isEqualByComparingTo("100.00");
        assertThat(resultado.getPresupuesto().getItems().get(1).getPrecioFinal())
            .isEqualByComparingTo("100.00");

        verify(listaEsperaService).marcarConvertida(solicitud);
        verify(notificacionService).crearParaUsuario(
            eq(CLIENTE),
            eq("PRESUPUESTO_RECIBIDO"),
            eq("Recibiste un presupuesto"),
            contains("200.00 ARS"),
            eq(SOLICITUD),
            eq("SOLICITUD"));
    }

    @Test
    void crear_informaArancelMpEstimadoAlArtista() {
        prepararCreacion();

        SolicitudDTO resultado = presupuestoService.crear(
            SOLICITUD, ARTISTA, request());

        PresupuestoDTO presupuesto = resultado.getPresupuesto();
        assertThat(presupuesto.getArancelMpPorcentaje())
            .isEqualByComparingTo("4.2229");
        assertThat(presupuesto.getArancelMpEstimado())
            .isEqualByComparingTo("8.45");
        assertThat(presupuesto.getMontoNetoArtistaConArancel())
            .isEqualByComparingTo("171.55");
        assertThat(presupuesto.getHitos()).hasSize(1);
        assertThat(presupuesto.getHitos().get(0).getArancelMpEstimado())
            .isEqualByComparingTo("8.45");
        assertThat(presupuesto.getHitos().get(0).getMontoNetoConArancel())
            .isEqualByComparingTo("171.55");
    }

    @Test
    void crear_calculaComisionYNetoDelArtista() {
        prepararCreacion();

        SolicitudDTO resultado = presupuestoService.crear(
            SOLICITUD, ARTISTA, request());

        PresupuestoDTO presupuesto = resultado.getPresupuesto();
        assertThat(presupuesto.getPrecioFinal()).isEqualByComparingTo("200.00");
        assertThat(presupuesto.getComisionPorcentaje()).isEqualByComparingTo("10.00");
        assertThat(presupuesto.getComisionMonto()).isEqualByComparingTo("20.00");
        assertThat(presupuesto.getMontoNetoArtista()).isEqualByComparingTo("180.00");
    }

    @Test
    void crear_sinHitos_creaUnPagoUnicoAlInicio() {
        prepararCreacion();

        SolicitudDTO resultado = presupuestoService.crear(
            SOLICITUD, ARTISTA, request());

        assertThat(resultado.getPresupuesto().getHitos()).hasSize(1);
        assertThat(resultado.getPresupuesto().getHitos().get(0).getTipo())
            .isEqualTo("SENA");
        assertThat(resultado.getPresupuesto().getHitos().get(0).getPorcentaje())
            .isEqualByComparingTo("100.00");
        assertThat(resultado.getPresupuesto().getHitos().get(0).getMonto())
            .isEqualByComparingTo("200.00");
        assertThat(resultado.getPresupuesto().getHitos().get(0).isRequiereEntrega())
            .isTrue();
    }

    @Test
    void crear_conSenaSkeb_laSenaNoRequiereEntregable() {
        prepararCreacion();
        PresupuestoRequest request = request();
        HitoPresupuestoRequest sena = hito("50");
        sena.setRequiereEntrega(false);
        request.setHitos(new ArrayList<>(List.of(sena, hito("50"))));

        SolicitudDTO resultado = presupuestoService.crear(SOLICITUD, ARTISTA, request);

        var hitos = resultado.getPresupuesto().getHitos();
        assertThat(hitos).hasSize(2);
        assertThat(hitos.get(0).getTipo()).isEqualTo("SENA");
        assertThat(hitos.get(0).isRequiereEntrega()).isFalse();
        assertThat(hitos.get(1).getTipo()).isEqualTo("ENTREGA");
        assertThat(hitos.get(1).isRequiereEntrega()).isTrue();
    }

    @Test
    void crear_conAlcance_loExponeEnElHitoDelPresupuesto() {
        prepararCreacion();
        PresupuestoRequest request = request();
        HitoPresupuestoRequest sena = hito("50");
        sena.setDescripcion("Bocetos de los 2 dibujos");
        request.setHitos(new ArrayList<>(List.of(sena, hito("50"))));

        SolicitudDTO resultado = presupuestoService.crear(SOLICITUD, ARTISTA, request);

        assertThat(resultado.getPresupuesto().getHitos().get(0).getDescripcion())
            .isEqualTo("Bocetos de los 2 dibujos");
    }

    @Test
    void crear_conDosHitos_losGuardaConComisionYNeto() {
        prepararCreacion();
        PresupuestoRequest request = request();
        request.setHitos(new ArrayList<>(List.of(hito("50"), hito("50"))));

        SolicitudDTO resultado = presupuestoService.crear(SOLICITUD, ARTISTA, request);

        assertThat(resultado.getPresupuesto().getHitos()).hasSize(2);
        var sena = resultado.getPresupuesto().getHitos().get(0);
        var entrega = resultado.getPresupuesto().getHitos().get(1);
        assertThat(sena.getTipo()).isEqualTo("SENA");
        assertThat(sena.getMonto()).isEqualByComparingTo("100.00");
        assertThat(sena.getComisionMonto()).isEqualByComparingTo("10.00");
        assertThat(sena.getMontoNeto()).isEqualByComparingTo("90.00");
        assertThat(entrega.getTipo()).isEqualTo("ENTREGA");
        assertThat(entrega.getMonto()).isEqualByComparingTo("100.00");
    }

    @Test
    void crear_conHitosQueNoSumanCien_lanzaError() {
        prepararCreacion();
        PresupuestoRequest request = request();
        request.setHitos(new ArrayList<>(List.of(hito("50"), hito("40"))));

        assertThatThrownBy(() -> presupuestoService.crear(SOLICITUD, ARTISTA, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("sumar 100");

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void crear_conMonedaDistinta_guardaCotizacionYCobroEnArs() {
        Moneda usd = Moneda.builder()
            .idMoneda(4L)
            .codigo("USD")
            .nombre("Dólar estadounidense")
            .build();
        servicio.setMoneda(usd);
        solicitud.setMoneda(usd);
        prepararCreacion();
        when(monedaVistaService.factorCobro(eq(usd), any(LocalDateTime.class)))
            .thenReturn(new BigDecimal("1200"));
        when(monedaVistaService.factorOrigenAMostrada(
                eq(usd), eq(moneda), any(LocalDateTime.class)))
            .thenReturn(new BigDecimal("1200"));
        when(monedaVistaService.factorCobroAMostrada(
                eq(moneda), any(LocalDateTime.class)))
            .thenReturn(BigDecimal.ONE);

        SolicitudDTO resultado = presupuestoService.crear(
            SOLICITUD, ARTISTA, request());

        PresupuestoDTO presupuesto = resultado.getPresupuesto();
        assertThat(presupuesto.getMoneda()).isEqualTo("USD");
        assertThat(presupuesto.getMonedaCobro()).isEqualTo("ARS");
        assertThat(presupuesto.getCotizacionCobro())
            .isEqualByComparingTo("1200");
        assertThat(presupuesto.getPrecioBaseTotalCobro())
            .isEqualByComparingTo("240000.00");
        assertThat(presupuesto.getPrecioFinalCobro())
            .isEqualByComparingTo("240000.00");
        assertThat(presupuesto.getMonedaMostrada()).isEqualTo("ARS");
        assertThat(presupuesto.getPrecioFinalMostrado())
            .isEqualByComparingTo("240000.00");
        assertThat(presupuesto.getItems().get(0).getPrecioFinalMostrado())
            .isEqualByComparingTo("120000.00");
    }

    @Test
    void crear_sinCotizacion_lanzaConflictoYNoGuarda() {
        prepararCreacion();
        when(monedaVistaService.factorCobro(
                eq(moneda), any(LocalDateTime.class)))
            .thenReturn(null);

        assertThatThrownBy(() -> presupuestoService.crear(
            SOLICITUD, ARTISTA, request()))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("cotización");

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void crear_conRecargosPorDibujo_calculaCadaItem() {
        prepararCreacion();
        PresupuestoRequest request = request();
        request.getItems().get(0).getRecargos().add(
            recargo("Personaje extra", TipoRecargo.PORCENTAJE, "50"));
        request.getItems().get(0).getRecargos().add(
            recargo("Fondo detallado", TipoRecargo.FIJO, "20"));

        SolicitudDTO resultado = presupuestoService.crear(SOLICITUD, ARTISTA, request);

        assertThat(resultado.getPresupuesto().getPrecioBaseTotal())
            .isEqualByComparingTo("200.00");
        assertThat(resultado.getPresupuesto().getItems().get(0).getPrecioFinal())
            .isEqualByComparingTo("170.00");
        assertThat(resultado.getPresupuesto().getItems().get(1).getPrecioFinal())
            .isEqualByComparingTo("100.00");
        assertThat(resultado.getPresupuesto().getPrecioFinal())
            .isEqualByComparingTo("270.00");
    }

    @Test
    void crear_variosPorcentajes_seSumanSinComponer() {
        prepararCreacion();
        PresupuestoRequest request = request();
        request.getItems().get(0).getRecargos().add(
            recargo("Personaje extra", TipoRecargo.PORCENTAJE, "50"));
        request.getItems().get(0).getRecargos().add(
            recargo("Armadura", TipoRecargo.PORCENTAJE, "25"));

        SolicitudDTO resultado = presupuestoService.crear(SOLICITUD, ARTISTA, request);

        assertThat(resultado.getPresupuesto().getItems().get(0).getPrecioFinal())
            .isEqualByComparingTo("175.00");
        assertThat(resultado.getPresupuesto().getPrecioFinal())
            .isEqualByComparingTo("275.00");
    }

    @Test
    void crear_conDerechos_agregaRecargosAutomaticosYLosSuma() {
        prepararCreacion();
        ItemSolicitud item = solicitud.getItems().get(0);
        agregarDerecho(item, 1L, "CONTENIDO",
            "Uso comercial en producción de contenido", "30");
        agregarDerecho(item, 2L, "MERCHANDISING",
            "Uso comercial como merchandising", "20");

        SolicitudDTO resultado = presupuestoService.crear(SOLICITUD, ARTISTA, request());

        ItemPresupuestoDTO itemDto = resultado.getPresupuesto().getItems().get(0);
        assertThat(itemDto.getPrecioFinal()).isEqualByComparingTo("150.00");
        assertThat(itemDto.getRecargos()).hasSize(2);
        assertThat(itemDto.getRecargos())
            .allMatch(RecargoPresupuestoDTO::isAutomatico);
        assertThat(itemDto.getRecargos().get(0).getValor())
            .isEqualByComparingTo("30.00");
        assertThat(itemDto.getRecargos().get(1).getValor())
            .isEqualByComparingTo("20.00");
        assertThat(resultado.getPresupuesto().getPrecioFinal())
            .isEqualByComparingTo("250.00");
    }

    @Test
    void crear_conDerechoYRecargoManual_sumaAmbosSinComponer() {
        prepararCreacion();
        agregarDerecho(solicitud.getItems().get(0), 1L, "CONTENIDO",
            "Uso comercial en producción de contenido", "30");
        PresupuestoRequest request = request();
        request.getItems().get(0).getRecargos().add(
            recargo("Personaje extra", TipoRecargo.PORCENTAJE, "20"));

        SolicitudDTO resultado = presupuestoService.crear(SOLICITUD, ARTISTA, request);

        ItemPresupuestoDTO itemDto = resultado.getPresupuesto().getItems().get(0);
        assertThat(itemDto.getPrecioFinal()).isEqualByComparingTo("150.00");
        assertThat(itemDto.getRecargos()).hasSize(2);
        assertThat(itemDto.getRecargos().get(0).isAutomatico()).isTrue();
        assertThat(itemDto.getRecargos().get(1).isAutomatico()).isFalse();
    }

    @Test
    void crear_faltanDibujos_lanzaError() {
        prepararCreacion();
        PresupuestoRequest request = request();
        request.getItems().remove(1);

        assertThatThrownBy(() -> presupuestoService.crear(SOLICITUD, ARTISTA, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("todos los dibujos");

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void crear_dibujoAjeno_lanzaError() {
        prepararCreacion();
        PresupuestoRequest request = request();
        request.getItems().get(0).setIdItemSolicitud(99L);

        assertThatThrownBy(() -> presupuestoService.crear(SOLICITUD, ARTISTA, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no pertenece a la solicitud");

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void crear_dibujoDuplicado_lanzaError() {
        prepararCreacion();
        PresupuestoRequest request = request();
        request.getItems().get(1).setIdItemSolicitud(ITEM_1);

        assertThatThrownBy(() -> presupuestoService.crear(SOLICITUD, ARTISTA, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("repetir un dibujo");

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void crear_sinCupos_lanzaConflicto() {
        prepararCreacion();
        when(colaSolicitudesService.evaluar(solicitud))
            .thenReturn(new ColaSolicitudesService.EvaluacionPresupuesto(
                false, 1, "No hay cupos libres. Podés ampliar los cupos del servicio."));

        assertThatThrownBy(() -> presupuestoService.crear(SOLICITUD, ARTISTA, request()))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("No hay cupos libres");

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void crear_fueraDeTurno_lanzaConflicto() {
        prepararCreacion();
        when(colaSolicitudesService.evaluar(solicitud))
            .thenReturn(new ColaSolicitudesService.EvaluacionPresupuesto(
                false, 2, "Hay solicitudes anteriores esperando cupo."));

        assertThatThrownBy(() -> presupuestoService.crear(SOLICITUD, ARTISTA, request()))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("solicitudes anteriores");

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void crear_otroArtista_lanzaAccesoDenegado() {
        prepararCreacion();

        assertThatThrownBy(() -> presupuestoService.crear(SOLICITUD, 99L, request()))
            .isInstanceOf(AccesoDenegadoException.class)
            .hasMessageContaining("No podés presupuestar");

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void crear_estadoInvalido_lanzaConflicto() {
        solicitud.setEstado(EstadoSolicitud.PRESUPUESTADA);
        prepararCreacion();

        assertThatThrownBy(() -> presupuestoService.crear(SOLICITUD, ARTISTA, request()))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("pendientes o en lista de espera");

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void crear_presupuestoExistente_lanzaConflicto() {
        prepararCreacion();
        when(presupuestoRepository.existsBySolicitudIdSolicitud(SOLICITUD)).thenReturn(true);

        assertThatThrownBy(() -> presupuestoService.crear(SOLICITUD, ARTISTA, request()))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("Ya enviaste un presupuesto");

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void crear_servicioArchivado_lanzaConflicto() {
        servicio.setEstado(EstadoServicio.ARCHIVADO);
        prepararCreacion();

        assertThatThrownBy(() -> presupuestoService.crear(SOLICITUD, ARTISTA, request()))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("archivado");

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void crear_sinFecha_usaVigenciaPorDefecto() {
        prepararCreacion();
        LocalDateTime antes = LocalDateTime.now();

        SolicitudDTO resultado = presupuestoService.crear(
            SOLICITUD, ARTISTA, request());

        LocalDateTime vencimiento = resultado.getPresupuesto().getFechaVencimiento();
        assertThat(vencimiento).isAfter(antes.plusHours(71));
        assertThat(vencimiento).isBefore(antes.plusHours(73));
    }

    @Test
    void crear_conFecha_usaFinDelDiaElegido() {
        prepararCreacion();
        LocalDate fecha = LocalDate.now().plusDays(3);
        PresupuestoRequest request = request();
        request.setFechaVencimiento(fecha);

        SolicitudDTO resultado = presupuestoService.crear(SOLICITUD, ARTISTA, request);

        assertThat(resultado.getPresupuesto().getFechaVencimiento())
            .isEqualTo(fecha.atTime(23, 59, 59));
    }

    @Test
    void crear_conFechaPasada_lanzaError() {
        prepararCreacion();
        PresupuestoRequest request = request();
        request.setFechaVencimiento(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> presupuestoService.crear(SOLICITUD, ARTISTA, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("futura");

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void crear_guardaFechaInicioEstimada() {
        prepararCreacion();
        LocalDate inicio = LocalDate.now().plusDays(10);
        PresupuestoRequest request = request();
        request.setFechaInicioEstimada(inicio);

        SolicitudDTO resultado = presupuestoService.crear(SOLICITUD, ARTISTA, request);

        assertThat(resultado.getPresupuesto().getFechaInicioEstimada())
            .isEqualTo(inicio);
        assertThat(resultado.getPresupuesto().getTiempoEntregaDias())
            .isEqualTo(14);
    }

    @Test
    void crear_inicioEstimadoPasado_lanzaError() {
        prepararCreacion();
        PresupuestoRequest request = request();
        request.setFechaInicioEstimada(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> presupuestoService.crear(SOLICITUD, ARTISTA, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("anterior a hoy");

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void crear_inicioEstimadoMayorAUnAnio_lanzaError() {
        prepararCreacion();
        PresupuestoRequest request = request();
        request.setFechaInicioEstimada(LocalDate.now().plusYears(1).plusDays(1));

        assertThatThrownBy(() -> presupuestoService.crear(SOLICITUD, ARTISTA, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("superar un año");

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void crear_plazoMayorAUnAnio_lanzaError() {
        prepararCreacion();
        PresupuestoRequest request = request();
        request.setTiempoEntregaValor(53);
        request.setTiempoEntregaUnidad(ETiempoEntregaUnidad.SEMANAS);

        assertThatThrownBy(() -> presupuestoService.crear(SOLICITUD, ARTISTA, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("plazo máximo")
            .hasMessageContaining("1 año");

        verify(presupuestoRepository, never()).save(any());
    }

    @Test
    void crear_plazoDeUnAnio_esValido() {
        prepararCreacion();
        PresupuestoRequest request = request();
        request.setTiempoEntregaValor(12);
        request.setTiempoEntregaUnidad(ETiempoEntregaUnidad.MESES);

        SolicitudDTO resultado = presupuestoService.crear(SOLICITUD, ARTISTA, request);

        assertThat(resultado.getPresupuesto().getTiempoEntregaValor()).isEqualTo(12);
        assertThat(resultado.getPresupuesto().getTiempoEntregaUnidad())
            .isEqualTo("MESES");
        assertThat(resultado.getPresupuesto().getTiempoEntregaDias()).isEqualTo(360);
    }

    @Test
    void crear_conNotaYMensaje_losGuarda() {
        prepararCreacion();
        PresupuestoRequest request = request();
        request.setMensaje("  Hola, este es el presupuesto.  ");
        request.getItems().get(0).setNota("  Incluye dos personajes  ");

        SolicitudDTO resultado = presupuestoService.crear(SOLICITUD, ARTISTA, request);

        assertThat(resultado.getPresupuesto().getMensaje())
            .isEqualTo("Hola, este es el presupuesto.");
        assertThat(resultado.getPresupuesto().getItems().get(0).getNota())
            .isEqualTo("Incluye dos personajes");
    }

    @Test
    void expirarVencidos_marcaVencidaYNotifica() {
        Solicitud presupuestada = crearSolicitud(EstadoSolicitud.PRESUPUESTADA);
        Presupuesto presupuesto = new Presupuesto();
        presupuesto.setIdPresupuesto(5L);
        presupuesto.setSolicitud(presupuestada);
        presupuesto.setFechaVencimiento(LocalDateTime.now().minusMinutes(1));
        when(presupuestoRepository.findVencidos(
            any(), eq(EstadoSolicitud.PRESUPUESTADA), any(Pageable.class)))
            .thenReturn(List.of(presupuesto));
        when(solicitudRepository.findByIdSolicitudForUpdate(SOLICITUD))
            .thenReturn(Optional.of(presupuestada));

        int expirados = presupuestoService.expirarVencidos();

        assertThat(expirados).isEqualTo(1);
        assertThat(presupuestada.getEstado()).isEqualTo(EstadoSolicitud.VENCIDA);
        verify(notificacionService).crearParaUsuario(
            eq(CLIENTE), eq("PRESUPUESTO_VENCIDO"), eq("Presupuesto vencido"),
            contains("venció"), eq(SOLICITUD), eq("SOLICITUD"));
        verify(notificacionService).crearParaUsuario(
            eq(ARTISTA), eq("PRESUPUESTO_VENCIDO"), eq("Presupuesto vencido"),
            contains("se liberó el cupo"), eq(SOLICITUD), eq("SOLICITUD"));
    }

    @Test
    void expirarVencidos_ignoraSolicitudYaCerrada() {
        Solicitud rechazada = crearSolicitud(EstadoSolicitud.RECHAZADA);
        Presupuesto presupuesto = new Presupuesto();
        presupuesto.setIdPresupuesto(5L);
        presupuesto.setSolicitud(rechazada);
        presupuesto.setFechaVencimiento(LocalDateTime.now().minusMinutes(1));
        when(presupuestoRepository.findVencidos(
            any(), eq(EstadoSolicitud.PRESUPUESTADA), any(Pageable.class)))
            .thenReturn(List.of(presupuesto));
        when(solicitudRepository.findByIdSolicitudForUpdate(SOLICITUD))
            .thenReturn(Optional.of(rechazada));

        int expirados = presupuestoService.expirarVencidos();

        assertThat(expirados).isZero();
        assertThat(rechazada.getEstado()).isEqualTo(EstadoSolicitud.RECHAZADA);
        verifyNoInteractions(notificacionService);
    }

    @Test
    void expirarVencidos_ignoraFechaFutura() {
        Solicitud presupuestada = crearSolicitud(EstadoSolicitud.PRESUPUESTADA);
        Presupuesto presupuesto = new Presupuesto();
        presupuesto.setIdPresupuesto(5L);
        presupuesto.setSolicitud(presupuestada);
        presupuesto.setFechaVencimiento(LocalDateTime.now().plusHours(1));
        when(presupuestoRepository.findVencidos(
            any(), eq(EstadoSolicitud.PRESUPUESTADA), any(Pageable.class)))
            .thenReturn(List.of(presupuesto));
        when(solicitudRepository.findByIdSolicitudForUpdate(SOLICITUD))
            .thenReturn(Optional.of(presupuestada));

        int expirados = presupuestoService.expirarVencidos();

        assertThat(expirados).isZero();
        assertThat(presupuestada.getEstado()).isEqualTo(EstadoSolicitud.PRESUPUESTADA);
        verifyNoInteractions(notificacionService);
    }

    private void prepararCreacion() {
        lenient().when(solicitudRepository.findByIdSolicitudForUpdate(SOLICITUD))
            .thenReturn(Optional.of(solicitud));
        lenient().when(presupuestoRepository.existsBySolicitudIdSolicitud(SOLICITUD))
            .thenReturn(false);
        lenient().when(servicioRepository.findByIdServicioForUpdate(SERVICIO))
            .thenReturn(Optional.of(servicio));
        lenient().when(colaSolicitudesService.evaluar(solicitud))
            .thenReturn(new ColaSolicitudesService.EvaluacionPresupuesto(
                true, 1, null));
        lenient().when(configuracionPresupuestoService.obtener())
            .thenReturn(configuracion);
        ConfiguracionComision comision = new ConfiguracionComision();
        comision.setPorcentajeServicio(new BigDecimal("10.00"));
        comision.setPorcentajeProducto(new BigDecimal("10.00"));
        lenient().when(comisionService.obtener()).thenReturn(comision);
        lenient().when(comisionService.calcularComision(any(), any()))
            .thenAnswer(inv -> {
                BigDecimal monto = inv.getArgument(0);
                BigDecimal porcentaje = inv.getArgument(1);
                return monto.multiply(porcentaje)
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            });
        lenient().when(arancelEstimadoService.estimar())
            .thenReturn(new ArancelEstimadoDTO(
                new BigDecimal("3.49"),
                new BigDecimal("21"),
                new BigDecimal("4.2229")));
        ConfiguracionPedido configuracionPedido = new ConfiguracionPedido();
        configuracionPedido.setMaxHitos(4);
        configuracionPedido.setPorcentajeMaxPrimerHito(new BigDecimal("50.00"));
        configuracionPedido.setMontoMinimoCuota(BigDecimal.ZERO);
        lenient().when(configuracionPedidoService.obtener())
            .thenReturn(configuracionPedido);
        lenient().when(monedaVistaService.monedaCobro()).thenReturn(moneda);
        lenient().when(monedaVistaService.factorCobro(
                eq(moneda), any(LocalDateTime.class)))
            .thenReturn(BigDecimal.ONE);
        lenient().when(monedaVistaService.preferidaDe(ARTISTA)).thenReturn(moneda);
        lenient().when(monedaVistaService.factorOrigenAMostrada(
                eq(moneda), eq(moneda), any(LocalDateTime.class)))
            .thenReturn(BigDecimal.ONE);
        lenient().when(monedaVistaService.factorCobroAMostrada(
                eq(moneda), any(LocalDateTime.class)))
            .thenReturn(BigDecimal.ONE);
        lenient().when(presupuestoRepository.save(any(Presupuesto.class)))
            .thenAnswer(inv -> {
                Presupuesto guardado = inv.getArgument(0);
                guardado.setIdPresupuesto(99L);
                return guardado;
            });
    }

    private Solicitud crearSolicitud(EstadoSolicitud estado) {
        Solicitud nueva = new Solicitud();
        nueva.setIdSolicitud(SOLICITUD);
        nueva.setServicio(servicio);
        nueva.setCliente(cliente);
        nueva.setArtista(artista);
        nueva.setEstado(estado);
        nueva.setCantidadDibujos(2);
        nueva.setPrecioBase(new BigDecimal("100.00"));
        nueva.setMoneda(moneda);
        nueva.setFechaCreacion(LocalDateTime.now().minusHours(1));
        nueva.agregarItem(crearItem(ITEM_1, "Dibujo 1", 0));
        nueva.agregarItem(crearItem(ITEM_2, "Dibujo 2", 1));
        return nueva;
    }

    private ItemSolicitud crearItem(Long id, String nombre, int orden) {
        ItemSolicitud item = new ItemSolicitud();
        item.setIdItemSolicitud(id);
        item.setNombre(nombre);
        item.setOrden(orden);
        item.setEstado("PENDIENTE");
        return item;
    }

    private void agregarDerecho(
            ItemSolicitud item,
            Long idDerechoTermino,
            String codigo,
            String nombre,
            String porcentaje) {
        DerechoTermino derecho = new DerechoTermino();
        derecho.setIdDerechoTermino(idDerechoTermino);
        derecho.setCodigo(codigo);
        derecho.setNombre(nombre);

        ItemSolicitudDerecho itemDerecho = new ItemSolicitudDerecho();
        itemDerecho.setDerecho(derecho);
        itemDerecho.setCodigo(codigo);
        itemDerecho.setNombre(nombre);
        itemDerecho.setPorcentajeExtra(new BigDecimal(porcentaje));
        itemDerecho.setOrden(item.getDerechos().size());
        item.agregarDerecho(itemDerecho);
    }

    private PresupuestoRequest request() {
        PresupuestoRequest request = new PresupuestoRequest();
        request.setTiempoEntregaValor(2);
        request.setTiempoEntregaUnidad(ETiempoEntregaUnidad.SEMANAS);
        request.setItems(new ArrayList<>(List.of(
            itemRequest(ITEM_1),
            itemRequest(ITEM_2))));
        return request;
    }

    private ItemPresupuestoRequest itemRequest(Long idItemSolicitud) {
        ItemPresupuestoRequest item = new ItemPresupuestoRequest();
        item.setIdItemSolicitud(idItemSolicitud);
        item.setRecargos(new ArrayList<>());
        return item;
    }

    private RecargoPresupuestoRequest recargo(
            String descripcion, TipoRecargo tipo, String valor) {
        RecargoPresupuestoRequest recargo = new RecargoPresupuestoRequest();
        recargo.setDescripcion(descripcion);
        recargo.setTipo(tipo);
        recargo.setValor(new BigDecimal(valor));
        return recargo;
    }

    private HitoPresupuestoRequest hito(String porcentaje) {
        HitoPresupuestoRequest hito = new HitoPresupuestoRequest();
        hito.setPorcentaje(new BigDecimal(porcentaje));
        return hito;
    }
}
