package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionBajaCuenta;
import com.temipora.backend_springboot.modelo.ConfiguracionBajaCuentaDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionBajaCuentaPublicaDTO;
import com.temipora.backend_springboot.repositorios.ConfiguracionBajaCuentaRepository;

@Service
public class ConfiguracionBajaCuentaService {

    private static final String ENTIDAD = "ConfiguracionBajaCuenta";
    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("diasParaReactivar", "Días para reactivar una cuenta"));

    private final ConfiguracionBajaCuentaRepository configuracionRepository;
    private final AuditoriaService auditoria;

    public ConfiguracionBajaCuentaService(
            ConfiguracionBajaCuentaRepository configuracionRepository,
            AuditoriaService auditoria) {
        this.configuracionRepository = configuracionRepository;
        this.auditoria = auditoria;
    }

    @Transactional
    public ConfiguracionBajaCuenta obtener() {
        return configuracionRepository.findById(ConfiguracionBajaCuenta.ID_UNICO)
            .orElseGet(() -> {
                ConfiguracionBajaCuenta config = new ConfiguracionBajaCuenta();
                config.setIdConfiguracion(ConfiguracionBajaCuenta.ID_UNICO);
                config.setDiasParaReactivar(ConfiguracionBajaCuenta.DIAS_POR_DEFECTO);
                return configuracionRepository.save(config);
            });
    }

    @Transactional
    public ConfiguracionBajaCuentaDTO obtenerDto() {
        return ConfiguracionBajaCuentaDTO.desdeEntidad(obtener());
    }

    @Transactional(readOnly = true)
    public ConfiguracionBajaCuentaPublicaDTO obtenerPublico() {
        return new ConfiguracionBajaCuentaPublicaDTO(diasParaReactivar());
    }

    /**
     * Lectura sin escritura para los chequeos de sesión y el cálculo del plazo.
     * Si la fila todavía no existe, se usa el valor por defecto.
     */
    @Transactional(readOnly = true)
    public int diasParaReactivar() {
        return configuracionRepository.findById(ConfiguracionBajaCuenta.ID_UNICO)
            .map(ConfiguracionBajaCuenta::getDiasParaReactivar)
            .orElse(ConfiguracionBajaCuenta.DIAS_POR_DEFECTO);
    }

    @Transactional
    public ConfiguracionBajaCuentaDTO actualizar(ConfiguracionBajaCuentaDTO request) {
        if (request == null || request.getDiasParaReactivar() == null) {
            throw new IllegalArgumentException(
                "Los días para reactivar una cuenta son obligatorios.");
        }
        int dias = request.getDiasParaReactivar();
        if (dias < 1 || dias > 365) {
            throw new IllegalArgumentException(
                "El plazo para reactivar debe estar entre 1 y 365 días.");
        }

        ConfiguracionBajaCuenta config = obtener();
        Snapshot antes = Snapshot.de(config, CAMPOS);
        config.setDiasParaReactivar(dias);
        config.setFechaActualizacion(LocalDateTime.now());
        ConfiguracionBajaCuenta guardada = configuracionRepository.save(config);
        auditoria.registrar(
            AccionesAdmin.BAJA_CUENTA_CONFIG_ACTUALIZAR, ENTIDAD,
            guardada.getIdConfiguracion(),
            "Actualizó el plazo para reactivar una cuenta dada de baja",
            antes, guardada, CAMPOS);
        return ConfiguracionBajaCuentaDTO.desdeEntidad(guardada);
    }
}
