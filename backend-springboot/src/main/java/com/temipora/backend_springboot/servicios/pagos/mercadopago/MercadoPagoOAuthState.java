package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * State firmado para el flujo OAuth. Evita CSRF sin guardar sesión en el
 * servidor: incluye el id de usuario y una marca de tiempo firmados con HMAC.
 */
@Component
public class MercadoPagoOAuthState {

    private static final long VIGENCIA_MS = 10 * 60 * 1000L;

    private final byte[] secreto;

    public MercadoPagoOAuthState(@Value("${jwt.secret:}") String secreto) {
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
    }

    public String generar(Long idUsuario) {
        long marca = System.currentTimeMillis();
        String base = idUsuario + ":" + marca;
        return base + ":" + hmac(base);
    }

    public Long validar(String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("Falta el state de OAuth.");
        }
        String[] partes = state.split(":");
        if (partes.length != 3) {
            throw new IllegalArgumentException("State de OAuth inválido.");
        }
        String base = partes[0] + ":" + partes[1];
        if (!MessageDigest.isEqual(
                hmac(base).getBytes(StandardCharsets.UTF_8),
                partes[2].getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Firma del state inválida.");
        }
        long marca;
        try {
            marca = Long.parseLong(partes[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("State de OAuth inválido.", e);
        }
        if (System.currentTimeMillis() - marca > VIGENCIA_MS) {
            throw new IllegalArgumentException("El state de OAuth expiró.");
        }
        try {
            return Long.valueOf(partes[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("State de OAuth inválido.", e);
        }
    }

    private String hmac(String base) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            return HexFormat.of().formatHex(
                mac.doFinal(base.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                "No se pudo firmar el state de OAuth.", e);
        }
    }
}
