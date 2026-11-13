package com.temipora.backend_springboot.servicios.opentimestamps;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class OtsCodecTest {

    private static byte[] digest() {
        byte[] d = new byte[32];
        for (int i = 0; i < d.length; i++) {
            d[i] = (byte) i;
        }
        return d;
    }

    @Test
    void idaYVueltaConAtestacionDeBitcoin() {
        byte[] digest = digest();
        OtsTimestamp raiz = new OtsTimestamp(digest);
        OtsTimestamp hijo = new OtsTimestamp(oracleSha256(digest));
        hijo.addAttestation(new OtsAttestation(
            OtsAttestation.BITCOIN, OtsOp.varuint(824000L)));
        raiz.addOp(new OtsOp(OtsOp.SHA256, null), hijo);

        byte[] cuerpo = OtsCodec.serializar(raiz);
        OtsTimestamp leido = OtsCodec.deserializar(cuerpo, digest);

        Optional<OtsCodec.SelloBitcoin> bitcoin = OtsCodec.buscarBitcoin(leido);
        assertTrue(bitcoin.isPresent());
        assertEquals(824000L, bitcoin.get().altura());
        assertEquals(oracleSha256Hex(digest), bitcoin.get().hashBloqueMerkle());
        assertArrayEquals(cuerpo, OtsCodec.serializar(leido));
    }

    @Test
    void detectaAtestacionPendienteConSuUri() {
        byte[] digest = digest();
        String uri = "https://alice.btc.calendar.opentimestamps.org";
        OtsTimestamp raiz = new OtsTimestamp(digest);
        OtsTimestamp hijo = new OtsTimestamp(oracleSha256(digest));
        hijo.addAttestation(new OtsAttestation(
            OtsAttestation.PENDING,
            OtsOp.varbytes(uri.getBytes(StandardCharsets.UTF_8))));
        raiz.addOp(new OtsOp(OtsOp.SHA256, null), hijo);

        byte[] cuerpo = OtsCodec.serializar(raiz);
        OtsTimestamp leido = OtsCodec.deserializar(cuerpo, digest);

        var pendientes = OtsCodec.atestacionesPendientes(leido);
        assertEquals(1, pendientes.size());
        assertEquals(uri, pendientes.get(0).uri());
        assertTrue(OtsCodec.buscarBitcoin(leido).isEmpty());
    }

    @Test
    void elArchivoDetachedConservaElDigest() {
        byte[] digest = digest();
        OtsTimestamp raiz = new OtsTimestamp(digest);
        raiz.addAttestation(new OtsAttestation(
            OtsAttestation.BITCOIN, OtsOp.varuint(1L)));
        byte[] cuerpo = OtsCodec.serializar(raiz);

        byte[] archivo = OtsCodec.archivoDetached(digest, cuerpo);
        OtsCodec.Detached detached = OtsCodec.leerDetached(archivo);

        assertArrayEquals(digest, detached.digest());
        assertFalse(detached.cuerpo().length == 0);
    }

    private static byte[] oracleSha256(byte[] valor) {
        try {
            return java.security.MessageDigest.getInstance("SHA-256").digest(valor);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String oracleSha256Hex(byte[] valor) {
        return OtsTimestamp.hex(oracleSha256(valor));
    }
}
