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

    private static final String ENTIDAD = "ConfiguracionInactividad";
    private static final java.util.List<CampoAuditado> CAMPOS = java.util.List.of(
        CampoAuditado.de("diasSinCompra", "Días sin compra"),
        CampoAuditado.de("diasSinConexion", "Días sin conexión"),
        CampoAuditado.de("diasCooldown", "Días de cooldown"),
        CampoAuditado.de("diasMinimosCuenta", "Días mínimos de cuenta"),
        CampoAuditado.de("porcentajeDescuento", "Descuento"),
        CampoAuditado.de("vigenciaHoras", "Vigencia de la oferta"),
        CampoAuditado.de("maxOfertasPorUsuario", "Servicios por usuario"),
        CampoAuditado.de("maxUsuariosPorCorrida", "Usuarios por ejecución"),
        CampoAuditado.de("horaEjecucion", "Hora de envío"));

    private final ConfiguracionInactividadRepository repository;
    private final ComisionService comisionService;
    private final AuditoriaService auditoria;

    public ConfiguracionInactividadService(
            ConfiguracionInactividadRepository repository,
            ComisionService comisionService,
            AuditoriaService auditoria) {
        this.repository = repository;
        this.comisionService = comisionService;
        this.auditoria = auditoria;
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
        if (cambios.getHoraEjecucion() == null) {
            throw new IllegalArgumentException(
                "La hora de envío es obligatoria.");
        }
        BigDecimal porcentaje = cambios.getPorcentajeDescuento()
            .setScale(2, RoundingMode.HALF_UP);
        if (porcentaje.signum() < 0
                || porcentaje.compareTo(new BigDecimal("50.00")) > 0) {
            throw new IllegalArgumentException(
                "El descuento debe estar entre 0 y 50%.");
        }

        BigDecimal comisionVigente = comisionService.obtener()
            .getPorcentajeServicio()
            .setScale(2, RoundingMode.HALF_UP);
        if (porcentaje.compareTo(comisionVigente) > 0) {
            throw new IllegalArgumentException(
                "El descuento no puede superar la comisión vigente de servicios ("
                    + comisionVigente.stripTrailingZeros().toPlainString()
                    + "%). Cambiala en Comisiones si querés descontar más.");
        }

        ConfiguracionInactividad config = obtener();
        Snapshot antes = Snapshot.de(config, CAMPOS);
        config.setDiasSinCompra(Math.max(0, cambios.getDiasSinCompra()));
        config.setDiasSinConexion(Math.max(0, cambios.getDiasSinConexion()));
        config.setDiasCooldown(Math.max(0, cambios.getDiasCooldown()));
        config.setDiasMinimosCuenta(Math.max(0, cambios.getDiasMinimosCuenta()));
        config.setPorcentajeDescuento(porcentaje);
        config.setVigenciaHoras(Math.max(1, cambios.getVigenciaHoras()));
        config.setMaxOfertasPorUsuario(Math.max(1, cambios.getMaxOfertasPorUsuario()));
        config.setMaxUsuariosPorCorrida(
            Math.max(1, cambios.getMaxUsuariosPorCorrida()));
        config.setHoraEjecucion(cambios.getHoraEjecucion());
        config.setFechaActualizacion(LocalDateTime.now());
        ConfiguracionInactividad guardada = repository.save(config);
        auditoria.registrar(
            AccionesAdmin.INACTIVIDAD_CONFIG_ACTUALIZAR, ENTIDAD, guardada.getIdConfiguracion(),
            "Actualizó la configuración de promociones por inactividad", antes, guardada, CAMPOS);
        return ConfiguracionInactividadDTO.desdeEntidad(guardada);
    }

    @Transactional
    public void marcarCorridaProgramada(LocalDateTime momento) {
        ConfiguracionInactividad config = obtener();
        config.setUltimaCorridaProgramada(momento);
        repository.save(config);
    }
}
