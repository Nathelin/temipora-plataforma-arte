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
        CampoAuditado.de("maxHitos", "Máximo de hitos"),
        CampoAuditado.de("porcentajeMaxPrimerHito", "Porcentaje máximo de la primera cuota"),
        CampoAuditado.de("montoMinimoCuota", "Monto mínimo por cuota"));

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
        return configuracionRepository.findById(ConfiguracionPedido.ID_UNICO)
            .orElseGet(() -> {
                ConfiguracionPedido config = new ConfiguracionPedido();
                config.setIdConfiguracion(ConfiguracionPedido.ID_UNICO);
                config.setDiasAprobacionAutomatica(
                    ConfiguracionPedido.DIAS_APROBACION_POR_DEFECTO);
                config.setDiasPagoCuota(ConfiguracionPedido.DIAS_PAGO_POR_DEFECTO);
                config.setMaxHitos(ConfiguracionPedido.MAX_HITOS_POR_DEFECTO);
                config.setPorcentajeMaxPrimerHito(
                    ConfiguracionPedido.PORCENTAJE_MAX_PRIMER_HITO_POR_DEFECTO);
                config.setMontoMinimoCuota(
                    ConfiguracionPedido.MONTO_MINIMO_CUOTA_POR_DEFECTO);
                return configuracionRepository.save(config);
            });
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
        validarRango(request.getMaxHitos(), 1, 6,
            "El máximo de hitos debe estar entre 1 y 6.");

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
        config.setMaxHitos(request.getMaxHitos());
        config.setPorcentajeMaxPrimerHito(
            porcentajeMaximo.setScale(2, RoundingMode.HALF_UP));
        config.setMontoMinimoCuota(montoMinimo.setScale(2, RoundingMode.HALF_UP));
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
