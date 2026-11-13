package com.temipora.backend_springboot.servicios.opentimestamps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Nodo del arbol de un sello: un mensaje con sus atestaciones y operaciones. */
final class OtsTimestamp {

    private final byte[] msg;
    private final List<OtsAttestation> attestations = new ArrayList<>();
    private final Map<String, Child> ops = new LinkedHashMap<>();

    OtsTimestamp(byte[] msg) {
        this.msg = msg;
    }

    byte[] msg() {
        return msg;
    }

    List<OtsAttestation> attestations() {
        return attestations;
    }

    Collection<Child> ops() {
        return ops.values();
    }

    Child child(String claveHex) {
        return ops.get(claveHex);
    }

    void addAttestation(OtsAttestation attestation) {
        attestations.add(attestation);
    }

    void addOp(OtsOp op, OtsTimestamp stamp) {
        ops.put(hex(op.clave()), new Child(op, stamp));
    }

    static String hex(byte[] valor) {
        StringBuilder sb = new StringBuilder(valor.length * 2);
        for (byte b : valor) {
            sb.append(Character.forDigit((b >> 4) & 0xf, 16));
            sb.append(Character.forDigit(b & 0xf, 16));
        }
        return sb.toString();
    }

    record Child(OtsOp op, OtsTimestamp stamp) {
    }
}
