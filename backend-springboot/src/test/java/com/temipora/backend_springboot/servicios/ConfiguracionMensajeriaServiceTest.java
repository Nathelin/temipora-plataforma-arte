package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ConfiguracionMensajeria;
import com.temipora.backend_springboot.modelo.ConfiguracionMensajeriaDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionMensajeriaRequest;
import com.temipora.backend_springboot.repositorios.ConfiguracionMensajeriaRepository;

@ExtendWith(MockitoExtension.class)
class ConfiguracionMensajeriaServiceTest {

    @Mock
    private ConfiguracionMensajeriaRepository configuracionRepository;

    @InjectMocks
    private ConfiguracionMensajeriaService configuracionService;

    @Test
    void obtener_sinFila_creaDefaults() {
        when(configuracionRepository.findById(ConfiguracionMensajeria.ID_UNICO))
            .thenReturn(Optional.empty());
        when(configuracionRepository.save(any(ConfiguracionMensajeria.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionMensajeria config = configuracionService.obtener();

        assertThat(config.getLongitudMaximaMensaje()).isEqualTo(2000);
        assertThat(config.getLongitudMaximaMotivo()).isEqualTo(200);
        assertThat(config.getHorasEsperaReenvio()).isEqualTo(168);
        assertThat(config.getMaxSolicitudesPendientes()).isEqualTo(5);
        assertThat(config.getMaxSolicitudesPorDia()).isEqualTo(10);
        assertThat(config.getMaxMensajesPorDia()).isEqualTo(100);
    }

    @Test
    void actualizar_ok_guardaLosValores() {
        when(configuracionRepository.findById(ConfiguracionMensajeria.ID_UNICO))
            .thenReturn(Optional.of(new ConfiguracionMensajeria()));
        when(configuracionRepository.save(any(ConfiguracionMensajeria.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionMensajeriaDTO resultado =
            configuracionService.actualizar(request());

        assertThat(resultado.getLongitudMaximaMensaje()).isEqualTo(500);
        assertThat(resultado.getHorasEsperaReenvio()).isZero();
        assertThat(resultado.getMaxSolicitudesPendientes()).isEqualTo(2);
        assertThat(resultado.getMaxMensajesPorDia()).isEqualTo(50);
        assertThat(resultado.getFechaActualizacion()).isNotNull();
    }

    @Test
    void actualizar_sinRequest_lanzaError() {
        assertThatThrownBy(() -> configuracionService.actualizar(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("obligatoria");
    }

    private ConfiguracionMensajeriaRequest request() {
        ConfiguracionMensajeriaRequest request = new ConfiguracionMensajeriaRequest();
        request.setLongitudMaximaMensaje(500);
        request.setLongitudMaximaMotivo(150);
        request.setHorasEsperaReenvio(0);
        request.setMaxSolicitudesPendientes(2);
        request.setMaxSolicitudesPorDia(4);
        request.setMaxMensajesPorDia(50);
        return request;
    }
}
