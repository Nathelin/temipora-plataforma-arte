package com.temipora.backend_springboot.servicios;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Huella SHA-256 de un texto en hexadecimal. Acredita el texto exacto de los
 * términos que un cliente aceptó, sin necesidad de guardar la huella.
 */
public final class HuellaTexto {

    private HuellaTexto() {
    }

    public static String sha256(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                digest.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible.", e);
        }
    }
}
