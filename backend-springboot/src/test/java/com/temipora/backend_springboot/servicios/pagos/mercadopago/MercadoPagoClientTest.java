package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MercadoPagoClientTest {

    @Test
    void crearPreferencia_enviaCredencialesYDevuelveLaPreferencia() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();
        MercadoPagoClient cliente =
            new MercadoPagoClient(builder, "https://api.test", "token");

        server.expect(requestTo("https://api.test/checkout/preferences"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer token"))
            .andExpect(header("X-Idempotency-Key", "clave-1"))
            .andExpect(jsonPath("$.external_reference").value("7"))
            .andRespond(withSuccess(
                "{\"id\":\"pref-1\",\"init_point\":\"https://prod\","
                    + "\"sandbox_init_point\":\"https://sandbox\"}",
                MediaType.APPLICATION_JSON));

        MercadoPagoPreferencia preferencia = cliente.crearPreferencia(
            Map.of("external_reference", "7"), "clave-1");

        assertThat(preferencia.id()).isEqualTo("pref-1");
        assertThat(preferencia.initPoint()).isEqualTo("https://prod");
        assertThat(preferencia.sandboxInitPoint()).isEqualTo("https://sandbox");
        server.verify();
    }

    @Test
    void consultarPago_devuelveElPago() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();
        MercadoPagoClient cliente =
            new MercadoPagoClient(builder, "https://api.test", "token");

        server.expect(requestTo("https://api.test/v1/payments/123"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer token"))
            .andRespond(withSuccess(
                "{\"id\":123,\"status\":\"approved\","
                    + "\"transaction_amount\":50.00,\"currency_id\":\"ARS\","
                    + "\"external_reference\":\"7\"}",
                MediaType.APPLICATION_JSON));

        MercadoPagoPago pago = cliente.consultarPago("123");

        assertThat(pago.id()).isEqualTo(123L);
        assertThat(pago.status()).isEqualTo("approved");
        assertThat(pago.externalReference()).isEqualTo("7");
        server.verify();
    }

    @Test
    void sinAccessToken_lanza() {
        MercadoPagoClient cliente = new MercadoPagoClient(
            RestClient.builder(), "https://api.test", "");

        assertThatThrownBy(() -> cliente.consultarPago("123"))
            .isInstanceOf(MercadoPagoException.class)
            .hasMessageContaining("MP_ACCESS_TK");
    }
}
