package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ComisionConfigDTO;
import com.temipora.backend_springboot.modelo.ComisionConfigRequest;
import com.temipora.backend_springboot.modelo.ConfiguracionComision;
import com.temipora.backend_springboot.repositorios.ConfiguracionComisionRepository;

@ExtendWith(MockitoExtension.class)
class ComisionServiceTest {

    @Mock
    private ConfiguracionComisionRepository configuracionRepository;

    @InjectMocks
    private ComisionService comisionService;

    @Test
    void calcularComision_redondeaADosDecimales() {
        BigDecimal comision = comisionService.calcularComision(
            new BigDecimal("100.00"), new BigDecimal("10.00"));

        assertThat(comision).isEqualByComparingTo("10.00");
    }

    @Test
    void calcularComision_conTercero_redondeaHalfUp() {
        BigDecimal comision = comisionService.calcularComision(
            new BigDecimal("10.00"), new BigDecimal("33.33"));

        assertThat(comision).isEqualByComparingTo("3.33");
    }

    @Test
    void actualizar_rechazaPorcentajeNegativo() {
        ComisionConfigRequest request = request("-1.00", "10.00");

        assertThatThrownBy(() -> comisionService.actualizar(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("negativa");
    }

    @Test
    void actualizar_rechazaPorcentajeMayorAlMaximo() {
        ComisionConfigRequest request = request("10.00", "50.01");

        assertThatThrownBy(() -> comisionService.actualizar(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("50%");
    }

    @Test
    void actualizar_guardaAmbosPorcentajes() {
        ConfiguracionComision config = new ConfiguracionComision();
        when(configuracionRepository.findById(ConfiguracionComision.ID_UNICO))
            .thenReturn(Optional.of(config));
        when(configuracionRepository.save(any(ConfiguracionComision.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ComisionConfigDTO dto = comisionService.actualizar(request("12.50", "8.75"));

        assertThat(dto.getPorcentajeServicio()).isEqualByComparingTo("12.50");
        assertThat(dto.getPorcentajeProducto()).isEqualByComparingTo("8.75");
        assertThat(dto.getFechaActualizacion()).isNotNull();
    }

    private ComisionConfigRequest request(String servicio, String producto) {
        ComisionConfigRequest request = new ComisionConfigRequest();
        request.setPorcentajeServicio(new BigDecimal(servicio));
        request.setPorcentajeProducto(new BigDecimal(producto));
        return request;
    }
}
