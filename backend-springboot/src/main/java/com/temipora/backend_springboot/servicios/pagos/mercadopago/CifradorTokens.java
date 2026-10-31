package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CifradorTokens {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final String claveBase64;
    private final SecureRandom random = new SecureRandom();

    public CifradorTokens(@Value("${pagos.token.key:}") String claveBase64) {
        this.claveBase64 = claveBase64;
    }

    public String cifrar(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, clave(),
                new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(
                valor.getBytes(StandardCharsets.UTF_8));
            byte[] salida = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, salida, 0, iv.length);
            System.arraycopy(cifrado, 0, salida, iv.length, cifrado.length);
            return Base64.getEncoder().encodeToString(salida);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el token.", e);
        }
    }

    public String descifrar(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            byte[] datos = Base64.getDecoder().decode(valor);
            if (datos.length <= IV_BYTES) {
                throw new IllegalStateException("Token cifrado inválido.");
            }
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(datos, 0, iv, 0, IV_BYTES);
            byte[] cifrado = new byte[datos.length - IV_BYTES];
            System.arraycopy(datos, IV_BYTES, cifrado, 0, cifrado.length);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, clave(),
                new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cifrado),
                StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar el token.", e);
        }
    }

    private SecretKeySpec clave() {
        if (claveBase64 == null || claveBase64.isBlank()) {
            throw new IllegalStateException(
                "Falta configurar PAGOS_TOKEN_KEY.");
        }
        byte[] bytes = Base64.getDecoder().decode(claveBase64);
        if (bytes.length != 32) {
            throw new IllegalStateException(
                "PAGOS_TOKEN_KEY debe tener 32 bytes codificados en Base64.");
        }
        return new SecretKeySpec(bytes, "AES");
    }
}
