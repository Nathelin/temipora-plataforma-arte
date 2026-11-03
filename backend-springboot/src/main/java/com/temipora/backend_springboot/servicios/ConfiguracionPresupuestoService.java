package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionPresupuesto;
import com.temipora.backend_springboot.modelo.PresupuestoConfigDTO;
import com.temipora.backend_springboot.modelo.PresupuestoConfigRequest;
import com.temipora.backend_springboot.repositorios.ConfiguracionPresupuestoRepository;

@Service
public class ConfiguracionPresupuestoService {

    private static final String ENTIDAD = "ConfiguracionPresupuesto";
    private static final java.util.List<CampoAuditado> CAMPOS = java.util.List.of(
        CampoAuditado.de("vigenciaHoras", "Vigencia en horas"));

    private final ConfiguracionPresupuestoRepository configuracionRepository;
    private final AuditoriaService auditoria;

    public ConfiguracionPresupuestoService(
            ConfiguracionPresupuestoRepository configuracionRepository,
            AuditoriaService auditoria) {
        this.configuracionRepository = configuracionRepository;
        this.auditoria = auditoria;
    }

    @Transactional
    public ConfiguracionPresupuesto obtener() {
        return configuracionRepository.findById(ConfiguracionPresupuesto.ID_UNICO)
            .orElseGet(() -> {
                ConfiguracionPresupuesto config = new ConfiguracionPresupuesto();
                config.setIdConfiguracion(ConfiguracionPresupuesto.ID_UNICO);
                config.setVigenciaHoras(ConfiguracionPresupuesto.VIGENCIA_POR_DEFECTO_HORAS);
                return configuracionRepository.save(config);
            });
    }

    @Transactional
    public PresupuestoConfigDTO obtenerEstado() {
        return PresupuestoConfigDTO.desdeConfiguracion(obtener());
    }

    @Transactional
    public PresupuestoConfigDTO actualizar(PresupuestoConfigRequest request) {
        if (request == null || request.getVigenciaHoras() == null) {
            throw new IllegalArgumentException("La vigencia es obligatoria.");
        }
        int horas = request.getVigenciaHoras();
        if (horas < 1 || horas > 8760) {
            throw new IllegalArgumentException(
                "La vigencia debe estar entre 1 y 8760 horas.");
        }
        ConfiguracionPresupuesto config = obtener();
        Snapshot antes = Snapshot.de(config, CAMPOS);
        config.setVigenciaHoras(horas);
        config.setFechaActualizacion(LocalDateTime.now());
        ConfiguracionPresupuesto guardada = configuracionRepository.save(config);
        auditoria.registrar(
            AccionesAdmin.PRESUPUESTO_CONFIG_ACTUALIZAR, ENTIDAD, guardada.getIdConfiguracion(),
            "Actualizó la vigencia de los presupuestos", antes, guardada, CAMPOS);
        return PresupuestoConfigDTO.desdeConfiguracion(guardada);
    }
}
