package com.temipora.backend_springboot.servicios.opentimestamps;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Atestacion de un sello OpenTimestamps: el tag de 8 bytes mas su payload.
 * Nos interesan la atestacion pendiente (con la URI del calendario) y la de
 * bloque de Bitcoin (con la altura).
 */
final class OtsAttestation {

    static final byte[] PENDING = hex("83dfe30d2ef90c8e");
    static final byte[] BITCOIN = hex("0588960d73d71901");
    static final byte[] LITECOIN = hex("06869a0d73d71b45");

    private final byte[] tag;
    private final byte[] payload;

    OtsAttestation(byte[] tag, byte[] payload) {
        this.tag = tag;
        this.payload = payload;
    }

    byte[] tag() {
        return tag;
    }

    byte[] payload() {
        return payload;
    }

    boolean esPendiente() {
        return java.util.Arrays.equals(tag, PENDING);
    }

    boolean esBitcoin() {
        return java.util.Arrays.equals(tag, BITCOIN);
    }

    /** URI del calendario (payload = varbytes de la URI). */
    String uri() {
        Cursor c = new Cursor(payload);
        byte[] uri = c.readVarBytes();
        return new String(uri, StandardCharsets.UTF_8);
    }

    /** Altura de bloque de Bitcoin (payload = varuint). */
    long altura() {
        Cursor c = new Cursor(payload);
        return c.readVarUint();
    }

    byte[] clave() {
        byte[] c = new byte[tag.length + payload.length];
        System.arraycopy(tag, 0, c, 0, tag.length);
        System.arraycopy(payload, 0, c, tag.length, payload.length);
        return c;
    }

    private static byte[] hex(String valor) {
        return HexFormat.of().parseHex(valor);
    }

    /** Cursor minimo sobre bytes, compartido por el parser del sello. */
    static final class Cursor {

        private final byte[] datos;
        private int pos;

        Cursor(byte[] datos) {
            this.datos = datos;
        }

        int restante() {
            return datos.length - pos;
        }

        int readByte() {
            if (pos >= datos.length) {
                throw new OtsException("Sello OpenTimestamps truncado.");
            }
            return datos[pos++] & 0xff;
        }

        byte[] readBytes(int n) {
            if (n < 0 || pos + n > datos.length) {
                throw new OtsException("Sello OpenTimestamps truncado.");
            }
            byte[] r = new byte[n];
            System.arraycopy(datos, pos, r, 0, n);
            pos += n;
            return r;
        }

        long readVarUint() {
            long valor = 0;
            int shift = 0;
            for (int i = 0; i < 9; i++) {
                int b = readByte();
                valor |= (long) (b & 0x7f) << shift;
                if ((b & 0x80) == 0) {
                    return valor;
                }
                shift += 7;
            }
            throw new OtsException("varuint demasiado largo en el sello.");
        }

        byte[] readVarBytes() {
            long largo = readVarUint();
            if (largo < 0 || largo > 1_000_000) {
                throw new OtsException("varbytes invalido en el sello: " + largo);
            }
            return readBytes((int) largo);
        }
    }
}
