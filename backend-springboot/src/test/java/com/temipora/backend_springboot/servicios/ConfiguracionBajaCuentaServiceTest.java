package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ConfiguracionBajaCuenta;
import com.temipora.backend_springboot.modelo.ConfiguracionBajaCuentaDTO;
import com.temipora.backend_springboot.repositorios.ConfiguracionBajaCuentaRepository;

@ExtendWith(MockitoExtension.class)
class ConfiguracionBajaCuentaServiceTest {

    @Mock
    private ConfiguracionBajaCuentaRepository configuracionRepository;

    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private ConfiguracionBajaCuentaService configuracionService;

    @Test
    void diasParaReactivar_sinFila_usaElValorPorDefecto() {
        when(configuracionRepository.findById(ConfiguracionBajaCuenta.ID_UNICO))
            .thenReturn(Optional.empty());

        assertThat(configuracionService.diasParaReactivar())
            .isEqualTo(ConfiguracionBajaCuenta.DIAS_POR_DEFECTO);
    }

    @Test
    void diasParaReactivar_conFila_usaElValorPersistido() {
        ConfiguracionBajaCuenta config = configuracion(45);
        when(configuracionRepository.findById(ConfiguracionBajaCuenta.ID_UNICO))
            .thenReturn(Optional.of(config));

        assertThat(configuracionService.diasParaReactivar()).isEqualTo(45);
    }

    @Test
    void obtener_sinFila_creaLaConfiguracionConElValorPorDefecto() {
        when(configuracionRepository.findById(ConfiguracionBajaCuenta.ID_UNICO))
            .thenReturn(Optional.empty());
        when(configuracionRepository.save(any(ConfiguracionBajaCuenta.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionBajaCuenta config = configuracionService.obtener();

        assertThat(config.getIdConfiguracion()).isEqualTo(ConfiguracionBajaCuenta.ID_UNICO);
        assertThat(config.getDiasParaReactivar())
            .isEqualTo(ConfiguracionBajaCuenta.DIAS_POR_DEFECTO);
        verify(configuracionRepository).save(config);
    }

    @Test
    void actualizar_valorValido_guardaYAudita() {
        ConfiguracionBajaCuenta config = configuracion(30);
        when(configuracionRepository.findById(ConfiguracionBajaCuenta.ID_UNICO))
            .thenReturn(Optional.of(config));
        when(configuracionRepository.save(config)).thenReturn(config);

        ConfiguracionBajaCuentaDTO resultado = configuracionService.actualizar(
            new ConfiguracionBajaCuentaDTO(45, null));

        assertThat(resultado.getDiasParaReactivar()).isEqualTo(45);
        assertThat(config.getDiasParaReactivar()).isEqualTo(45);
        assertThat(config.getFechaActualizacion()).isNotNull();
        verify(auditoria).registrar(
            eq(AccionesAdmin.BAJA_CUENTA_CONFIG_ACTUALIZAR),
            eq("ConfiguracionBajaCuenta"),
            eq(ConfiguracionBajaCuenta.ID_UNICO),
            anyString(), any(), eq(config), anyList());
    }

    @Test
    void actualizar_valorFueraDeRango_lanzaErrorYNoGuarda() {
        assertThatThrownBy(() -> configuracionService.actualizar(
            new ConfiguracionBajaCuentaDTO(0, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("El plazo para reactivar debe estar entre 1 y 365 días.");

        assertThatThrownBy(() -> configuracionService.actualizar(
            new ConfiguracionBajaCuentaDTO(366, null)))
            .isInstanceOf(IllegalArgumentException.class);

        verify(configuracionRepository, never()).save(any(ConfiguracionBajaCuenta.class));
    }

    private ConfiguracionBajaCuenta configuracion(int dias) {
        ConfiguracionBajaCuenta config = new ConfiguracionBajaCuenta();
        config.setIdConfiguracion(ConfiguracionBajaCuenta.ID_UNICO);
        config.setDiasParaReactivar(dias);
        return config;
    }
}
