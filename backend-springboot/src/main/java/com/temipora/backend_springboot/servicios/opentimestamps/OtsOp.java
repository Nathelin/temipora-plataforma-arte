package com.temipora.backend_springboot.servicios.opentimestamps;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Operacion del arbol de un sello OpenTimestamps (una arista entre mensajes).
 * Implementa las operaciones criptograficas y de concatenacion del protocolo.
 * Tags tomados de python-opentimestamps (core/op.py).
 */
final class OtsOp {

    static final int APPEND = 0xf0;
    static final int PREPEND = 0xf1;
    static final int REVERSE = 0xf2;
    static final int HEXLIFY = 0xf3;
    static final int SHA1 = 0x02;
    static final int RIPEMD160 = 0x03;
    static final int SHA256 = 0x08;
    static final int KECCAK256 = 0x67;

    private final int tag;
    private final byte[] arg;

    OtsOp(int tag, byte[] arg) {
        this.tag = tag;
        this.arg = arg;
    }

    int tag() {
        return tag;
    }

    byte[] arg() {
        return arg;
    }

    boolean conArgumento() {
        return tag == APPEND || tag == PREPEND;
    }

    /** Representacion binaria: tag + (arg como varbytes si corresponde). */
    byte[] serializar() {
        byte[] salida = new byte[1 + (conArgumento() ? varbytes(arg).length : 0)];
        salida[0] = (byte) tag;
        if (conArgumento()) {
            byte[] vb = varbytes(arg);
            System.arraycopy(vb, 0, salida, 1, vb.length);
        }
        return salida;
    }

    /** Aplica la operacion al mensaje. */
    byte[] aplicar(byte[] msg) {
        return switch (tag) {
            case SHA256 -> hash("SHA-256", msg);
            case SHA1 -> hash("SHA-1", msg);
            case RIPEMD160 -> hash("RIPEMD160", msg);
            case APPEND -> concat(msg, arg);
            case PREPEND -> concat(arg, msg);
            case REVERSE -> invertir(msg);
            case HEXLIFY -> HexFormat.of().formatHex(msg).getBytes(StandardCharsets.US_ASCII);
            default -> throw new OtsException(
                "Operacion OpenTimestamps no soportada: 0x" + Integer.toHexString(tag));
        };
    }

    /** Clave de orden (tag + arg) para reproducir la serializacion canonica. */
    byte[] clave() {
        byte[] c = new byte[1 + (arg != null ? arg.length : 0)];
        c[0] = (byte) tag;
        if (arg != null) {
            System.arraycopy(arg, 0, c, 1, arg.length);
        }
        return c;
    }

    private static byte[] hash(String algoritmo, byte[] msg) {
        try {
            return MessageDigest.getInstance(algoritmo).digest(msg);
        } catch (NoSuchAlgorithmException e) {
            throw new OtsException("Hash no disponible: " + algoritmo, e);
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    private static byte[] invertir(byte[] msg) {
        byte[] r = new byte[msg.length];
        for (int i = 0; i < msg.length; i++) {
            r[i] = msg[msg.length - 1 - i];
        }
        return r;
    }

    static byte[] varbytes(byte[] valor) {
        byte[] largo = varuint(valor.length);
        byte[] r = new byte[largo.length + valor.length];
        System.arraycopy(largo, 0, r, 0, largo.length);
        System.arraycopy(valor, 0, r, largo.length, valor.length);
        return r;
    }

    /** Entero sin signo en base128 little-endian (LEB128). */
    static byte[] varuint(long valor) {
        if (valor == 0) {
            return new byte[] {0};
        }
        byte[] tmp = new byte[10];
        int i = 0;
        long v = valor;
        while (v != 0) {
            int b = (int) (v & 0x7f);
            v >>>= 7;
            if (v != 0) {
                b |= 0x80;
            }
            tmp[i++] = (byte) b;
        }
        byte[] r = new byte[i];
        System.arraycopy(tmp, 0, r, 0, i);
        return r;
    }
}
