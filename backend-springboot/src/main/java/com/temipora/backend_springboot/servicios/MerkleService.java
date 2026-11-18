package com.temipora.backend_springboot.servicios;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Arbol de Merkle binario determinista sobre una lista de hashes (hex).
 *
 * Hoja i = bytes del hash (32). Nodo padre = SHA-256(izq || der). Si un nivel
 * tiene cantidad impar, el ultimo nodo se promueve sin cambios. La raiz se
 * sella con OpenTimestamps y permite emitir pruebas de inclusion.
 */
@Service
public class MerkleService {

    /** Raiz de Merkle (hex) de la lista de hashes (hex), en el orden dado. */
    public String raiz(List<String> hashesHex) {
        return hex(nivelRaiz(aBytes(hashesHex)));
    }

    /**
     * Prueba de inclusion de la hoja en la posicion indicada: lista de pasos
     * (hermano + si el hermano va a la derecha de nuestro nodo).
     */
    public List<PasoMerkle> prueba(List<String> hashesHex, int indice) {
        List<byte[]> nivel = aBytes(hashesHex);
        List<PasoMerkle> pasos = new ArrayList<>();
        int idx = indice;
        while (nivel.size() > 1) {
            boolean hermanoDerecha = idx % 2 == 0;
            int idxHermano = hermanoDerecha ? idx + 1 : idx - 1;
            if (idxHermano < nivel.size()) {
                pasos.add(new PasoMerkle(hex(nivel.get(idxHermano)), hermanoDerecha));
            }
            List<byte[]> siguiente = new ArrayList<>();
            for (int i = 0; i < nivel.size(); i += 2) {
                if (i + 1 < nivel.size()) {
                    siguiente.add(hash(nivel.get(i), nivel.get(i + 1)));
                } else {
                    siguiente.add(nivel.get(i));
                }
            }
            idx = idx / 2;
            nivel = siguiente;
        }
        return pasos;
    }

    /** Verifica una prueba de inclusion contra una raiz conocida. */
    public boolean verificar(String hashHojaHex, List<PasoMerkle> pasos, String raizHex) {
        byte[] actual = hexBytes(hashHojaHex);
        for (PasoMerkle paso : pasos) {
            byte[] hermano = hexBytes(paso.hashHermano());
            actual = paso.hermanoDerecha()
                ? hash(actual, hermano)
                : hash(hermano, actual);
        }
        return hex(actual).equalsIgnoreCase(raizHex);
    }

    private byte[] nivelRaiz(List<byte[]> hojas) {
        if (hojas.isEmpty()) {
            return new byte[32];
        }
        List<byte[]> nivel = hojas;
        while (nivel.size() > 1) {
            List<byte[]> siguiente = new ArrayList<>();
            for (int i = 0; i < nivel.size(); i += 2) {
                if (i + 1 < nivel.size()) {
                    siguiente.add(hash(nivel.get(i), nivel.get(i + 1)));
                } else {
                    siguiente.add(nivel.get(i));
                }
            }
            nivel = siguiente;
        }
        return nivel.get(0);
    }

    private List<byte[]> aBytes(List<String> hashesHex) {
        List<byte[]> resultado = new ArrayList<>(hashesHex.size());
        for (String h : hashesHex) {
            resultado.add(hexBytes(h));
        }
        return resultado;
    }

    private byte[] hash(byte[] izq, byte[] der) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            md.update(izq);
            md.update(der);
            return md.digest();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible.", e);
        }
    }

    private byte[] hexBytes(String hex) {
        return java.util.HexFormat.of().parseHex(hex);
    }

    private String hex(byte[] valor) {
        return java.util.HexFormat.of().formatHex(valor);
    }

    /** Un paso de una prueba de inclusion de Merkle. */
    public record PasoMerkle(String hashHermano, boolean hermanoDerecha) {
    }
}
