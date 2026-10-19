package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ConversionMoneda;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.repositorios.ConversionMonedaRepository;

@ExtendWith(MockitoExtension.class)
class ConversionMonedaServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 1, 1, 12, 0);
    private static final Moneda ARS = moneda(1L, "ARS");
    private static final Moneda USD = moneda(2L, "USD");

    @Mock
    private ConversionMonedaRepository repository;

    @InjectMocks
    private ConversionMonedaService service;

    @Test
    void factor_mismaMoneda_esUno() {
        assertThat(service.factor(ARS, ARS, AHORA)).isEqualByComparingTo("1");
        verify(repository, never()).vigente(any(), any(), any());
    }

    @Test
    void factor_nulo_devuelveNull() {
        assertThat(service.factor(null, ARS, AHORA)).isNull();
        assertThat(service.factor(ARS, null, AHORA)).isNull();
    }

    @Test
    void factor_directo() {
        when(repository.vigente(USD, ARS, AHORA))
            .thenReturn(List.of(fila(USD, ARS, "1000")));

        assertThat(service.factor(USD, ARS, AHORA)).isEqualByComparingTo("1000");
    }

    @Test
    void factor_inverso() {
        when(repository.vigente(ARS, USD, AHORA)).thenReturn(List.of());
        when(repository.vigente(USD, ARS, AHORA))
            .thenReturn(List.of(fila(USD, ARS, "1000")));

        assertThat(service.factor(ARS, USD, AHORA)).isEqualByComparingTo("0.001");
    }

    @Test
    void factor_sinDatos_devuelveNull() {
        when(repository.vigente(ARS, USD, AHORA)).thenReturn(List.of());
        when(repository.vigente(USD, ARS, AHORA)).thenReturn(List.of());

        assertThat(service.factor(ARS, USD, AHORA)).isNull();
    }

    @Test
    void convertir_aplicaFactor() {
        when(repository.vigente(USD, ARS, AHORA))
            .thenReturn(List.of(fila(USD, ARS, "1000")));

        assertThat(service.convertir(new BigDecimal("2.5"), USD, ARS, AHORA))
            .isEqualByComparingTo("2500.00");
    }

    @Test
    void convertir_montoNulo_devuelveNull() {
        assertThat(service.convertir(null, USD, ARS, AHORA)).isNull();
    }

    @Test
    void factorAbierto_devuelvePrimeroONull() {
        when(repository.abierta(USD, ARS)).thenReturn(List.of());
        assertThat(service.factorAbierto(USD, ARS)).isNull();

        ConversionMoneda abierta = fila(USD, ARS, "1000");
        when(repository.abierta(USD, ARS)).thenReturn(List.of(abierta));
        assertThat(service.factorAbierto(USD, ARS)).isSameAs(abierta);
    }

    @Test
    void registrarFactor_mismasMonedas_lanzaError() {
        assertThatThrownBy(() -> service.registrarFactor(ARS, ARS, BigDecimal.ONE, "API", AHORA))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registrarFactor_factorInvalido_lanzaError() {
        assertThatThrownBy(() -> service.registrarFactor(USD, ARS, BigDecimal.ZERO, "API", AHORA))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registrarFactor_factorIgualAlAbierto_noGuarda() {
        ConversionMoneda abierta = fila(USD, ARS, "1000");
        when(repository.abierta(USD, ARS)).thenReturn(List.of(abierta));

        ConversionMoneda resultado = service.registrarFactor(
            USD, ARS, new BigDecimal("1000"), "API", AHORA);

        assertThat(resultado).isSameAs(abierta);
        verify(repository, never()).save(any());
    }

    @Test
    void registrarFactor_cierraAbiertoYGuardaNuevo() {
        ConversionMoneda abierta = fila(USD, ARS, "1000");
        when(repository.abierta(USD, ARS)).thenReturn(List.of(abierta));
        when(repository.save(any(ConversionMoneda.class))).thenAnswer(inv -> inv.getArgument(0));

        ConversionMoneda resultado = service.registrarFactor(
            USD, ARS, new BigDecimal("1200"), "API", AHORA);

        assertThat(abierta.getFinValidez()).isEqualTo(AHORA);
        assertThat(resultado.getFactor()).isEqualByComparingTo("1200");
        assertThat(resultado.getOrigen()).isEqualTo("API");
        assertThat(resultado.getInicioValidez()).isEqualTo(AHORA);
        assertThat(resultado.getFinValidez()).isNull();
    }

    private static Moneda moneda(Long id, String codigo) {
        return Moneda.builder().idMoneda(id).codigo(codigo).nombre(codigo).build();
    }

    private ConversionMoneda fila(Moneda entrada, Moneda salida, String factor) {
        ConversionMoneda c = new ConversionMoneda();
        c.setMonedaEntrada(entrada);
        c.setMonedaSalida(salida);
        c.setFactor(new BigDecimal(factor));
        c.setInicioValidez(AHORA.minusDays(1));
        return c;
    }
}
