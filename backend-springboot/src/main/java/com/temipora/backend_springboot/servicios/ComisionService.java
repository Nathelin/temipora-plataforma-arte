package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

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

    private static final String ENTIDAD = "ConfiguracionComision";
    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("porcentajeServicio", "Comisión de servicios"),
        CampoAuditado.de("porcentajeProducto", "Comisión de productos"),
        CampoAuditado.de("porcentajeReventa", "Comisión de reventas"));

    private final ConfiguracionComisionRepository configuracionRepository;
    private final AuditoriaService auditoria;

    public ComisionService(
            ConfiguracionComisionRepository configuracionRepository,
            AuditoriaService auditoria) {
        this.configuracionRepository = configuracionRepository;
        this.auditoria = auditoria;
    }

    @Transactional
    public ConfiguracionComision obtener() {
        return configuracionRepository.findById(ConfiguracionComision.ID_UNICO)
            .orElseGet(() -> {
                ConfiguracionComision config = new ConfiguracionComision();
                config.setIdConfiguracion(ConfiguracionComision.ID_UNICO);
                config.setPorcentajeServicio(ConfiguracionComision.PORCENTAJE_POR_DEFECTO);
                config.setPorcentajeProducto(ConfiguracionComision.PORCENTAJE_POR_DEFECTO);
                config.setPorcentajeReventa(ConfiguracionComision.PORCENTAJE_POR_DEFECTO);
                return configuracionRepository.save(config);
            });
    }

    @Transactional
    public ComisionConfigDTO obtenerEstado() {
        return ComisionConfigDTO.desdeConfiguracion(obtener());
    }

    @Transactional
    public ComisionConfigDTO actualizar(ComisionConfigRequest request) {
        if (request == null
                || request.getPorcentajeServicio() == null
                || request.getPorcentajeProducto() == null
                || request.getPorcentajeReventa() == null) {
            throw new IllegalArgumentException("Las comisiones son obligatorias.");
        }
        BigDecimal servicio = normalizar(request.getPorcentajeServicio(), "servicios");
        BigDecimal producto = normalizar(request.getPorcentajeProducto(), "productos");
        BigDecimal reventa = normalizar(request.getPorcentajeReventa(), "reventas");

        ConfiguracionComision config = obtener();
        Snapshot antes = Snapshot.de(config, CAMPOS);
        config.setPorcentajeServicio(servicio);
        config.setPorcentajeProducto(producto);
        config.setPorcentajeReventa(reventa);
        config.setFechaActualizacion(LocalDateTime.now());
        ConfiguracionComision guardada = configuracionRepository.save(config);
        auditoria.registrar(
            AccionesAdmin.COMISION_ACTUALIZAR, ENTIDAD, guardada.getIdConfiguracion(),
            "Actualizó la comisión de la plataforma", antes, guardada, CAMPOS);
        return ComisionConfigDTO.desdeConfiguracion(guardada);
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
