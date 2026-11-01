package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionInactividad;
import com.temipora.backend_springboot.modelo.ConfiguracionInactividadDTO;
import com.temipora.backend_springboot.repositorios.ConfiguracionInactividadRepository;

@Service
public class ConfiguracionInactividadService {

    private final ConfiguracionInactividadRepository repository;

    public ConfiguracionInactividadService(
            ConfiguracionInactividadRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ConfiguracionInactividad obtener() {
        return repository.findById(ConfiguracionInactividad.ID_UNICO)
            .orElseGet(() -> {
                ConfiguracionInactividad config = new ConfiguracionInactividad();
                config.setIdConfiguracion(ConfiguracionInactividad.ID_UNICO);
                return repository.save(config);
            });
    }

    @Transactional
    public ConfiguracionInactividadDTO obtenerDto() {
        return ConfiguracionInactividadDTO.desdeEntidad(obtener());
    }

    @Transactional
    public ConfiguracionInactividadDTO actualizar(
            ConfiguracionInactividadDTO cambios) {
        if (cambios == null || cambios.getPorcentajeDescuento() == null) {
            throw new IllegalArgumentException(
                "El porcentaje de descuento es obligatorio.");
        }
        BigDecimal porcentaje = cambios.getPorcentajeDescuento()
            .setScale(2, RoundingMode.HALF_UP);
        if (porcentaje.signum() < 0
                || porcentaje.compareTo(new BigDecimal("50.00")) > 0) {
            throw new IllegalArgumentException(
                "El descuento debe estar entre 0 y 50%.");
        }

        ConfiguracionInactividad config = obtener();
        config.setDiasSinCompra(Math.max(0, cambios.getDiasSinCompra()));
        config.setDiasSinConexion(Math.max(0, cambios.getDiasSinConexion()));
        config.setDiasCooldown(Math.max(0, cambios.getDiasCooldown()));
        config.setDiasMinimosCuenta(Math.max(0, cambios.getDiasMinimosCuenta()));
        config.setPorcentajeDescuento(porcentaje);
        config.setVigenciaHoras(Math.max(1, cambios.getVigenciaHoras()));
        config.setMaxOfertasPorUsuario(Math.max(1, cambios.getMaxOfertasPorUsuario()));
        config.setMaxUsuariosPorCorrida(
            Math.max(1, cambios.getMaxUsuariosPorCorrida()));
        config.setFechaActualizacion(LocalDateTime.now());
        return ConfiguracionInactividadDTO.desdeEntidad(
            repository.save(config));
    }
}
