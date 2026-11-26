package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.PedidoConfigDTO;
import com.temipora.backend_springboot.modelo.PedidoConfigRequest;
import com.temipora.backend_springboot.repositorios.ConfiguracionPedidoRepository;

@Service
public class ConfiguracionPedidoService {

    private static final String ENTIDAD = "ConfiguracionPedido";
    private static final java.util.List<CampoAuditado> CAMPOS = java.util.List.of(
        CampoAuditado.de("diasAprobacionAutomatica", "Días de aprobación automática"),
        CampoAuditado.de("diasPagoCuota", "Días para pagar una cuota"),
        CampoAuditado.de("revisionesMaximasPorHito", "Revisiones máximas por hito"),
        CampoAuditado.de("diasReentrega", "Días para reentregar tras una revisión"),
        CampoAuditado.de("maxHitos", "Máximo de hitos"),
        CampoAuditado.de("porcentajeMaxPrimerHito", "Porcentaje máximo de la primera cuota"),
        CampoAuditado.de("montoMinimoCuota", "Monto mínimo por cuota"),
        CampoAuditado.de("diasGraciaEntrega", "Días de gracia de entrega"),
        CampoAuditado.de("diasSancionVentas", "Duración de la sanción de ventas"),
        CampoAuditado.de("diasReapelacion", "Días entre apelaciones"),
        CampoAuditado.de("diasCierreTrasBloqueo", "Días para cerrar tras el bloqueo"),
        CampoAuditado.de("diasGraciaPago", "Días de gracia de pago del cliente"),
        CampoAuditado.de("diasSancionCompras", "Duración de la sanción de compras"),
        CampoAuditado.de("horasRespuestaProrroga", "Horas para responder una prórroga"));

    private final ConfiguracionPedidoRepository configuracionRepository;
    private final AuditoriaService auditoria;

    public ConfiguracionPedidoService(
            ConfiguracionPedidoRepository configuracionRepository,
            AuditoriaService auditoria) {
        this.configuracionRepository = configuracionRepository;
        this.auditoria = auditoria;
    }

    @Transactional
    public ConfiguracionPedido obtener() {
        ConfiguracionPedido config = configuracionRepository
            .findById(ConfiguracionPedido.ID_UNICO)
            .orElseGet(() -> {
                ConfiguracionPedido nueva = new ConfiguracionPedido();
                nueva.setIdConfiguracion(ConfiguracionPedido.ID_UNICO);
                nueva.setDiasAprobacionAutomatica(
                    ConfiguracionPedido.DIAS_APROBACION_POR_DEFECTO);
                nueva.setDiasPagoCuota(ConfiguracionPedido.DIAS_PAGO_POR_DEFECTO);
                nueva.setRevisionesMaximasPorHito(
                    ConfiguracionPedido.REVISIONES_MAXIMAS_POR_DEFECTO);
                nueva.setDiasReentrega(
                    ConfiguracionPedido.DIAS_REENTREGA_POR_DEFECTO);
                nueva.setMaxHitos(ConfiguracionPedido.MAX_HITOS_POR_DEFECTO);
                nueva.setPorcentajeMaxPrimerHito(
                    ConfiguracionPedido.PORCENTAJE_MAX_PRIMER_HITO_POR_DEFECTO);
                nueva.setMontoMinimoCuota(
                    ConfiguracionPedido.MONTO_MINIMO_CUOTA_POR_DEFECTO);
                nueva.setDiasGraciaEntrega(
                    ConfiguracionPedido.DIAS_GRACIA_ENTREGA_POR_DEFECTO);
                nueva.setDiasSancionVentas(
                    ConfiguracionPedido.DIAS_SANCION_VENTAS_POR_DEFECTO);
                nueva.setDiasReapelacion(
                    ConfiguracionPedido.DIAS_REAPELACION_POR_DEFECTO);
                nueva.setDiasCierreTrasBloqueo(
                    ConfiguracionPedido.DIAS_CIERRE_TRAS_BLOQUEO_POR_DEFECTO);
                nueva.setDiasGraciaPago(
                    ConfiguracionPedido.DIAS_GRACIA_PAGO_POR_DEFECTO);
                nueva.setDiasSancionCompras(
                    ConfiguracionPedido.DIAS_SANCION_COMPRAS_POR_DEFECTO);
                nueva.setHorasRespuestaProrroga(
                    ConfiguracionPedido.HORAS_RESPUESTA_PRORROGA_POR_DEFECTO);
                return configuracionRepository.save(nueva);
            });
        if (config.getSancionesAutomaticasDesde() == null) {
            config.setSancionesAutomaticasDesde(LocalDateTime.now());
            config = configuracionRepository.save(config);
        }
        return config;
    }

    @Transactional
    public PedidoConfigDTO obtenerEstado() {
        return PedidoConfigDTO.desdeConfiguracion(obtener());
    }

    @Transactional
    public PedidoConfigDTO actualizar(PedidoConfigRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La configuración de pedidos es obligatoria.");
        }
        validarRango(request.getDiasAprobacionAutomatica(), 1, 60,
            "La aprobación automática debe estar entre 1 y 60 días.");
        validarRango(request.getDiasPagoCuota(), 1, 14,
            "El plazo de pago debe estar entre 1 y 14 días.");
        validarRango(request.getRevisionesMaximasPorHito(), 1, 10,
            "El máximo de revisiones debe estar entre 1 y 10.");
        validarRango(request.getDiasReentrega(), 1, 60,
            "El plazo de reentrega debe estar entre 1 y 60 días.");
        validarRango(request.getMaxHitos(), 1, 6,
            "El máximo de hitos debe estar entre 1 y 6.");
        if (request.getDiasGraciaEntrega() != null) {
            validarRango(request.getDiasGraciaEntrega(), 1, 30,
                "La gracia de entrega debe estar entre 1 y 30 días.");
        }
        validarRango(request.getDiasSancionVentas(), 1, 60,
            "La sanción de ventas debe estar entre 1 y 60 días.");
        validarRango(request.getDiasReapelacion(), 1, 90,
            "La espera entre apelaciones debe estar entre 1 y 90 días.");
        validarRango(request.getDiasCierreTrasBloqueo(), 7, 180,
            "El cierre tras el bloqueo debe estar entre 7 y 180 días.");
        if (request.getDiasGraciaPago() != null) {
            validarRango(request.getDiasGraciaPago(), 1, 30,
                "La gracia de pago debe estar entre 1 y 30 días.");
        }
        if (request.getDiasSancionCompras() != null) {
            validarRango(request.getDiasSancionCompras(), 1, 90,
                "La sanción de compras debe estar entre 1 y 90 días.");
        }
        if (request.getHorasRespuestaProrroga() != null) {
            validarRango(request.getHorasRespuestaProrroga(), 1, 168,
                "La respuesta a una prórroga debe estar entre 1 y 168 horas.");
        }

        BigDecimal porcentajeMaximo = decimal(
            request.getPorcentajeMaxPrimerHito(),
            "El porcentaje máximo del primer hito es obligatorio.");
        if (porcentajeMaximo.compareTo(BigDecimal.ONE) < 0
                || porcentajeMaximo.compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException(
                "El porcentaje máximo del primer hito debe estar entre 1 y 100.");
        }

        BigDecimal montoMinimo = decimal(
            request.getMontoMinimoCuota(),
            "El monto mínimo por cuota es obligatorio.");
        if (montoMinimo.signum() < 0) {
            throw new IllegalArgumentException("El monto mínimo no puede ser negativo.");
        }

        ConfiguracionPedido config = obtener();
        Snapshot antes = Snapshot.de(config, CAMPOS);
        config.setDiasAprobacionAutomatica(request.getDiasAprobacionAutomatica());
        config.setDiasPagoCuota(request.getDiasPagoCuota());
        config.setRevisionesMaximasPorHito(request.getRevisionesMaximasPorHito());
        config.setDiasReentrega(request.getDiasReentrega());
        config.setMaxHitos(request.getMaxHitos());
        config.setPorcentajeMaxPrimerHito(
            porcentajeMaximo.setScale(2, RoundingMode.HALF_UP));
        config.setMontoMinimoCuota(montoMinimo.setScale(2, RoundingMode.HALF_UP));
        config.setDiasGraciaEntrega(request.getDiasGraciaEntrega() != null
            ? request.getDiasGraciaEntrega()
            : config.getDiasGraciaEntrega());
        config.setDiasSancionVentas(request.getDiasSancionVentas());
        config.setDiasReapelacion(request.getDiasReapelacion());
        config.setDiasCierreTrasBloqueo(request.getDiasCierreTrasBloqueo());
        config.setDiasGraciaPago(request.getDiasGraciaPago() != null
            ? request.getDiasGraciaPago()
            : config.getDiasGraciaPago());
        config.setDiasSancionCompras(request.getDiasSancionCompras() != null
            ? request.getDiasSancionCompras()
            : config.getDiasSancionCompras());
        config.setHorasRespuestaProrroga(
            request.getHorasRespuestaProrroga() != null
                ? request.getHorasRespuestaProrroga()
                : config.getHorasRespuestaProrroga());
        config.setFechaActualizacion(LocalDateTime.now());
        ConfiguracionPedido guardada = configuracionRepository.save(config);
        auditoria.registrar(
            AccionesAdmin.PEDIDO_CONFIG_ACTUALIZAR, ENTIDAD, guardada.getIdConfiguracion(),
            "Actualizó las reglas de pedidos e hitos", antes, guardada, CAMPOS);
        return PedidoConfigDTO.desdeConfiguracion(guardada);
    }

    private void validarRango(Integer valor, int minimo, int maximo, String mensaje) {
        if (valor == null || valor < minimo || valor > maximo) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private BigDecimal decimal(BigDecimal valor, String mensaje) {
        if (valor == null) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor;
    }
}
