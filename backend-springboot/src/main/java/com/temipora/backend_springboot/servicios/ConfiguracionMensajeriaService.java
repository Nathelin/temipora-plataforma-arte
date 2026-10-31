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

    private final ConfiguracionMensajeriaRepository configuracionRepository;

    public ConfiguracionMensajeriaService(
            ConfiguracionMensajeriaRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
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
        config.setLongitudMaximaMensaje(request.getLongitudMaximaMensaje());
        config.setLongitudMaximaMotivo(request.getLongitudMaximaMotivo());
        config.setHorasEsperaReenvio(request.getHorasEsperaReenvio());
        config.setMaxSolicitudesPendientes(request.getMaxSolicitudesPendientes());
        config.setMaxSolicitudesPorDia(request.getMaxSolicitudesPorDia());
        config.setMaxMensajesPorDia(request.getMaxMensajesPorDia());
        config.setFechaActualizacion(LocalDateTime.now());
        return ConfiguracionMensajeriaDTO.desdeConfiguracion(
            configuracionRepository.save(config));
    }
}
