package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Pago;
import com.temipora.backend_springboot.repositorios.PagoRepository;
import com.temipora.backend_springboot.servicios.PagoService;

@ExtendWith(MockitoExtension.class)
class MercadoPagoWebhookServiceTest {

    @Mock
    private MercadoPagoClient cliente;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private PagoService pagoService;

    @InjectMocks
    private MercadoPagoWebhookService webhookService;

    @Test
    void procesarPago_aprobado_aplicaElEstado() {
        Pago pago = pago();
        when(cliente.consultarPago("123")).thenReturn(pagoMp("approved", "50.00"));
        when(pagoRepository.findById(30L)).thenReturn(Optional.of(pago));

        webhookService.procesarPago("123");

        verify(pagoService).aplicarEstadoProveedor(
            30L, EstadoPago.APROBADO, "123", "approved", "accredited");
    }

    @Test
    void procesarPago_conMontoDistinto_lanza() {
        Pago pago = pago();
        when(cliente.consultarPago("123")).thenReturn(pagoMp("approved", "49.00"));
        when(pagoRepository.findById(30L)).thenReturn(Optional.of(pago));

        assertThatThrownBy(() -> webhookService.procesarPago("123"))
            .isInstanceOf(MercadoPagoException.class)
            .hasMessageContaining("monto");

        verify(pagoService, never()).aplicarEstadoProveedor(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    }

    @Test
    void procesarPago_sinExternalReference_ignora() {
        when(cliente.consultarPago("123")).thenReturn(
            new MercadoPagoPago(
                123L, "approved", "accredited",
                new BigDecimal("50.00"), "ARS", null, "visa",
                "credit_card", null));

        webhookService.procesarPago("123");

        verify(pagoService, never()).aplicarEstadoProveedor(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    }

    @Test
    void mapearEstado_traduceLosEstadosDeMercadoPago() {
        assertThat(MercadoPagoEstados.mapear("approved"))
            .isEqualTo(EstadoPago.APROBADO);
        assertThat(MercadoPagoEstados.mapear("rejected"))
            .isEqualTo(EstadoPago.RECHAZADO);
        assertThat(MercadoPagoEstados.mapear("cancelled"))
            .isEqualTo(EstadoPago.CANCELADO);
        assertThat(MercadoPagoEstados.mapear("refunded"))
            .isEqualTo(EstadoPago.REEMBOLSADO);
        assertThat(MercadoPagoEstados.mapear("pending"))
            .isEqualTo(EstadoPago.PENDIENTE);
        assertThat(MercadoPagoEstados.mapear(null))
            .isEqualTo(EstadoPago.PENDIENTE);
    }

    private MercadoPagoPago pagoMp(String estado, String monto) {
        return new MercadoPagoPago(
            123L, estado, "accredited",
            new BigDecimal(monto), "ARS", "30", "visa",
            "credit_card", null);
    }

    private Pago pago() {
        Moneda ars = Moneda.builder()
            .idMoneda(3L)
            .codigo("ARS")
            .nombre("Peso argentino")
            .build();
        Pago pago = new Pago();
        pago.setIdPago(30L);
        pago.setMontoCobro(new BigDecimal("50.00"));
        pago.setMonedaCobro(ars);
        return pago;
    }
}
