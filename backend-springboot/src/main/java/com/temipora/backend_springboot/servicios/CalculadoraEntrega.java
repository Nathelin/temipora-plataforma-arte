package com.temipora.backend_springboot.servicios;

import java.time.LocalDate;
import java.util.Objects;

import com.temipora.backend_springboot.modelo.ETiempoEntregaUnidad;

/**
 * Reglas de cálculo del plazo de entrega de un presupuesto.
 *
 * <p>El plazo se expresa en la unidad elegida por el artista y se suma con
 * calendario real: una semana son siete días y un mes es el mismo día del mes
 * siguiente (ajustado al último día válido si no existe).</p>
 */
public final class CalculadoraEntrega {

    public static final int MAX_DIAS_PLAZO = 365;
    public static final int MAX_ANIOS_INICIO_ESTIMADO = 1;

    private CalculadoraEntrega() {
    }

    /**
     * Suma el plazo a una fecha de inicio usando calendario real.
     */
    public static LocalDate sumarPlazo(
            LocalDate inicio,
            int valor,
            ETiempoEntregaUnidad unidad) {
        Objects.requireNonNull(inicio, "La fecha de inicio es obligatoria.");
        Objects.requireNonNull(unidad, "La unidad del plazo es obligatoria.");
        if (valor < 1) {
            throw new IllegalArgumentException("El plazo debe ser al menos 1.");
        }
        return switch (unidad) {
            case DIAS -> inicio.plusDays(valor);
            case SEMANAS -> inicio.plusWeeks(valor);
            case MESES -> inicio.plusMonths(valor);
        };
    }

    /**
     * El trabajo arranca cuando ocurre lo último entre el pago y el inicio
     * estimado por el artista.
     */
    public static LocalDate inicioReal(
            LocalDate fechaPago,
            LocalDate fechaInicioEstimada) {
        Objects.requireNonNull(fechaPago, "La fecha de pago es obligatoria.");
        if (fechaInicioEstimada == null) {
            return fechaPago;
        }
        return fechaInicioEstimada.isAfter(fechaPago)
            ? fechaInicioEstimada
            : fechaPago;
    }

    /**
     * Calcula la fecha de entrega comprometida para un pago concreto.
     */
    public static LocalDate fechaEntrega(
            LocalDate fechaPago,
            LocalDate fechaInicioEstimada,
            int valor,
            ETiempoEntregaUnidad unidad) {
        return sumarPlazo(
            inicioReal(fechaPago, fechaInicioEstimada),
            valor,
            unidad);
    }

    /**
     * Días normalizados que se usan para filtros y para el tope de un año.
     * No se usan para calcular la fecha de entrega.
     */
    public static long diasNormalizados(int valor, ETiempoEntregaUnidad unidad) {
        Objects.requireNonNull(unidad, "La unidad del plazo es obligatoria.");
        return (long) valor * unidad.getFactorDias();
    }

    public static boolean excedePlazoMaximo(int valor, ETiempoEntregaUnidad unidad) {
        return diasNormalizados(valor, unidad) > MAX_DIAS_PLAZO;
    }
}
