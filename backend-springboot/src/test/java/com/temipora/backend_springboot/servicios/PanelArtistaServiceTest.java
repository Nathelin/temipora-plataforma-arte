package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ContadorMensajesDTO;
import com.temipora.backend_springboot.modelo.CuentaCobroDTO;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.PanelArtistaResumenDTO;
import com.temipora.backend_springboot.modelo.SolicitudResumenDTO;
import com.temipora.backend_springboot.repositorios.CompraGrupoRepository;
import com.temipora.backend_springboot.repositorios.PagoRepository;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoOAuthService;

@ExtendWith(MockitoExtension.class)
class PanelArtistaServiceTest {

    private static final Long ARTISTA = 2L;

    @Mock
    private SolicitudServicioService solicitudServicioService;
    @Mock
    private PedidoService pedidoService;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private CompraGrupoRepository compraGrupoRepository;
    @Mock
    private ConversacionService conversacionService;
    @Mock
    private MercadoPagoOAuthService mercadoPagoOAuthService;

    @InjectMocks
    private PanelArtistaService panelArtistaService;

    @Test
    void sinDatosDevuelveTodoEnCero() {
        when(solicitudServicioService.listarRecibidasEnEstados(
            eq(ARTISTA), any()))
            .thenReturn(List.of());
        sinPedidosNiCobros();

        PanelArtistaResumenDTO resumen = panelArtistaService.resumen(ARTISTA);

        assertThat(resumen.getMetricas().getSolicitudesPorResponder()).isZero();
        assertThat(resumen.getMetricas().getSolicitudesEnEspera()).isZero();
        assertThat(resumen.getPedidosActivos()).isEmpty();
        assertThat(resumen.getHitosAtencion()).isEmpty();
        assertThat(resumen.getSolicitudesRecientes()).isEmpty();
        assertThat(resumen.getDinero().getPorCobrar())
            .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resumen.getDinero().getCobradoTotal())
            .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resumen.isCuentaCobrosConectada()).isFalse();
    }

    @Test
    void separaPorResponderDeListaDeEsperaYPresupuestosPorVencer() {
        LocalDateTime ahora = LocalDateTime.now();
        SolicitudResumenDTO nueva = solicitud(
            2L, EstadoSolicitud.PENDIENTE, ahora.minusDays(1));
        SolicitudResumenDTO vieja = solicitud(
            1L, EstadoSolicitud.PENDIENTE, ahora.minusDays(4));
        SolicitudResumenDTO enEspera = solicitud(
            3L, EstadoSolicitud.EN_LISTA_ESPERA, ahora.minusDays(2));
        SolicitudResumenDTO porVencer = solicitud(
            4L, EstadoSolicitud.PRESUPUESTADA, ahora.minusDays(1));
        porVencer.setFechaVencimiento(ahora.plusHours(10));
        SolicitudResumenDTO lejano = solicitud(
            5L, EstadoSolicitud.PRESUPUESTADA, ahora.minusDays(1));
        lejano.setFechaVencimiento(ahora.plusHours(72));

        when(solicitudServicioService.listarRecibidasEnEstados(
            eq(ARTISTA), any()))
            .thenReturn(List.of(
                nueva, vieja, enEspera, porVencer, lejano));
        sinPedidosNiCobros();

        PanelArtistaResumenDTO resumen = panelArtistaService.resumen(ARTISTA);

        assertThat(resumen.getMetricas().getSolicitudesPorResponder())
            .isEqualTo(2);
        assertThat(resumen.getMetricas().getSolicitudesEnEspera())
            .isEqualTo(1);
        assertThat(resumen.getMetricas().getPresupuestosPorVencer())
            .isEqualTo(1);
        assertThat(resumen.getSolicitudesPorResponder())
            .extracting(SolicitudResumenDTO::getIdSolicitud)
            .containsExactly(1L, 2L);
        assertThat(resumen.getPresupuestosPorVencer())
            .extracting(SolicitudResumenDTO::getIdSolicitud)
            .containsExactly(4L);
    }

    @Test
    void limitaLasSolicitudesPorResponderACinco() {
        LocalDateTime ahora = LocalDateTime.now();
        List<SolicitudResumenDTO> pendientes = List.of(
            solicitud(1L, EstadoSolicitud.PENDIENTE, ahora.minusDays(6)),
            solicitud(2L, EstadoSolicitud.PENDIENTE, ahora.minusDays(5)),
            solicitud(3L, EstadoSolicitud.PENDIENTE, ahora.minusDays(4)),
            solicitud(4L, EstadoSolicitud.PENDIENTE, ahora.minusDays(3)),
            solicitud(5L, EstadoSolicitud.PENDIENTE, ahora.minusDays(2)),
            solicitud(6L, EstadoSolicitud.PENDIENTE, ahora.minusDays(1)));

        when(solicitudServicioService.listarRecibidasEnEstados(
            eq(ARTISTA), any()))
            .thenReturn(pendientes);
        sinPedidosNiCobros();

        PanelArtistaResumenDTO resumen = panelArtistaService.resumen(ARTISTA);

        assertThat(resumen.getMetricas().getSolicitudesPorResponder())
            .isEqualTo(6);
        assertThat(resumen.getSolicitudesPorResponder()).hasSize(5);
        assertThat(resumen.getSolicitudesRecientes()).hasSize(5);
    }

    @Test
    void sumaElNetoRealSeparandoElMesCorriente() {
        when(solicitudServicioService.listarRecibidasEnEstados(
            eq(ARTISTA), any()))
            .thenReturn(List.of());
        when(pedidoService.actividadArtista(ARTISTA)).thenReturn(
            new PedidoService.ActividadArtista(
                List.of(),
                List.of(),
                1,
                0,
                0,
                0,
                0,
                0,
                new BigDecimal("250.00"),
                "ARS"));
        when(pagoRepository.netosRealesDeArtista(ARTISTA))
            .thenReturn(List.<Object[]>of(
                new Object[] {
                    new BigDecimal("300.00"), LocalDateTime.now(),
                },
                new Object[] {
                    new BigDecimal("100.00"),
                    LocalDateTime.now().minusMonths(2),
                }));
        when(compraGrupoRepository.netosRealesDeArtista(ARTISTA))
            .thenReturn(List.of());
        when(conversacionService.contador(ARTISTA))
            .thenReturn(new ContadorMensajesDTO(0, 0));
        when(mercadoPagoOAuthService.estado(ARTISTA))
            .thenReturn(new CuentaCobroDTO(true, null));

        PanelArtistaResumenDTO resumen = panelArtistaService.resumen(ARTISTA);

        assertThat(resumen.getDinero().getCobradoTotal())
            .isEqualByComparingTo("400.00");
        assertThat(resumen.getDinero().getCobradoMes())
            .isEqualByComparingTo("300.00");
        assertThat(resumen.getDinero().getPorCobrar())
            .isEqualByComparingTo("250.00");
        assertThat(resumen.getDinero().getMonedaMostrada()).isEqualTo("ARS");
        assertThat(resumen.isCuentaCobrosConectada()).isTrue();
    }

    private void sinPedidosNiCobros() {
        when(pedidoService.actividadArtista(ARTISTA)).thenReturn(
            new PedidoService.ActividadArtista(
                List.of(),
                List.of(),
                0,
                0,
                0,
                0,
                0,
                0,
                BigDecimal.ZERO,
                "ARS"));
        when(pagoRepository.netosRealesDeArtista(ARTISTA))
            .thenReturn(List.of());
        when(compraGrupoRepository.netosRealesDeArtista(ARTISTA))
            .thenReturn(List.of());
        when(conversacionService.contador(ARTISTA))
            .thenReturn(new ContadorMensajesDTO(0, 0));
        when(mercadoPagoOAuthService.estado(ARTISTA))
            .thenReturn(new CuentaCobroDTO(false, null));
    }

    private SolicitudResumenDTO solicitud(
            Long id, EstadoSolicitud estado, LocalDateTime creada) {
        return SolicitudResumenDTO.builder()
            .idSolicitud(id)
            .estado(estado)
            .fechaCreacion(creada)
            .nombreServicio("Retrato")
            .build();
    }
}
