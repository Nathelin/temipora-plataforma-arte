package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionMensajeria;
import com.temipora.backend_springboot.modelo.ConfiguracionMensajeriaDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionMensajeriaRequest;
import com.temipora.backend_springboot.repositorios.ConfiguracionMensajeriaRepository;

@Service
public class ConfiguracionMensajeriaService {

    private static final String ENTIDAD = "ConfiguracionMensajeria";
    private static final java.util.List<CampoAuditado> CAMPOS = java.util.List.of(
        CampoAuditado.de("longitudMaximaMensaje", "Caracteres por mensaje"),
        CampoAuditado.de("longitudMaximaMotivo", "Caracteres del motivo"),
        CampoAuditado.de("horasEsperaReenvio", "Horas para reenviar"),
        CampoAuditado.de("maxSolicitudesPendientes", "Solicitudes pendientes"),
        CampoAuditado.de("maxSolicitudesPorDia", "Solicitudes por día"),
        CampoAuditado.de("maxMensajesPorDia", "Mensajes por día"),
        CampoAuditado.de("diasVencimientoSolicitud", "Días para vencer una solicitud"));

    private final ConfiguracionMensajeriaRepository configuracionRepository;
    private final AuditoriaService auditoria;

    public ConfiguracionMensajeriaService(
            ConfiguracionMensajeriaRepository configuracionRepository,
            AuditoriaService auditoria) {
        this.configuracionRepository = configuracionRepository;
        this.auditoria = auditoria;
    }

    @Transactional
    public ConfiguracionMensajeria obtener() {
        return configuracionRepository.findById(ConfiguracionMensajeria.ID_UNICO)
            .orElseGet(() -> {
                ConfiguracionMensajeria config = new ConfiguracionMensajeria();
                config.setIdConfiguracion(ConfiguracionMensajeria.ID_UNICO);
                config.setLongitudMaximaMensaje(
                    ConfiguracionMensajeria.LONGITUD_MAXIMA_MENSAJE_POR_DEFECTO);
                config.setLongitudMaximaMotivo(
                    ConfiguracionMensajeria.LONGITUD_MAXIMA_MOTIVO_POR_DEFECTO);
                config.setHorasEsperaReenvio(
                    ConfiguracionMensajeria.HORAS_ESPERA_REENVIO_POR_DEFECTO);
                config.setMaxSolicitudesPendientes(
                    ConfiguracionMensajeria.MAX_SOLICITUDES_PENDIENTES_POR_DEFECTO);
                config.setMaxSolicitudesPorDia(
                    ConfiguracionMensajeria.MAX_SOLICITUDES_POR_DIA_POR_DEFECTO);
                config.setMaxMensajesPorDia(
                    ConfiguracionMensajeria.MAX_MENSAJES_POR_DIA_POR_DEFECTO);
                config.setDiasVencimientoSolicitud(
                    ConfiguracionMensajeria.DIAS_VENCIMIENTO_SOLICITUD_POR_DEFECTO);
                return configuracionRepository.save(config);
            });
    }

    @Transactional
    public ConfiguracionMensajeriaDTO obtenerEstado() {
        return ConfiguracionMensajeriaDTO.desdeConfiguracion(obtener());
    }

    @Transactional
    public ConfiguracionMensajeriaDTO actualizar(
            ConfiguracionMensajeriaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                "La configuración de mensajería es obligatoria.");
        }
        ConfiguracionMensajeria config = obtener();
        Snapshot antes = Snapshot.de(config, CAMPOS);
        config.setLongitudMaximaMensaje(request.getLongitudMaximaMensaje());
        config.setLongitudMaximaMotivo(request.getLongitudMaximaMotivo());
        config.setHorasEsperaReenvio(request.getHorasEsperaReenvio());
        config.setMaxSolicitudesPendientes(request.getMaxSolicitudesPendientes());
        config.setMaxSolicitudesPorDia(request.getMaxSolicitudesPorDia());
        config.setMaxMensajesPorDia(request.getMaxMensajesPorDia());
        config.setDiasVencimientoSolicitud(request.getDiasVencimientoSolicitud());
        config.setFechaActualizacion(LocalDateTime.now());
        ConfiguracionMensajeria guardada = configuracionRepository.save(config);
        auditoria.registrar(
            AccionesAdmin.MENSAJERIA_CONFIG_ACTUALIZAR, ENTIDAD, guardada.getIdConfiguracion(),
            "Actualizó la configuración de mensajería", antes, guardada, CAMPOS);
        return ConfiguracionMensajeriaDTO.desdeConfiguracion(guardada);
    }
}
