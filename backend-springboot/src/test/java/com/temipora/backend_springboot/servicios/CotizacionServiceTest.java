package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ConfiguracionCotizacion;
import com.temipora.backend_springboot.modelo.ConversionMoneda;
import com.temipora.backend_springboot.modelo.CotizacionConfigDTO;
import com.temipora.backend_springboot.modelo.CotizacionConfigRequest;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.repositorios.ConfiguracionCotizacionRepository;

@ExtendWith(MockitoExtension.class)
class CotizacionServiceTest {

    private static final Moneda ARS = moneda(1L, "ARS");
    private static final Moneda USD = moneda(2L, "USD");

    @Mock
    private ConfiguracionCotizacionRepository configuracionRepository;

    @Mock
    private ConversionMonedaService conversionMonedaService;

    @Mock
    private MonedaService monedaService;

    @Mock
    private CotizacionApiClient apiClient;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private CotizacionService service;

    @Test
    void obtenerEstado_creaConfiguracionPorDefectoSiNoExiste() {
        when(configuracionRepository.findById(ConfiguracionCotizacion.ID_UNICO)).thenReturn(Optional.empty());
        when(configuracionRepository.save(any(ConfiguracionCotizacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(monedaService.predeterminada()).thenReturn(ARS);
        when(monedaService.obtenerPorCodigo("USD")).thenReturn(USD);
        when(conversionMonedaService.factorAbierto(USD, ARS)).thenReturn(null);

        CotizacionConfigDTO dto = service.obtenerEstado();

        assertThat(dto.isApiHabilitada()).isTrue();
        assertThat(dto.getMonedaOrigen()).isEqualTo("USD");
        assertThat(dto.getMonedaDestino()).isEqualTo("ARS");
        assertThat(dto.getFactorVigente()).isNull();
        verify(configuracionRepository).save(any(ConfiguracionCotizacion.class));
    }

    @Test
    void obtenerEstado_incluyeFactorVigente() {
        ConfiguracionCotizacion config = configuracion();
        when(configuracionRepository.findById(ConfiguracionCotizacion.ID_UNICO)).thenReturn(Optional.of(config));

        ConversionMoneda vigente = new ConversionMoneda();
        vigente.setFactor(new BigDecimal("1500.00000000"));
        vigente.setOrigen("API");
        when(conversionMonedaService.factorAbierto(USD, ARS)).thenReturn(vigente);

        CotizacionConfigDTO dto = service.obtenerEstado();

        assertThat(dto.getFactorVigente()).isEqualByComparingTo("1500");
        assertThat(dto.getOrigenFactor()).isEqualTo("API");
    }

    @Test
    void actualizar_monedasIguales_lanzaError() {
        when(monedaService.obtener(1L)).thenReturn(ARS);
        CotizacionConfigRequest request = request(1L, 1L, null);

        assertThatThrownBy(() -> service.actualizar(request))
            .isInstanceOf(IllegalArgumentException.class);
        verify(configuracionRepository, never()).save(any());
    }

    @Test
    void actualizar_conFactorManual_registraFactorManual() {
        ConfiguracionCotizacion config = configuracion();
        when(monedaService.obtener(2L)).thenReturn(USD);
        when(monedaService.obtener(1L)).thenReturn(ARS);
        when(configuracionRepository.findById(ConfiguracionCotizacion.ID_UNICO)).thenReturn(Optional.of(config));
        when(configuracionRepository.save(any(ConfiguracionCotizacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conversionMonedaService.factorAbierto(USD, ARS)).thenReturn(null);

        CotizacionConfigDTO dto = service.actualizar(request(2L, 1L, new BigDecimal("1234.5")));

        ArgumentCaptor<BigDecimal> factor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(conversionMonedaService).registrarFactor(
            eq(USD), eq(ARS), factor.capture(), eq("MANUAL"), any());
        assertThat(factor.getValue()).isEqualByComparingTo("1234.5");
        assertThat(dto.getFactorManual()).isEqualByComparingTo("1234.5");
        assertThat(config.getFechaActualizacion()).isNotNull();
    }

    @Test
    void actualizar_sinFactorManual_noRegistra() {
        ConfiguracionCotizacion config = configuracion();
        when(monedaService.obtener(2L)).thenReturn(USD);
        when(monedaService.obtener(1L)).thenReturn(ARS);
        when(configuracionRepository.findById(ConfiguracionCotizacion.ID_UNICO)).thenReturn(Optional.of(config));
        when(configuracionRepository.save(any(ConfiguracionCotizacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conversionMonedaService.factorAbierto(USD, ARS)).thenReturn(null);

        service.actualizar(request(2L, 1L, null));

        verify(conversionMonedaService, never()).registrarFactor(any(), any(), any(), any(), any());
    }

    @Test
    void refrescarDesdeApi_apiDeshabilitada_lanzaError() {
        ConfiguracionCotizacion config = configuracion();
        config.setApiHabilitada(false);
        when(configuracionRepository.findById(ConfiguracionCotizacion.ID_UNICO)).thenReturn(Optional.of(config));

        assertThatThrownBy(() -> service.refrescarDesdeApi())
            .isInstanceOf(IllegalStateException.class);
        verify(apiClient, never()).obtenerFactorVenta();
    }

    @Test
    void refrescarDesdeApi_registraFactorDeLaApi() {
        ConfiguracionCotizacion config = configuracion();
        when(configuracionRepository.findById(ConfiguracionCotizacion.ID_UNICO)).thenReturn(Optional.of(config));
        when(configuracionRepository.save(any(ConfiguracionCotizacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(apiClient.obtenerFactorVenta()).thenReturn(new BigDecimal("1500.00"));
        when(conversionMonedaService.factorAbierto(USD, ARS)).thenReturn(null);

        CotizacionConfigDTO dto = service.refrescarDesdeApi();

        verify(conversionMonedaService).registrarFactor(
            eq(USD), eq(ARS), eq(new BigDecimal("1500.00")), eq("API"), any());
        assertThat(config.getFechaActualizacion()).isNotNull();
        assertThat(dto.getMonedaOrigen()).isEqualTo("USD");
    }

    @Test
    void consultarFactorApi_delegaEnElCliente() {
        when(apiClient.obtenerFactorVenta()).thenReturn(new BigDecimal("999"));

        assertThat(service.consultarFactorApi()).isEqualByComparingTo("999");
    }

    private static Moneda moneda(Long id, String codigo) {
        return Moneda.builder().idMoneda(id).codigo(codigo).nombre(codigo).build();
    }

    private ConfiguracionCotizacion configuracion() {
        ConfiguracionCotizacion config = new ConfiguracionCotizacion();
        config.setIdConfiguracion(ConfiguracionCotizacion.ID_UNICO);
        config.setMonedaOrigen(USD);
        config.setMonedaDestino(ARS);
        return config;
    }

    private CotizacionConfigRequest request(Long idOrigen, Long idDestino, BigDecimal factorManual) {
        CotizacionConfigRequest request = new CotizacionConfigRequest();
        request.setApiHabilitada(true);
        request.setIdMonedaOrigen(idOrigen);
        request.setIdMonedaDestino(idDestino);
        request.setFactorManual(factorManual);
        return request;
    }
}
