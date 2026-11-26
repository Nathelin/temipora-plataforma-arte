package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.PedidoConfigDTO;
import com.temipora.backend_springboot.modelo.PedidoConfigRequest;
import com.temipora.backend_springboot.repositorios.ConfiguracionPedidoRepository;

@ExtendWith(MockitoExtension.class)
class ConfiguracionPedidoServiceTest {

    @Mock
    private ConfiguracionPedidoRepository configuracionRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ConfiguracionPedidoService configuracionPedidoService;

    @Test
    void obtener_sinFila_creaLaConfiguracionPorDefecto() {
        when(configuracionRepository.findById(ConfiguracionPedido.ID_UNICO))
            .thenReturn(Optional.empty());
        when(configuracionRepository.save(any(ConfiguracionPedido.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionPedido config = configuracionPedidoService.obtener();

        assertThat(config.getIdConfiguracion())
            .isEqualTo(ConfiguracionPedido.ID_UNICO);
        assertThat(config.getDiasAprobacionAutomatica())
            .isEqualTo(ConfiguracionPedido.DIAS_APROBACION_POR_DEFECTO);
        assertThat(config.getDiasPagoCuota())
            .isEqualTo(ConfiguracionPedido.DIAS_PAGO_POR_DEFECTO);
        assertThat(config.getRevisionesMaximasPorHito())
            .isEqualTo(ConfiguracionPedido.REVISIONES_MAXIMAS_POR_DEFECTO);
        assertThat(config.getDiasReentrega())
            .isEqualTo(ConfiguracionPedido.DIAS_REENTREGA_POR_DEFECTO);
        assertThat(config.getMaxHitos())
            .isEqualTo(ConfiguracionPedido.MAX_HITOS_POR_DEFECTO);
        assertThat(config.getPorcentajeMaxPrimerHito())
            .isEqualByComparingTo(ConfiguracionPedido.PORCENTAJE_MAX_PRIMER_HITO_POR_DEFECTO);
        assertThat(config.getMontoMinimoCuota())
            .isEqualByComparingTo(ConfiguracionPedido.MONTO_MINIMO_CUOTA_POR_DEFECTO);
        verify(configuracionRepository).save(config);
    }

    @Test
    void obtener_conFila_devuelveLaExistente() {
        ConfiguracionPedido config = configuracion(4);
        when(configuracionRepository.findById(ConfiguracionPedido.ID_UNICO))
            .thenReturn(Optional.of(config));

        assertThat(configuracionPedidoService.obtener()).isSameAs(config);
    }

    @Test
    void obtenerEstado_devuelveLosValoresConfigurados() {
        when(configuracionRepository.findById(ConfiguracionPedido.ID_UNICO))
            .thenReturn(Optional.of(configuracion(3)));

        PedidoConfigDTO dto = configuracionPedidoService.obtenerEstado();

        assertThat(dto.getDiasAprobacionAutomatica()).isEqualTo(3);
        assertThat(dto.getDiasPagoCuota()).isEqualTo(10);
        assertThat(dto.getRevisionesMaximasPorHito()).isEqualTo(2);
        assertThat(dto.getDiasReentrega()).isEqualTo(7);
        assertThat(dto.getMaxHitos()).isEqualTo(3);
        assertThat(dto.getPorcentajeMaxPrimerHito())
            .isEqualByComparingTo("40.00");
        assertThat(dto.getMontoMinimoCuota())
            .isEqualByComparingTo("2.00");
    }

    @Test
    void actualizar_ok_redondeaYGuarda() {
        when(configuracionRepository.findById(ConfiguracionPedido.ID_UNICO))
            .thenReturn(Optional.of(configuracion(4)));
        when(configuracionRepository.save(any(ConfiguracionPedido.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        PedidoConfigRequest request = requestValido();
        request.setPorcentajeMaxPrimerHito(new BigDecimal("55.555"));
        request.setMontoMinimoCuota(new BigDecimal("10.555"));

        PedidoConfigDTO dto = configuracionPedidoService.actualizar(request);

        assertThat(dto.getDiasAprobacionAutomatica()).isEqualTo(5);
        assertThat(dto.getDiasPagoCuota()).isEqualTo(14);
        assertThat(dto.getRevisionesMaximasPorHito()).isEqualTo(3);
        assertThat(dto.getDiasReentrega()).isEqualTo(5);
        assertThat(dto.getMaxHitos()).isEqualTo(4);
        assertThat(dto.getPorcentajeMaxPrimerHito())
            .isEqualByComparingTo("55.56");
        assertThat(dto.getMontoMinimoCuota())
            .isEqualByComparingTo("10.56");
        assertThat(dto.getFechaActualizacion()).isNotNull();
    }

    @Test
    void actualizar_sinRequest_lanzaError() {
        assertThatThrownBy(() -> configuracionPedidoService.actualizar(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("obligatoria");
    }

    @Test
    void actualizar_rangosInvalidos_lanzaError() {
        PedidoConfigRequest sinAprobacion = requestValido();
        sinAprobacion.setDiasAprobacionAutomatica(0);
        assertThatThrownBy(() -> configuracionPedidoService.actualizar(sinAprobacion))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("aprobación automática");

        PedidoConfigRequest sinPago = requestValido();
        sinPago.setDiasPagoCuota(15);
        assertThatThrownBy(() -> configuracionPedidoService.actualizar(sinPago))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("plazo de pago");

        PedidoConfigRequest sinRevisiones = requestValido();
        sinRevisiones.setRevisionesMaximasPorHito(11);
        assertThatThrownBy(() -> configuracionPedidoService.actualizar(sinRevisiones))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("máximo de revisiones");

        PedidoConfigRequest sinReentrega = requestValido();
        sinReentrega.setDiasReentrega(0);
        assertThatThrownBy(() -> configuracionPedidoService.actualizar(sinReentrega))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("plazo de reentrega");

        PedidoConfigRequest sinHitos = requestValido();
        sinHitos.setMaxHitos(7);
        assertThatThrownBy(() -> configuracionPedidoService.actualizar(sinHitos))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("máximo de hitos");

        PedidoConfigRequest sinPorcentaje = requestValido();
        sinPorcentaje.setPorcentajeMaxPrimerHito(null);
        assertThatThrownBy(() -> configuracionPedidoService.actualizar(sinPorcentaje))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("porcentaje máximo del primer hito");

        PedidoConfigRequest porcentajeBajo = requestValido();
        porcentajeBajo.setPorcentajeMaxPrimerHito(new BigDecimal("0.50"));
        assertThatThrownBy(() -> configuracionPedidoService.actualizar(porcentajeBajo))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("entre 1 y 100");

        PedidoConfigRequest sinMonto = requestValido();
        sinMonto.setMontoMinimoCuota(null);
        assertThatThrownBy(() -> configuracionPedidoService.actualizar(sinMonto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("monto mínimo por cuota");

        PedidoConfigRequest montoNegativo = requestValido();
        montoNegativo.setMontoMinimoCuota(new BigDecimal("-1"));
        assertThatThrownBy(() -> configuracionPedidoService.actualizar(montoNegativo))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("negativo");
    }

    private ConfiguracionPedido configuracion(int maxHitos) {
        ConfiguracionPedido config = new ConfiguracionPedido();
        config.setIdConfiguracion(ConfiguracionPedido.ID_UNICO);
        config.setDiasAprobacionAutomatica(3);
        config.setDiasPagoCuota(10);
        config.setRevisionesMaximasPorHito(2);
        config.setDiasReentrega(7);
        config.setMaxHitos(maxHitos);
        config.setPorcentajeMaxPrimerHito(new BigDecimal("40.00"));
        config.setMontoMinimoCuota(new BigDecimal("2.00"));
        return config;
    }

    private PedidoConfigRequest requestValido() {
        PedidoConfigRequest request = new PedidoConfigRequest();
        request.setDiasAprobacionAutomatica(5);
        request.setDiasPagoCuota(14);
        request.setRevisionesMaximasPorHito(3);
        request.setDiasReentrega(5);
        request.setMaxHitos(4);
        request.setPorcentajeMaxPrimerHito(new BigDecimal("50.00"));
        request.setMontoMinimoCuota(new BigDecimal("10.00"));
        request.setDiasGraciaEntrega(3);
        request.setDiasSancionVentas(14);
        request.setDiasReapelacion(7);
        request.setDiasCierreTrasBloqueo(30);
        return request;
    }
}
