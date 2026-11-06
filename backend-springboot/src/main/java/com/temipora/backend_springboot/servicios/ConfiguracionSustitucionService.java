package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionSustitucion;
import com.temipora.backend_springboot.modelo.ConfiguracionSustitucionDTO;
import com.temipora.backend_springboot.repositorios.ConfiguracionSustitucionRepository;

@Service
public class ConfiguracionSustitucionService {

    private static final String ENTIDAD = "ConfiguracionSustitucion";
    private static final java.util.List<CampoAuditado> CAMPOS = java.util.List.of(
        CampoAuditado.de("habilitado", "Proceso habilitado"),
        CampoAuditado.de("maxAlternativas", "Alternativas por búsqueda"),
        CampoAuditado.de("maxPorArtista", "Alternativas por artista"),
        CampoAuditado.de("rangoPrecioPct", "Diferencia de precio admitida"),
        CampoAuditado.de("bonusFavorito", "Peso extra para artistas seguidos"),
        CampoAuditado.de("factorTiempoMax", "Tope de tiempo de entrega"),
        CampoAuditado.de("mostrarEnExplorar", "Mostrar sin cupos en Explorar"),
        CampoAuditado.de("minutosDedupe", "Minutos de agrupación"),
        CampoAuditado.de("diasRetencion", "Días de retención"),
        CampoAuditado.de("emailCierreHabilitado", "Mails de cierre"),
        CampoAuditado.de("emailCierreMinutos", "Minutos de agrupación del mail"),
        CampoAuditado.de("emailCierreMax", "Cierres por mail"),
        CampoAuditado.de("emailCierreVigenciaHoras", "Vigencia del enlace del mail"));

    private final ConfiguracionSustitucionRepository repository;
    private final AuditoriaService auditoria;

    public ConfiguracionSustitucionService(
            ConfiguracionSustitucionRepository repository,
            AuditoriaService auditoria) {
        this.repository = repository;
        this.auditoria = auditoria;
    }

    @Transactional
    public ConfiguracionSustitucion obtener() {
        return repository.findById(ConfiguracionSustitucion.ID_UNICO)
            .orElseGet(() -> {
                ConfiguracionSustitucion config = new ConfiguracionSustitucion();
                config.setIdConfiguracion(ConfiguracionSustitucion.ID_UNICO);
                return repository.save(config);
            });
    }

    @Transactional
    public ConfiguracionSustitucionDTO obtenerDto() {
        return ConfiguracionSustitucionDTO.desdeEntidad(obtener());
    }

    @Transactional
    public ConfiguracionSustitucionDTO actualizar(
            ConfiguracionSustitucionDTO cambios) {
        if (cambios == null) {
            throw new IllegalArgumentException("La configuración es obligatoria.");
        }
        if (cambios.getRangoPrecioPct() == null
                || cambios.getBonusFavorito() == null
                || cambios.getFactorTiempoMax() == null) {
            throw new IllegalArgumentException(
                "Los valores de similitud son obligatorios.");
        }

        BigDecimal rango = cambios.getRangoPrecioPct()
            .setScale(2, RoundingMode.HALF_UP);
        if (rango.signum() < 0
                || rango.compareTo(new BigDecimal("200.00")) > 0) {
            throw new IllegalArgumentException(
                "La diferencia de precio debe estar entre 0 y 200%.");
        }
        BigDecimal bonus = cambios.getBonusFavorito()
            .setScale(3, RoundingMode.HALF_UP);
        if (bonus.signum() < 0 || bonus.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                "El peso extra para artistas seguidos debe estar entre 0 y 1.");
        }
        BigDecimal factorTiempo = cambios.getFactorTiempoMax()
            .setScale(2, RoundingMode.HALF_UP);
        if (factorTiempo.compareTo(BigDecimal.ONE) < 0
                || factorTiempo.compareTo(new BigDecimal("10.00")) > 0) {
            throw new IllegalArgumentException(
                "El tope de tiempo de entrega debe estar entre 1 y 10 veces.");
        }

        ConfiguracionSustitucion config = obtener();
        Snapshot antes = Snapshot.de(config, CAMPOS);
        config.setHabilitado(cambios.isHabilitado());
        config.setMaxAlternativas(clamp(cambios.getMaxAlternativas(), 1, 20));
        config.setMaxPorArtista(clamp(cambios.getMaxPorArtista(), 1, 5));
        config.setRangoPrecioPct(rango);
        config.setBonusFavorito(bonus);
        config.setFactorTiempoMax(factorTiempo);
        config.setMostrarEnExplorar(cambios.isMostrarEnExplorar());
        config.setMinutosDedupe(clamp(cambios.getMinutosDedupe(), 0, 1440));
        config.setDiasRetencion(Math.max(0, cambios.getDiasRetencion()));
        config.setEmailCierreHabilitado(cambios.isEmailCierreHabilitado());
        config.setEmailCierreMinutos(
            clamp(cambios.getEmailCierreMinutos(), 0, 1440));
        config.setEmailCierreMax(clamp(cambios.getEmailCierreMax(), 1, 20));
        config.setEmailCierreVigenciaHoras(
            clamp(cambios.getEmailCierreVigenciaHoras(), 1, 720));
        config.setFechaActualizacion(LocalDateTime.now());
        ConfiguracionSustitucion guardada = repository.save(config);
        auditoria.registrar(
            AccionesAdmin.SUSTITUCION_CONFIG_ACTUALIZAR, ENTIDAD,
            guardada.getIdConfiguracion(),
            "Actualizó la configuración de sustituciones de artistas",
            antes, guardada, CAMPOS);
        return ConfiguracionSustitucionDTO.desdeEntidad(guardada);
    }

    private int clamp(int valor, int minimo, int maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
    }
}
