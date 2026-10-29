package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;

import com.temipora.backend_springboot.modelo.ConfiguracionResenia;
import com.temipora.backend_springboot.modelo.EtiquetaReputacion;

public final class ReputacionCalculator {

    private ReputacionCalculator() {
    }

    public static EtiquetaReputacion calcular(
            long cantidad, BigDecimal promedio, ConfiguracionResenia config) {
        if (promedio == null || cantidad < config.getMinimoReseniasEtiqueta()) {
            return EtiquetaReputacion.SIN_DATOS;
        }
        if (promedio.compareTo(config.getUmbralExcelente()) >= 0) {
            return EtiquetaReputacion.EXCELENTE;
        }
        if (promedio.compareTo(config.getUmbralBuena()) >= 0) {
            return EtiquetaReputacion.BUENA;
        }
        return EtiquetaReputacion.REGULAR;
    }
}
