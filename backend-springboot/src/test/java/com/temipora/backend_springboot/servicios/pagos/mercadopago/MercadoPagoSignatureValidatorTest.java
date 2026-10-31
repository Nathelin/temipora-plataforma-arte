package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

class MercadoPagoSignatureValidatorTest {

    private static final String SECRETO = "secreto-de-prueba";

    @Test
    void validar_conFirmaCorrecta_devuelveTrue() throws Exception {
        String ts = "1700000000";
        String requestId = "req-1";
        String dataId = "123";
        String manifiesto = "id:" + dataId
            + ";request-id:" + requestId + ";ts:" + ts + ";";

        MercadoPagoSignatureValidator validador =
            new MercadoPagoSignatureValidator(SECRETO);

        assertThat(validador.validar(
            dataId, requestId, "ts=" + ts + ",v1=" + hmac(manifiesto)))
            .isTrue();
    }

    @Test
    void validar_conFirmaIncorrecta_devuelveFalse() {
        MercadoPagoSignatureValidator validador =
            new MercadoPagoSignatureValidator(SECRETO);

        assertThat(validador.validar("123", "req-1", "ts=1,v1=incorrecta"))
            .isFalse();
    }

    @Test
    void validar_sinEncabezado_devuelveFalse() {
        MercadoPagoSignatureValidator validador =
            new MercadoPagoSignatureValidator(SECRETO);

        assertThat(validador.validar("123", "req-1", null)).isFalse();
    }

    @Test
    void validar_sinSecretoConfigurado_permiteElEvento() {
        MercadoPagoSignatureValidator validador =
            new MercadoPagoSignatureValidator("");

        assertThat(validador.validar("123", "req-1", null)).isTrue();
    }

    private String hmac(String manifiesto) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
            SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(
            mac.doFinal(manifiesto.getBytes(StandardCharsets.UTF_8)));
    }
}
