package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConversionMoneda;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.repositorios.ConversionMonedaRepository;

@Service
public class ConversionMonedaService {

    private static final int ESCALA = 8;

    private final ConversionMonedaRepository conversionRepository;

    public ConversionMonedaService(ConversionMonedaRepository conversionRepository) {
        this.conversionRepository = conversionRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal factor(Moneda origen, Moneda destino, LocalDateTime fecha) {
        if (origen == null || destino == null) {
            return null;
        }
        if (mismaMoneda(origen, destino)) {
            return BigDecimal.ONE;
        }
        LocalDateTime momento = fecha != null ? fecha : LocalDateTime.now();

        BigDecimal directo = conversionRepository.vigente(origen, destino, momento).stream()
            .findFirst()
            .map(ConversionMoneda::getFactor)
            .orElse(null);
        if (directo != null) {
            return directo;
        }

        BigDecimal inverso = conversionRepository.vigente(destino, origen, momento).stream()
            .findFirst()
            .map(ConversionMoneda::getFactor)
            .orElse(null);
        if (inverso != null && inverso.signum() != 0) {
            return BigDecimal.ONE.divide(inverso, ESCALA, RoundingMode.HALF_UP);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public BigDecimal convertir(BigDecimal monto, Moneda origen, Moneda destino, LocalDateTime fecha) {
        if (monto == null) {
            return null;
        }
        BigDecimal factor = factor(origen, destino, fecha);
        if (factor == null) {
            return null;
        }
        return monto.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public ConversionMoneda factorAbierto(Moneda entrada, Moneda salida) {
        return conversionRepository.abierta(entrada, salida).stream().findFirst().orElse(null);
    }

    @Transactional
    public ConversionMoneda registrarFactor(
            Moneda entrada,
            Moneda salida,
            BigDecimal factor,
            String origen,
            LocalDateTime desde) {
        if (entrada == null || salida == null || mismaMoneda(entrada, salida)) {
            throw new IllegalArgumentException("Las monedas de la conversión deben ser distintas.");
        }
        if (factor == null || factor.signum() <= 0) {
            throw new IllegalArgumentException("El factor de conversión debe ser mayor a cero.");
        }
        LocalDateTime momento = desde != null ? desde : LocalDateTime.now();

        ConversionMoneda abierta = factorAbierto(entrada, salida);
        if (abierta != null && abierta.getFactor().compareTo(factor) == 0) {
            return abierta;
        }
        if (abierta != null) {
            abierta.setFinValidez(momento);
        }

        ConversionMoneda nuevo = new ConversionMoneda();
        nuevo.setMonedaEntrada(entrada);
        nuevo.setMonedaSalida(salida);
        nuevo.setFactor(factor);
        nuevo.setInicioValidez(momento);
        nuevo.setFinValidez(null);
        nuevo.setOrigen(origen != null ? origen : "MANUAL");
        return conversionRepository.save(nuevo);
    }

    private boolean mismaMoneda(Moneda a, Moneda b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.getIdMoneda() != null && b.getIdMoneda() != null) {
            return a.getIdMoneda().equals(b.getIdMoneda());
        }
        return java.util.Objects.equals(a.getCodigo(), b.getCodigo());
    }
}
