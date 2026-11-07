package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ConfiguracionSustitucion;
import com.temipora.backend_springboot.modelo.ConfiguracionSustitucionDTO;
import com.temipora.backend_springboot.modelo.PesoPuntajeDTO;
import com.temipora.backend_springboot.repositorios.ConfiguracionSustitucionRepository;

@ExtendWith(MockitoExtension.class)
class ConfiguracionSustitucionServiceTest {

    @Mock
    private ConfiguracionSustitucionRepository repository;

    @Mock
    private AuditoriaService auditoria;

    private ConfiguracionSustitucionService service;

    @BeforeEach
    void setUp() {
        service = new ConfiguracionSustitucionService(repository, auditoria);
    }

    @Test
    void obtener_sinRegistro_creaElUnico() {
        when(repository.findById(ConfiguracionSustitucion.ID_UNICO))
            .thenReturn(Optional.empty());
        when(repository.save(any(ConfiguracionSustitucion.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionSustitucion config = service.obtener();

        assertThat(config.getIdConfiguracion())
            .isEqualTo(ConfiguracionSustitucion.ID_UNICO);
        verify(repository).save(config);
    }

    @Test
    void obtener_existente_loDevuelveSinGuardar() {
        ConfiguracionSustitucion config = new ConfiguracionSustitucion();
        when(repository.findById(ConfiguracionSustitucion.ID_UNICO))
            .thenReturn(Optional.of(config));

        assertThat(service.obtener()).isSameAs(config);
        verify(repository, never()).save(any());
    }

    @Test
    void obtenerDto_incluyeLaDistribucionBase() {
        when(repository.findById(ConfiguracionSustitucion.ID_UNICO))
            .thenReturn(Optional.of(new ConfiguracionSustitucion()));

        ConfiguracionSustitucionDTO dto = service.obtenerDto();

        assertThat(dto.getDistribucionBase())
            .extracting(PesoPuntajeDTO::clave)
            .containsExactly(
                "etiquetas", "tipo", "precio", "categorias", "medio", "rating");
        assertThat(dto.getDistribucionBase())
            .extracting(PesoPuntajeDTO::porcentaje)
            .containsExactly(
                new BigDecimal("35.0"),
                new BigDecimal("30.0"),
                new BigDecimal("25.0"),
                new BigDecimal("5.0"),
                new BigDecimal("3.0"),
                new BigDecimal("2.0"));
        assertThat(dto.getBonusFavorito()).isEqualByComparingTo("0.100");
    }

    @Test
    void actualizar_aplicaLimitesYAudita() {
        ConfiguracionSustitucion config = new ConfiguracionSustitucion();
        when(repository.findById(ConfiguracionSustitucion.ID_UNICO))
            .thenReturn(Optional.of(config));
        when(repository.save(any(ConfiguracionSustitucion.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionSustitucionDTO cambios = dtoValido();
        cambios.setHabilitado(false);
        cambios.setMaxAlternativas(99);
        cambios.setMaxPorArtista(0);
        cambios.setMinutosDedupe(9999);
        cambios.setDiasRetencion(-5);
        cambios.setEmailCierreMinutos(9999);
        cambios.setEmailCierreMax(99);
        cambios.setEmailCierreVigenciaHoras(9999);
        cambios.setMostrarEnExplorar(false);
        cambios.setEmailCierreHabilitado(false);

        ConfiguracionSustitucionDTO dto = service.actualizar(cambios);

        assertThat(config.isHabilitado()).isFalse();
        assertThat(config.getMaxAlternativas()).isEqualTo(20);
        assertThat(config.getMaxPorArtista()).isEqualTo(1);
        assertThat(config.getMinutosDedupe()).isEqualTo(1440);
        assertThat(config.getDiasRetencion()).isZero();
        assertThat(config.getEmailCierreMinutos()).isEqualTo(1440);
        assertThat(config.getEmailCierreMax()).isEqualTo(20);
        assertThat(config.getEmailCierreVigenciaHoras()).isEqualTo(720);
        assertThat(config.isMostrarEnExplorar()).isFalse();
        assertThat(config.isEmailCierreHabilitado()).isFalse();
        assertThat(config.getFechaActualizacion()).isNotNull();
        assertThat(dto.getDistribucionBase()).hasSize(6);
        verify(auditoria).registrar(
            anyString(), anyString(), any(), anyString(), any(), any(), anyList());
    }

    @Test
    void actualizar_rechazaConfiguracionNula() {
        assertThatThrownBy(() -> service.actualizar(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("obligatoria");
    }

    @Test
    void actualizar_rechazaValoresDeSimilitudNulos() {
        ConfiguracionSustitucionDTO cambios = dtoValido();
        cambios.setBonusFavorito(null);

        assertThatThrownBy(() -> service.actualizar(cambios))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("similitud");
    }

    @Test
    void actualizar_rechazaRangoDePrecioInvalido() {
        ConfiguracionSustitucionDTO cambios = dtoValido();
        cambios.setRangoPrecioPct(new BigDecimal("200.01"));

        assertThatThrownBy(() -> service.actualizar(cambios))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("diferencia de precio");
    }

    @Test
    void actualizar_rechazaBonusFueraDeRango() {
        ConfiguracionSustitucionDTO cambios = dtoValido();
        cambios.setBonusFavorito(new BigDecimal("0.301"));

        assertThatThrownBy(() -> service.actualizar(cambios))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("30%");
    }

    @Test
    void actualizar_rechazaFactorDeTiempoFueraDeRango() {
        ConfiguracionSustitucionDTO cambios = dtoValido();
        cambios.setFactorTiempoMax(new BigDecimal("10.01"));

        assertThatThrownBy(() -> service.actualizar(cambios))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tiempo de entrega");
    }

    private ConfiguracionSustitucionDTO dtoValido() {
        return ConfiguracionSustitucionDTO.builder()
            .habilitado(true)
            .maxAlternativas(6)
            .maxPorArtista(1)
            .rangoPrecioPct(new BigDecimal("40.00"))
            .bonusFavorito(new BigDecimal("0.100"))
            .factorTiempoMax(new BigDecimal("3.00"))
            .mostrarEnExplorar(true)
            .minutosDedupe(30)
            .diasRetencion(180)
            .emailCierreHabilitado(true)
            .emailCierreMinutos(10)
            .emailCierreMax(5)
            .emailCierreVigenciaHoras(72)
            .build();
    }
}
