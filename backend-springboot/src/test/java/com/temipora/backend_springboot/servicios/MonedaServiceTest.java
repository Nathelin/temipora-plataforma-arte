package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.MonedaDTO;
import com.temipora.backend_springboot.modelo.MonedaRequest;
import com.temipora.backend_springboot.repositorios.MonedaRepository;

@ExtendWith(MockitoExtension.class)
class MonedaServiceTest {

    @Mock
    private MonedaRepository monedaRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private MonedaService service;

    @Test
    void listar_mapeaTodas() {
        when(monedaRepository.findAllByOrderByCodigoAsc())
            .thenReturn(List.of(moneda(1L, "ARS", true), moneda(2L, "USD", false)));

        List<MonedaDTO> resultado = service.listar();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getCodigo()).isEqualTo("ARS");
        assertThat(resultado.get(0).isPredeterminada()).isTrue();
    }

    @Test
    void listarActivas_mapeaSoloActivas() {
        when(monedaRepository.findByEstadoTrueOrderByCodigoAsc())
            .thenReturn(List.of(moneda(1L, "ARS", true)));

        List<MonedaDTO> resultado = service.listarActivas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).isActivo()).isTrue();
    }

    @Test
    void predeterminada_usaLaMarcadaComoPredeterminada() {
        Moneda ars = moneda(1L, "ARS", true);
        when(monedaRepository.findFirstByPredeterminadaTrueAndEstadoTrue()).thenReturn(Optional.of(ars));

        assertThat(service.predeterminada()).isSameAs(ars);
    }

    @Test
    void predeterminada_sinMarcada_caeEnLaPrimeraActiva() {
        Moneda ars = moneda(1L, "ARS", false);
        when(monedaRepository.findFirstByPredeterminadaTrueAndEstadoTrue()).thenReturn(Optional.empty());
        when(monedaRepository.findFirstByEstadoTrueOrderByIdMonedaAsc()).thenReturn(Optional.of(ars));

        assertThat(service.predeterminada()).isSameAs(ars);
    }

    @Test
    void predeterminada_sinActivas_caeEnLaPrimera() {
        Moneda ars = moneda(1L, "ARS", false);
        when(monedaRepository.findFirstByPredeterminadaTrueAndEstadoTrue()).thenReturn(Optional.empty());
        when(monedaRepository.findFirstByEstadoTrueOrderByIdMonedaAsc()).thenReturn(Optional.empty());
        when(monedaRepository.findFirstByOrderByIdMonedaAsc()).thenReturn(Optional.of(ars));

        assertThat(service.predeterminada()).isSameAs(ars);
    }

    @Test
    void predeterminada_sinMonedas_devuelveNull() {
        when(monedaRepository.findFirstByPredeterminadaTrueAndEstadoTrue()).thenReturn(Optional.empty());
        when(monedaRepository.findFirstByEstadoTrueOrderByIdMonedaAsc()).thenReturn(Optional.empty());
        when(monedaRepository.findFirstByOrderByIdMonedaAsc()).thenReturn(Optional.empty());

        assertThat(service.predeterminada()).isNull();
    }

    @Test
    void obtener_inexistente_lanzaNoSuchElement() {
        when(monedaRepository.findByIdMoneda(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(9L))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Moneda no encontrada: 9");
    }

    @Test
    void obtenerPorCodigo_nulo_devuelveNull() {
        assertThat(service.obtenerPorCodigo(null)).isNull();
        verify(monedaRepository, never()).findByCodigoIgnoreCase(any());
    }

    @Test
    void obtenerPorCodigo_existente() {
        Moneda usd = moneda(2L, "USD", false);
        when(monedaRepository.findByCodigoIgnoreCase("USD")).thenReturn(Optional.of(usd));

        assertThat(service.obtenerPorCodigo("USD")).isSameAs(usd);
    }

    @Test
    void crear_ok_normalizaElCodigo() {
        when(monedaRepository.existsByCodigoIgnoreCase("EUR")).thenReturn(false);
        when(monedaRepository.save(any(Moneda.class))).thenAnswer(inv -> inv.getArgument(0));

        MonedaDTO dto = service.crear(request("eur", "Euro", "€", false));

        assertThat(dto.getCodigo()).isEqualTo("EUR");
        assertThat(dto.getNombre()).isEqualTo("Euro");
        assertThat(dto.isActivo()).isTrue();
    }

    @Test
    void crear_codigoDuplicado_lanzaError() {
        when(monedaRepository.existsByCodigoIgnoreCase("ARS")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(request("ars", "Peso", "$", false)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Ya existe una moneda con ese código.");
        verify(monedaRepository, never()).save(any());
    }

    @Test
    void modificar_codigoDeOtraMoneda_lanzaError() {
        when(monedaRepository.findByIdMoneda(1L)).thenReturn(Optional.of(moneda(1L, "ARS", true)));
        when(monedaRepository.existsByCodigoIgnoreCaseAndIdMonedaNot("USD", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.modificar(1L, request("usd", "Dólar", "US$", false)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void modificar_ok() {
        Moneda ars = moneda(1L, "ARS", true);
        when(monedaRepository.findByIdMoneda(1L)).thenReturn(Optional.of(ars));
        when(monedaRepository.existsByCodigoIgnoreCaseAndIdMonedaNot("ARS", 1L)).thenReturn(false);
        when(monedaRepository.save(any(Moneda.class))).thenAnswer(inv -> inv.getArgument(0));

        MonedaDTO dto = service.modificar(1L, request("ars", "Peso argentino", "$", false));

        assertThat(dto.getNombre()).isEqualTo("Peso argentino");
    }

    @Test
    void marcarPredeterminada_dejaSoloUna() {
        Moneda ars = moneda(1L, "ARS", false);
        Moneda usd = moneda(2L, "USD", true);
        when(monedaRepository.findByIdMoneda(1L)).thenReturn(Optional.of(ars));
        when(monedaRepository.findAll()).thenReturn(List.of(ars, usd));

        MonedaDTO dto = service.marcarPredeterminada(1L);

        assertThat(dto.isPredeterminada()).isTrue();
        assertThat(ars.isPredeterminada()).isTrue();
        assertThat(usd.isPredeterminada()).isFalse();
        verify(monedaRepository).saveAll(List.of(ars, usd));
    }

    @Test
    void marcarPredeterminada_dadaDeBaja_lanzaError() {
        Moneda inactiva = moneda(3L, "BRL", false);
        inactiva.setEstado(false);
        when(monedaRepository.findByIdMoneda(3L)).thenReturn(Optional.of(inactiva));

        assertThatThrownBy(() -> service.marcarPredeterminada(3L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void darDeBaja_laPredeterminada_lanzaError() {
        when(monedaRepository.findByIdMoneda(1L)).thenReturn(Optional.of(moneda(1L, "ARS", true)));

        assertThatThrownBy(() -> service.darDeBaja(1L))
            .isInstanceOf(IllegalArgumentException.class);
        verify(monedaRepository, never()).save(any());
    }

    @Test
    void darDeBaja_ultimaActiva_lanzaError() {
        when(monedaRepository.findByIdMoneda(2L)).thenReturn(Optional.of(moneda(2L, "USD", false)));
        when(monedaRepository.countByEstadoTrue()).thenReturn(1L);

        assertThatThrownBy(() -> service.darDeBaja(2L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Debe quedar al menos una moneda activa.");
    }

    @Test
    void darDeBaja_ok() {
        Moneda usd = moneda(2L, "USD", false);
        when(monedaRepository.findByIdMoneda(2L)).thenReturn(Optional.of(usd));
        when(monedaRepository.countByEstadoTrue()).thenReturn(2L);
        when(monedaRepository.save(any(Moneda.class))).thenAnswer(inv -> inv.getArgument(0));

        MonedaDTO dto = service.darDeBaja(2L);

        assertThat(dto.isActivo()).isFalse();
    }

    @Test
    void darDeAlta_ok() {
        Moneda usd = moneda(2L, "USD", false);
        usd.setEstado(false);
        when(monedaRepository.findByIdMoneda(2L)).thenReturn(Optional.of(usd));
        when(monedaRepository.save(any(Moneda.class))).thenAnswer(inv -> inv.getArgument(0));

        MonedaDTO dto = service.darDeAlta(2L);

        assertThat(dto.isActivo()).isTrue();
    }

    private Moneda moneda(Long id, String codigo, boolean predeterminada) {
        return Moneda.builder()
            .idMoneda(id)
            .codigo(codigo)
            .nombre(codigo)
            .predeterminada(predeterminada)
            .estado(true)
            .build();
    }

    private MonedaRequest request(String codigo, String nombre, String simbolo, boolean predeterminada) {
        MonedaRequest r = new MonedaRequest();
        r.setCodigo(codigo);
        r.setNombre(nombre);
        r.setSimbolo(simbolo);
        r.setPredeterminada(predeterminada);
        return r;
    }
}
