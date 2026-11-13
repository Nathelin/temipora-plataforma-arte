package com.temipora.backend_springboot.servicios.opentimestamps;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.temipora.backend_springboot.servicios.opentimestamps.OtsAttestation.Cursor;
import com.temipora.backend_springboot.servicios.opentimestamps.OtsTimestamp.Child;

/**
 * Lectura/escritura del formato binario de OpenTimestamps (compatible con el
 * archivo .ots del cliente oficial). Reproduce el mismo orden canonico que la
 * implementacion de referencia para que un tercero pueda verificar el sello.
 *
 * No genera hashes de Bitcoin: solo interpreta el arbol de operaciones y
 * atestaciones. La verificacion criptografica final contra el bloque de
 * Bitcoin la hace el verificador externo (o nuestra comprobacion de altura).
 */
public final class OtsCodec {

    private static final byte[] MAGIC = new byte[] {
        0x00, 'O', 'p', 'e', 'n', 'T', 'i', 'm', 'e', 's', 't', 'a', 'm', 'p', 's',
        0x00, 0x00, 'P', 'r', 'o', 'o', 'f', 0x00,
        (byte) 0xbf, (byte) 0x89, (byte) 0xe2, (byte) 0xe8, (byte) 0x84, (byte) 0xe8,
        (byte) 0x92, (byte) 0x94
    };

    private static final int VERSION = 1;

    private OtsCodec() {
    }

    // ------------------------------------------------------------------
    // Lectura / escritura del cuerpo (arbol de operaciones)
    // ------------------------------------------------------------------

    public static OtsTimestamp deserializar(byte[] cuerpo, byte[] mensajeInicial) {
        return leerTimestamp(new Cursor(cuerpo), mensajeInicial);
    }

    private static OtsTimestamp leerTimestamp(Cursor c, byte[] msg) {
        OtsTimestamp nodo = new OtsTimestamp(msg);
        int tag = c.readByte();
        while (tag == 0xff) {
            leerItem(c, nodo, c.readByte());
            tag = c.readByte();
        }
        leerItem(c, nodo, tag);
        return nodo;
    }

    private static void leerItem(Cursor c, OtsTimestamp nodo, int itemTag) {
        if (itemTag == 0x00) {
            byte[] tag = c.readBytes(8);
            byte[] payload = c.readVarBytes();
            nodo.addAttestation(new OtsAttestation(tag, payload));
            return;
        }
        int opTag = itemTag;
        byte[] arg = null;
        if (opTag == OtsOp.APPEND || opTag == OtsOp.PREPEND) {
            arg = c.readVarBytes();
        }
        OtsOp op = new OtsOp(opTag, arg);
        byte[] resultado = op.aplicar(nodo.msg());
        nodo.addOp(op, leerTimestamp(c, resultado));
    }

    public static byte[] serializar(OtsTimestamp raiz) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializarEn(raiz, out);
        return out.toByteArray();
    }

    private static void serializarEn(OtsTimestamp nodo, ByteArrayOutputStream out) {
        List<OtsAttestation> atts = new ArrayList<>(nodo.attestations());
        atts.sort((a, b) -> comparar(a.clave(), b.clave()));
        List<Child> ops = new ArrayList<>(nodo.ops());
        ops.sort((a, b) -> comparar(a.op().clave(), b.op().clave()));

        int total = atts.size() + ops.size();
        if (total == 0) {
            throw new OtsException("Un sello OpenTimestamps no puede quedar vacio.");
        }
        int index = 0;
        for (OtsAttestation att : atts) {
            index++;
            if (index < total) {
                out.write(0xff);
            }
            out.write(0x00);
            out.write(att.tag(), 0, att.tag().length);
            escribirVarBytes(out, att.payload());
        }
        for (Child child : ops) {
            index++;
            if (index < total) {
                out.write(0xff);
            }
            out.write(child.op().tag());
            if (child.op().conArgumento()) {
                escribirVarBytes(out, child.op().arg());
            }
            serializarEn(child.stamp(), out);
        }
    }

    // ------------------------------------------------------------------
    // Merge (incorporar una prueba actualizada en un nodo)
    // ------------------------------------------------------------------

    public static void merge(OtsTimestamp raiz, OtsTimestamp otro) {
        OtsTimestamp nodo = buscarPorMensaje(raiz, otro.msg());
        if (nodo != null) {
            mergeNodo(nodo, otro);
        }
    }

    private static void mergeNodo(OtsTimestamp nodo, OtsTimestamp otro) {
        for (OtsAttestation att : otro.attestations()) {
            boolean existe = nodo.attestations().stream()
                .anyMatch(a -> Arrays.equals(a.clave(), att.clave()));
            if (!existe) {
                nodo.addAttestation(att);
            }
        }
        for (Child child : otro.ops()) {
            Child existente = nodo.child(OtsTimestamp.hex(child.op().clave()));
            if (existente == null) {
                nodo.addOp(child.op(), child.stamp());
            } else {
                mergeNodo(existente.stamp(), child.stamp());
            }
        }
    }

    public static OtsTimestamp buscarPorMensaje(OtsTimestamp nodo, byte[] msg) {
        if (Arrays.equals(nodo.msg(), msg)) {
            return nodo;
        }
        for (Child child : nodo.ops()) {
            OtsTimestamp encontrado = buscarPorMensaje(child.stamp(), msg);
            if (encontrado != null) {
                return encontrado;
            }
        }
        return null;
    }

    public static List<OtsAttestation> atestacionesPendientes(OtsTimestamp raiz) {
        List<OtsAttestation> resultado = new ArrayList<>();
        recolectar(raiz, att -> {
            if (att.esPendiente()) {
                resultado.add(att);
            }
        });
        return resultado;
    }

    /** Mensaje del nodo que contiene una atestacion puntual (para el upgrade). */
    public static byte[] mensajeDe(OtsTimestamp nodo, OtsAttestation objetivo) {
        if (nodo.attestations().stream()
                .anyMatch(a -> Arrays.equals(a.clave(), objetivo.clave()))) {
            return nodo.msg();
        }
        for (Child child : nodo.ops()) {
            byte[] r = mensajeDe(child.stamp(), objetivo);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    public static Optional<SelloBitcoin> buscarBitcoin(OtsTimestamp raiz) {
        return buscarBitcoinNodo(raiz);
    }

    private static Optional<SelloBitcoin> buscarBitcoinNodo(OtsTimestamp nodo) {
        for (OtsAttestation att : nodo.attestations()) {
            if (att.esBitcoin()) {
                return Optional.of(new SelloBitcoin(
                    OtsTimestamp.hex(nodo.msg()), att.altura()));
            }
        }
        for (Child child : nodo.ops()) {
            Optional<SelloBitcoin> r = buscarBitcoinNodo(child.stamp());
            if (r.isPresent()) {
                return r;
            }
        }
        return Optional.empty();
    }

    public static Optional<OtsAttestation> primeraPendiente(OtsTimestamp raiz) {
        List<OtsAttestation> pendientes = atestacionesPendientes(raiz);
        return pendientes.isEmpty() ? Optional.empty() : Optional.of(pendientes.get(0));
    }

    private static void recolectar(OtsTimestamp nodo, java.util.function.Consumer<OtsAttestation> visitante) {
        nodo.attestations().forEach(visitante);
        for (Child child : nodo.ops()) {
            recolectar(child.stamp(), visitante);
        }
    }

    // ------------------------------------------------------------------
    // Archivo .ots (DetachedTimestampFile)
    // ------------------------------------------------------------------

    /** Arma el archivo .ots: header + version + OpSHA256 + digest + cuerpo. */
    public static byte[] archivoDetached(byte[] digest, byte[] cuerpo) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(MAGIC, 0, MAGIC.length);
        out.write(VERSION);
        out.write(OtsOp.SHA256);
        out.write(digest, 0, digest.length);
        out.write(cuerpo, 0, cuerpo.length);
        return out.toByteArray();
    }

    public static Detached leerDetached(byte[] archivo) {
        Cursor c = new Cursor(archivo);
        byte[] magic = c.readBytes(MAGIC.length);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new OtsException("El archivo no es un sello OpenTimestamps valido.");
        }
        int version = c.readByte();
        if (version != VERSION) {
            throw new OtsException("Version de archivo OpenTimestamps no soportada: " + version);
        }
        int opTag = c.readByte();
        int largoDigest = (opTag == OtsOp.SHA1 || opTag == OtsOp.RIPEMD160) ? 20 : 32;
        byte[] digest = c.readBytes(largoDigest);
        byte[] cuerpo = c.readBytes(c.restante());
        return new Detached(digest, cuerpo);
    }

    private static void escribirVarBytes(ByteArrayOutputStream out, byte[] valor) {
        byte[] vb = OtsOp.varbytes(valor);
        out.write(vb, 0, vb.length);
    }

    static int comparar(byte[] a, byte[] b) {
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int x = a[i] & 0xff;
            int y = b[i] & 0xff;
            if (x != y) {
                return Integer.compare(x, y);
            }
        }
        return Integer.compare(a.length, b.length);
    }

    /** Sello de tiempo localizado en un bloque de Bitcoin. */
    public record SelloBitcoin(String hashBloqueMerkle, long altura) {
        public SelloBitcoin {
            Objects.requireNonNull(hashBloqueMerkle);
        }
    }

    /** Contenido de un archivo .ots. */
    public record Detached(byte[] digest, byte[] cuerpo) {
    }
}
