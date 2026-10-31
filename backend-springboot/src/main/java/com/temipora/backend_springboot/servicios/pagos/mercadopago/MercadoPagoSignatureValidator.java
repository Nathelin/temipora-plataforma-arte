package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MercadoPagoSignatureValidator {

    private static final Logger log =
        LoggerFactory.getLogger(MercadoPagoSignatureValidator.class);

    private final String secreto;

    public MercadoPagoSignatureValidator(
            @Value("${pagos.mercadopago.webhook-secret:}") String secreto) {
        this.secreto = secreto;
        if (secreto == null || secreto.isBlank()) {
            log.warn("pagos.mercadopago.webhook-secret vacío: "
                + "no se validará la firma de los webhooks de MercadoPago.");
        }
    }

    public boolean validar(
            String dataId,
            String requestId,
            String encabezadoFirma) {
        if (secreto == null || secreto.isBlank()) {
            return true;
        }
        if (dataId == null || encabezadoFirma == null) {
            return false;
        }
        String ts = null;
        String v1 = null;
        for (String parte : encabezadoFirma.split(",")) {
            String[] par = parte.trim().split("=", 2);
            if (par.length != 2) {
                continue;
            }
            if ("ts".equals(par[0])) {
                ts = par[1];
            } else if ("v1".equals(par[0])) {
                v1 = par[1];
            }
        }
        if (ts == null || v1 == null) {
            return false;
        }
        String manifiesto = "id:" + dataId
            + ";request-id:" + (requestId == null ? "" : requestId)
            + ";ts:" + ts + ";";
        String esperado = hmac(manifiesto);
        return MessageDigest.isEqual(
            esperado.getBytes(StandardCharsets.UTF_8),
            v1.getBytes(StandardCharsets.UTF_8));
    }

    private String hmac(String manifiesto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(
                mac.doFinal(manifiesto.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                "No se pudo calcular la firma del webhook.", e);
        }
    }
}
