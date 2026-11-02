package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.temipora.backend_springboot.modelo.ConfiguracionComision;
import com.temipora.backend_springboot.modelo.ConfiguracionInactividad;
import com.temipora.backend_springboot.modelo.ConfiguracionInactividadDTO;
import com.temipora.backend_springboot.repositorios.ConfiguracionInactividadRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfiguracionInactividadServiceTest {

    @Mock
    private ConfiguracionInactividadRepository repository;

    @Mock
    private ComisionService comisionService;

    @InjectMocks
    private ConfiguracionInactividadService service;

    @BeforeEach
    void setUp() {
        ConfiguracionComision comision = new ConfiguracionComision();
        comision.setPorcentajeServicio(new BigDecimal("10.00"));
        when(comisionService.obtener()).thenReturn(comision);
        when(repository.save(any(ConfiguracionInactividad.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void obtener_creaDefaultSiNoExiste() {
        when(repository.findById(ConfiguracionInactividad.ID_UNICO))
            .thenReturn(Optional.empty());

        ConfiguracionInactividad config = service.obtener();

        assertThat(config.getIdConfiguracion())
            .isEqualTo(ConfiguracionInactividad.ID_UNICO);
        assertThat(config.getPorcentajeDescuento())
            .isEqualByComparingTo("10.00");
    }

    @Test
    void actualizar_aceptaDescuentoIgualALaComision() {
        when(repository.findById(ConfiguracionInactividad.ID_UNICO))
            .thenReturn(Optional.of(new ConfiguracionInactividad()));

        ConfiguracionInactividadDTO resultado = service.actualizar(dto("10.00"));

        assertThat(resultado.getPorcentajeDescuento())
            .isEqualByComparingTo("10.00");
    }

    @Test
    void actualizar_rechazaDescuentoMayorALaComision() {
        when(repository.findById(ConfiguracionInactividad.ID_UNICO))
            .thenReturn(Optional.of(new ConfiguracionInactividad()));

        assertThatThrownBy(() -> service.actualizar(dto("20.00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("comisión vigente");

        verify(repository, never()).save(any(ConfiguracionInactividad.class));
    }

    private ConfiguracionInactividadDTO dto(String porcentaje) {
        return ConfiguracionInactividadDTO.builder()
            .diasSinCompra(60)
            .diasSinConexion(30)
            .diasCooldown(30)
            .diasMinimosCuenta(7)
            .porcentajeDescuento(new BigDecimal(porcentaje))
            .vigenciaHoras(72)
            .maxOfertasPorUsuario(3)
            .maxUsuariosPorCorrida(50)
            .build();
    }
}
