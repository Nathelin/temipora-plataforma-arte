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

import com.temipora.backend_springboot.modelo.ConfiguracionResenia;
import com.temipora.backend_springboot.modelo.ConfiguracionReseniaDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionReseniaRequest;
import com.temipora.backend_springboot.repositorios.ConfiguracionReseniaRepository;

@ExtendWith(MockitoExtension.class)
class ConfiguracionReseniaServiceTest {

    @Mock
    private ConfiguracionReseniaRepository configuracionRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ConfiguracionReseniaService configuracionReseniaService;

    @Test
    void obtener_sinFila_creaDefaults() {
        when(configuracionRepository.findById(ConfiguracionResenia.ID_UNICO))
            .thenReturn(Optional.empty());
        when(configuracionRepository.save(any(ConfiguracionResenia.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionResenia config = configuracionReseniaService.obtener();

        assertThat(config.getDiasVentanaCreacion()).isEqualTo(30);
        assertThat(config.getDiasEdicion()).isEqualTo(3);
        assertThat(config.getMinimoReseniasEtiqueta()).isEqualTo(3);
        assertThat(config.getUmbralExcelente()).isEqualByComparingTo("4.50");
        assertThat(config.getUmbralBuena()).isEqualByComparingTo("3.50");
    }

    @Test
    void actualizar_ok_normalizaUmbrales() {
        ConfiguracionResenia existente = configuracion();
        when(configuracionRepository.findById(ConfiguracionResenia.ID_UNICO))
            .thenReturn(Optional.of(existente));
        when(configuracionRepository.save(any(ConfiguracionResenia.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionReseniaDTO resultado = configuracionReseniaService
            .actualizar(request(15, 5, 4, "4.8", "3.2"));

        assertThat(resultado.getDiasVentanaCreacion()).isEqualTo(15);
        assertThat(resultado.getDiasEdicion()).isEqualTo(5);
        assertThat(resultado.getMinimoReseniasEtiqueta()).isEqualTo(4);
        assertThat(resultado.getUmbralExcelente()).isEqualByComparingTo("4.80");
        assertThat(resultado.getUmbralBuena()).isEqualByComparingTo("3.20");
        assertThat(resultado.getFechaActualizacion()).isNotNull();
    }

    @Test
    void actualizar_excelenteMenorQueBuena_lanzaError() {
        assertThatThrownBy(() -> configuracionReseniaService
                .actualizar(request(30, 3, 3, "3.00", "4.00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mayor que");
    }

    @Test
    void actualizar_diasEdicionFueraDeRango_lanzaError() {
        assertThatThrownBy(() -> configuracionReseniaService
                .actualizar(request(30, 0, 3, "4.50", "3.50")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ventana de edición");
    }

    @Test
    void actualizar_umbralFueraDeRango_lanzaError() {
        assertThatThrownBy(() -> configuracionReseniaService
                .actualizar(request(30, 3, 3, "6.00", "3.50")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("entre 1.00 y 5.00");
    }

    private ConfiguracionResenia configuracion() {
        ConfiguracionResenia config = new ConfiguracionResenia();
        config.setIdConfiguracion(ConfiguracionResenia.ID_UNICO);
        return config;
    }

    private ConfiguracionReseniaRequest request(
            Integer ventana, Integer edicion, Integer minimo,
            String excelente, String buena) {
        ConfiguracionReseniaRequest request = new ConfiguracionReseniaRequest();
        request.setDiasVentanaCreacion(ventana);
        request.setDiasEdicion(edicion);
        request.setMinimoReseniasEtiqueta(minimo);
        request.setUmbralExcelente(new BigDecimal(excelente));
        request.setUmbralBuena(new BigDecimal(buena));
        return request;
    }
}
