package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.temipora.backend_springboot.modelo.ConciliacionResultadoDTO;
import com.temipora.backend_springboot.modelo.EstadoIntentoCobro;
import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.IntentoCobro;
import com.temipora.backend_springboot.modelo.Pago;
import com.temipora.backend_springboot.repositorios.IntentoCobroRepository;
import com.temipora.backend_springboot.repositorios.PagoRepository;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoClient;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoException;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoOAuthService;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoPago;

class ConciliacionMercadoPagoServiceTest {

    private final PagoRepository pagoRepository = mock(PagoRepository.class);
    private final IntentoCobroRepository intentoCobroRepository =
        mock(IntentoCobroRepository.class);
    private final MercadoPagoClient cliente = mock(MercadoPagoClient.class);
    private final MercadoPagoOAuthService oauthService =
        mock(MercadoPagoOAuthService.class);
    private final ConciliacionPersistencia persistencia =
        mock(ConciliacionPersistencia.class);

    private final ConciliacionMercadoPagoService servicio =
        new ConciliacionMercadoPagoService(pagoRepository,
            intentoCobroRepository, cliente, oauthService, persistencia);

    @Test
    void completaPagosEIntentosYCuentaLosQueNoSePuedenLeer() {
        Pago pago = pago(1L, "MP-1");
        IntentoCobro intento = intento(9L, "MP-9");
        IntentoCobro sinDatos = intento(10L, "MP-10");

        when(pagoRepository.idsPendientesDeConciliar(EstadoPago.APROBADO))
            .thenReturn(List.of(1L, 2L));
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago));
        when(pagoRepository.findById(2L))
            .thenReturn(Optional.of(pago(2L, "MP-2")));

        when(intentoCobroRepository.idsPendientesDeConciliar(
            EstadoIntentoCobro.APROBADO)).thenReturn(List.of(9L, 10L));
        when(intentoCobroRepository.findById(9L))
            .thenReturn(Optional.of(intento));
        when(intentoCobroRepository.findById(10L))
            .thenReturn(Optional.of(sinDatos));

        when(cliente.consultarPago("MP-1")).thenReturn(pagoMp(
            new BigDecimal("128.25"), new BigDecimal("15.75")));
        when(cliente.consultarPago("MP-2"))
            .thenThrow(new MercadoPagoException("sin acceso"));
        when(oauthService.tokensVigentes()).thenReturn(List.of("token-artista"));
        when(cliente.consultarPago("MP-2", "token-artista"))
            .thenThrow(new MercadoPagoException("sin acceso"));
        when(cliente.consultarPago("MP-9")).thenReturn(pagoMp(
            new BigDecimal("171.00"), new BigDecimal("9.00")));
        when(cliente.consultarPago("MP-10")).thenReturn(pagoMp(null, null));

        when(persistencia.completarPago(eq(1L), any())).thenReturn(true);
        when(persistencia.completarIntento(eq(9L), any())).thenReturn(true);

        ConciliacionResultadoDTO resultado = servicio.conciliar();

        assertThat(resultado.revisados()).isEqualTo(4);
        assertThat(resultado.completados()).isEqualTo(2);
        assertThat(resultado.sinDatos()).isEqualTo(1);
        assertThat(resultado.errores()).isEqualTo(1);
    }

    @Test
    void noConsultaMercadoPagoSiLaOperacionNoTieneIdExterno() {
        Pago pago = pago(1L, null);
        when(pagoRepository.idsPendientesDeConciliar(EstadoPago.APROBADO))
            .thenReturn(List.of(1L));
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago));
        when(intentoCobroRepository.idsPendientesDeConciliar(
            EstadoIntentoCobro.APROBADO)).thenReturn(List.of());

        ConciliacionResultadoDTO resultado = servicio.conciliar();

        assertThat(resultado.revisados()).isEqualTo(1);
        assertThat(resultado.sinDatos()).isEqualTo(1);
        assertThat(resultado.completados()).isZero();
        assertThat(resultado.errores()).isZero();
    }

    private Pago pago(Long id, String idExterno) {
        Pago pago = new Pago();
        pago.setIdPago(id);
        pago.setIdPagoExterno(idExterno);
        return pago;
    }

    private IntentoCobro intento(Long id, String idExterno) {
        IntentoCobro intento = new IntentoCobro();
        intento.setIdIntentoCobro(id);
        intento.setIdPagoExterno(idExterno);
        return intento;
    }

    private MercadoPagoPago pagoMp(BigDecimal neto, BigDecimal arancel) {
        return new MercadoPagoPago(
            1L, "approved", "accredited", new BigDecimal("150.00"), "ARS",
            "1", "visa", "credit_card", null, null, null,
            new MercadoPagoPago.TransactionDetails(
                neto, new BigDecimal("150.00")),
            arancel == null ? List.of()
                : List.of(new MercadoPagoPago.FeeDetail(
                    "mercadopago_fee", arancel, "collector")),
            null, null);
    }
}
