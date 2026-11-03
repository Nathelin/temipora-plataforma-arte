package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ConfiguracionPresupuesto;
import com.temipora.backend_springboot.modelo.PresupuestoConfigDTO;
import com.temipora.backend_springboot.modelo.PresupuestoConfigRequest;
import com.temipora.backend_springboot.repositorios.ConfiguracionPresupuestoRepository;

@ExtendWith(MockitoExtension.class)
class ConfiguracionPresupuestoServiceTest {

    @Mock
    private ConfiguracionPresupuestoRepository configuracionRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ConfiguracionPresupuestoService configuracionPresupuestoService;

    @Test
    void obtener_sinFila_creaLaConfiguracionPorDefecto() {
        when(configuracionRepository.findById(ConfiguracionPresupuesto.ID_UNICO))
            .thenReturn(Optional.empty());
        when(configuracionRepository.save(any(ConfiguracionPresupuesto.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionPresupuesto config = configuracionPresupuestoService.obtener();

        assertThat(config.getIdConfiguracion())
            .isEqualTo(ConfiguracionPresupuesto.ID_UNICO);
        assertThat(config.getVigenciaHoras()).isEqualTo(72);
        verify(configuracionRepository).save(config);
    }

    @Test
    void obtener_conFila_devuelveLaExistente() {
        ConfiguracionPresupuesto config = configuracion(24);
        when(configuracionRepository.findById(ConfiguracionPresupuesto.ID_UNICO))
            .thenReturn(Optional.of(config));

        assertThat(configuracionPresupuestoService.obtener()).isSameAs(config);
    }

    @Test
    void obtenerEstado_devuelveLaVigencia() {
        ConfiguracionPresupuesto config = configuracion(24);
        when(configuracionRepository.findById(ConfiguracionPresupuesto.ID_UNICO))
            .thenReturn(Optional.of(config));

        PresupuestoConfigDTO dto = configuracionPresupuestoService.obtenerEstado();

        assertThat(dto.getVigenciaHoras()).isEqualTo(24);
    }

    @Test
    void actualizar_cambiaLaVigencia() {
        ConfiguracionPresupuesto config = configuracion(72);
        when(configuracionRepository.findById(ConfiguracionPresupuesto.ID_UNICO))
            .thenReturn(Optional.of(config));
        when(configuracionRepository.save(any(ConfiguracionPresupuesto.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        PresupuestoConfigRequest request = new PresupuestoConfigRequest();
        request.setVigenciaHoras(48);
        PresupuestoConfigDTO dto = configuracionPresupuestoService.actualizar(request);

        assertThat(dto.getVigenciaHoras()).isEqualTo(48);
        assertThat(config.getVigenciaHoras()).isEqualTo(48);
        assertThat(config.getFechaActualizacion()).isNotNull();
    }

    @Test
    void actualizar_sinRequest_lanzaError() {
        assertThatThrownBy(() -> configuracionPresupuestoService.actualizar(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("vigencia");
    }

    @Test
    void actualizar_fueraDeRango_lanzaError() {
        PresupuestoConfigRequest request = new PresupuestoConfigRequest();
        request.setVigenciaHoras(0);

        assertThatThrownBy(() -> configuracionPresupuestoService.actualizar(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("entre 1 y 8760");
    }

    private ConfiguracionPresupuesto configuracion(int horas) {
        ConfiguracionPresupuesto config = new ConfiguracionPresupuesto();
        config.setIdConfiguracion(ConfiguracionPresupuesto.ID_UNICO);
        config.setVigenciaHoras(horas);
        return config;
    }
}
