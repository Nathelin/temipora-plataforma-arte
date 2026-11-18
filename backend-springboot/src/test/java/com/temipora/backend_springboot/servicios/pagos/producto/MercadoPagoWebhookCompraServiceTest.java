package com.temipora.backend_springboot.servicios.pagos.producto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.IntentoCobro;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.repositorios.IntentoCobroRepository;
import com.temipora.backend_springboot.servicios.CompraPagoService;
import com.temipora.backend_springboot.servicios.pagos.EstadoCobroProveedor;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoClient;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoException;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoOAuthService;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoPago;

@ExtendWith(MockitoExtension.class)
class MercadoPagoWebhookCompraServiceTest {

    @Mock private MercadoPagoClient cliente;
    @Mock private MercadoPagoOAuthService oauthService;
    @Mock private IntentoCobroRepository intentoCobroRepository;
    @Mock private CompraPagoService compraPagoService;

    @InjectMocks private MercadoPagoWebhookCompraService service;

    @Test
    void procesarPago_aprobadoAplicaEstadoAlIntento() {
        IntentoCobro intento = intento(5L, "COMPRA-1", "100.00");
        when(cliente.consultarPago("123")).thenReturn(pago("COMPRA-1", "100.00"));
        when(intentoCobroRepository.findByExternalReference("COMPRA-1"))
            .thenReturn(Optional.of(intento));

        service.procesarPago("123");

        ArgumentCaptor<EstadoCobroProveedor> captor =
            ArgumentCaptor.forClass(EstadoCobroProveedor.class);
        verify(compraPagoService).aplicarEstadoProveedor(eq(5L), captor.capture());
        assertThat(captor.getValue().estado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(captor.getValue().idPagoExterno()).isEqualTo("123");
    }

    @Test
    void procesarPago_referenciaAjena_noAplicaNada() {
        when(cliente.consultarPago("123")).thenReturn(pago("PEDIDO-1", "100.00"));

        service.procesarPago("123");

        verify(compraPagoService, never())
            .aplicarEstadoProveedor(any(), any());
    }

    @Test
    void procesarPago_montoDistinto_lanzaError() {
        when(cliente.consultarPago("123")).thenReturn(pago("COMPRA-1", "90.00"));
        when(intentoCobroRepository.findByExternalReference("COMPRA-1"))
            .thenReturn(Optional.of(intento(5L, "COMPRA-1", "100.00")));

        assertThatThrownBy(() -> service.procesarPago("123"))
            .isInstanceOf(MercadoPagoException.class)
            .hasMessageContaining("monto");
    }

    private IntentoCobro intento(Long id, String referencia, String monto) {
        Moneda moneda = new Moneda();
        moneda.setCodigo("ARS");
        IntentoCobro intento = new IntentoCobro();
        intento.setIdIntentoCobro(id);
        intento.setExternalReference(referencia);
        intento.setMonto(new BigDecimal(monto));
        intento.setMoneda(moneda);
        return intento;
    }

    private MercadoPagoPago pago(String referencia, String monto) {
        return new MercadoPagoPago(
            123L,
            "approved",
            "accredited",
            new BigDecimal(monto),
            "ARS",
            referencia,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    }
}
