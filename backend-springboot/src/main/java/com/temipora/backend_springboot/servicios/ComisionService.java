package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ComisionConfigDTO;
import com.temipora.backend_springboot.modelo.ComisionConfigRequest;
import com.temipora.backend_springboot.modelo.ConfiguracionComision;
import com.temipora.backend_springboot.repositorios.ConfiguracionComisionRepository;

@Service
public class ComisionService {

    public static final BigDecimal PORCENTAJE_MAXIMO = new BigDecimal("50.00");
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final ConfiguracionComisionRepository configuracionRepository;

    public ComisionService(ConfiguracionComisionRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    @Transactional
    public ConfiguracionComision obtener() {
        return configuracionRepository.findById(ConfiguracionComision.ID_UNICO)
            .orElseGet(() -> {
                ConfiguracionComision config = new ConfiguracionComision();
                config.setIdConfiguracion(ConfiguracionComision.ID_UNICO);
                config.setPorcentajeServicio(ConfiguracionComision.PORCENTAJE_POR_DEFECTO);
                config.setPorcentajeProducto(ConfiguracionComision.PORCENTAJE_POR_DEFECTO);
                return configuracionRepository.save(config);
            });
    }

    @Transactional(readOnly = true)
    public ComisionConfigDTO obtenerEstado() {
        return ComisionConfigDTO.desdeConfiguracion(obtener());
    }

    @Transactional
    public ComisionConfigDTO actualizar(ComisionConfigRequest request) {
        if (request == null
                || request.getPorcentajeServicio() == null
                || request.getPorcentajeProducto() == null) {
            throw new IllegalArgumentException("Las comisiones son obligatorias.");
        }
        BigDecimal servicio = normalizar(request.getPorcentajeServicio(), "servicios");
        BigDecimal producto = normalizar(request.getPorcentajeProducto(), "productos");

        ConfiguracionComision config = obtener();
        config.setPorcentajeServicio(servicio);
        config.setPorcentajeProducto(producto);
        config.setFechaActualizacion(LocalDateTime.now());
        return ComisionConfigDTO.desdeConfiguracion(configuracionRepository.save(config));
    }

    public BigDecimal calcularComision(BigDecimal monto, BigDecimal porcentaje) {
        if (monto == null || porcentaje == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return monto.multiply(porcentaje)
            .divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizar(BigDecimal valor, String etiqueta) {
        if (valor.signum() < 0) {
            throw new IllegalArgumentException(
                "La comisión de " + etiqueta + " no puede ser negativa.");
        }
        if (valor.compareTo(PORCENTAJE_MAXIMO) > 0) {
            throw new IllegalArgumentException(
                "La comisión de " + etiqueta + " no puede superar el 50%.");
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
