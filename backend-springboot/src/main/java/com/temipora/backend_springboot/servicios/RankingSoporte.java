package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Matemática compartida por los recomendadores de servicios y productos.
 *
 * Se mantiene como utilidad estática para no acoplar la fórmula a una entidad
 * concreta: ambos rankers puntúan candidatos distintos pero normalizan,
 * comparan etiquetas y diversifican igual.
 */
public final class RankingSoporte {

    private RankingSoporte() {
    }

    public static double jaccard(Set<Long> a, Set<Long> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int interseccion = 0;
        for (Long valor : a) {
            if (b.contains(valor)) {
                interseccion++;
            }
        }
        int union = a.size() + b.size() - interseccion;
        return union <= 0 ? 0 : interseccion / (double) union;
    }

    public static Map<Long, Double> normalizar(
            Map<Long, Long> crudo,
            Map<Long, Long> popularidad) {
        Map<Long, Double> resultado = new HashMap<>();
        if (crudo == null) {
            return resultado;
        }
        for (Map.Entry<Long, Long> entrada : crudo.entrySet()) {
            long total = popularidad != null
                ? popularidad.getOrDefault(entrada.getKey(), 0L)
                : 0L;
            if (total <= 0) {
                continue;
            }
            resultado.put(
                entrada.getKey(),
                Math.min(1.0, entrada.getValue() / (double) total));
        }
        return resultado;
    }

    public static double recencia(LocalDateTime ultima, int diasDecaimiento) {
        if (ultima == null) {
            return 0;
        }
        long dias = ChronoUnit.DAYS.between(ultima.toLocalDate(), LocalDate.now());
        if (dias < 0) {
            dias = 0;
        }
        return Math.max(0, 1.0 - dias / (double) Math.max(1, diasDecaimiento));
    }

    public static double precioSim(
            BigDecimal referencia,
            BigDecimal candidato,
            BigDecimal rangoPct) {
        if (referencia == null || candidato == null || referencia.signum() <= 0) {
            return 0.5;
        }
        double diferencia = Math.abs(candidato.subtract(referencia).doubleValue())
            / referencia.doubleValue();
        double rango = rangoPct != null ? rangoPct.doubleValue() / 100.0 : 0.4;
        if (rango <= 0) {
            return diferencia == 0 ? 1 : 0;
        }
        return Math.max(0, Math.min(1, 1 - diferencia / rango));
    }

    /**
     * Fuerza presencia de al menos {@code minimoFranjas} franjas de precio
     * dentro de una selección ya ordenada por puntaje. Devuelve una lista
     * nueva sin perder ni reordenar de más los elementos restantes.
     */
    public static <T> List<T> diversificarPorPrecio(
            List<T> seleccionados,
            Function<T, BigDecimal> precio,
            int minimoFranjas) {
        if (seleccionados == null || seleccionados.size() <= 1 || minimoFranjas <= 1) {
            return seleccionados == null
                ? new ArrayList<>()
                : new ArrayList<>(seleccionados);
        }
        List<BigDecimal> valores = seleccionados.stream()
            .map(precio)
            .filter(Objects::nonNull)
            .sorted()
            .toList();
        if (valores.size() < minimoFranjas) {
            return new ArrayList<>(seleccionados);
        }

        BigDecimal corte33 = valores.get((int) Math.floor(valores.size() * 0.33));
        BigDecimal corte66 = valores.get((int) Math.floor(valores.size() * 0.66));

        List<T> resultado = new ArrayList<>();
        Set<T> incluidos = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Integer> franjas = new HashSet<>();

        int iniciales = Math.min(2, seleccionados.size());
        for (int i = 0; i < iniciales; i++) {
            T item = seleccionados.get(i);
            resultado.add(item);
            incluidos.add(item);
            franjas.add(franja(precio.apply(item), corte33, corte66));
        }

        for (int numeroFranja = 0;
                numeroFranja < 3 && franjas.size() < minimoFranjas;
                numeroFranja++) {
            if (franjas.contains(numeroFranja)) {
                continue;
            }
            for (T item : seleccionados) {
                if (incluidos.contains(item)) {
                    continue;
                }
                if (franja(precio.apply(item), corte33, corte66) != numeroFranja) {
                    continue;
                }
                resultado.add(item);
                incluidos.add(item);
                franjas.add(numeroFranja);
                break;
            }
        }

        for (T item : seleccionados) {
            if (incluidos.add(item)) {
                resultado.add(item);
            }
        }
        return resultado;
    }

    private static int franja(
            BigDecimal precio,
            BigDecimal corte33,
            BigDecimal corte66) {
        if (precio == null) {
            return -1;
        }
        if (precio.compareTo(corte33) <= 0) {
            return 0;
        }
        if (precio.compareTo(corte66) <= 0) {
            return 1;
        }
        return 2;
    }
}
