package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionCotizacion;
import com.temipora.backend_springboot.modelo.ConversionMoneda;
import com.temipora.backend_springboot.modelo.CotizacionConfigDTO;
import com.temipora.backend_springboot.modelo.CotizacionConfigRequest;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.repositorios.ConfiguracionCotizacionRepository;

@Service
public class CotizacionService {

    private static final Logger log = LoggerFactory.getLogger(CotizacionService.class);
    private static final String ORIGEN_API = "API";
    private static final String ORIGEN_MANUAL = "MANUAL";
    private static final String CODIGO_ORIGEN_PREFERIDO = "USD";

    private static final String ENTIDAD = "ConfiguracionCotizacion";
    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("apiHabilitada", "Actualización por API"),
        CampoAuditado.de("monedaOrigen", "Moneda de origen"),
        CampoAuditado.de("monedaDestino", "Moneda de destino"),
        CampoAuditado.de("factorManual", "Factor manual"));

    private final ConfiguracionCotizacionRepository configuracionRepository;
    private final ConversionMonedaService conversionMonedaService;
    private final MonedaService monedaService;
    private final CotizacionApiClient apiClient;
    private final AuditoriaService auditoria;

    public CotizacionService(
            ConfiguracionCotizacionRepository configuracionRepository,
            ConversionMonedaService conversionMonedaService,
            MonedaService monedaService,
            CotizacionApiClient apiClient,
            AuditoriaService auditoria) {
        this.configuracionRepository = configuracionRepository;
        this.conversionMonedaService = conversionMonedaService;
        this.monedaService = monedaService;
        this.apiClient = apiClient;
        this.auditoria = auditoria;
    }

    @Transactional
    public ConfiguracionCotizacion obtenerConfiguracion() {
        return configuracionRepository.findById(ConfiguracionCotizacion.ID_UNICO)
            .orElseGet(() -> {
                ConfiguracionCotizacion config = new ConfiguracionCotizacion();
                config.setIdConfiguracion(ConfiguracionCotizacion.ID_UNICO);
                aplicarMonedasPorDefecto(config);
                return configuracionRepository.save(config);
            });
    }

    @Transactional
    public CotizacionConfigDTO obtenerEstado() {
        ConfiguracionCotizacion config = obtenerConfiguracion();
        return aDTO(config);
    }

    @Transactional
    public CotizacionConfigDTO actualizar(CotizacionConfigRequest request) {
        Moneda origen = monedaService.obtener(request.getIdMonedaOrigen());
        Moneda destino = monedaService.obtener(request.getIdMonedaDestino());
        if (origen.getIdMoneda().equals(destino.getIdMoneda())) {
            throw new IllegalArgumentException("Las monedas de origen y destino deben ser distintas.");
        }

        ConfiguracionCotizacion config = obtenerConfiguracion();
        Snapshot antes = Snapshot.de(config, CAMPOS);
        config.setApiHabilitada(Boolean.TRUE.equals(request.getApiHabilitada()));
        config.setMonedaOrigen(origen);
        config.setMonedaDestino(destino);
        config.setFactorManual(request.getFactorManual());

        if (request.getFactorManual() != null) {
            conversionMonedaService.registrarFactor(
                origen,
                destino,
                request.getFactorManual(),
                ORIGEN_MANUAL,
                LocalDateTime.now());
        }

        config.setFechaActualizacion(LocalDateTime.now());
        ConfiguracionCotizacion guardada = configuracionRepository.save(config);
        auditoria.registrar(
            AccionesAdmin.COTIZACION_ACTUALIZAR, ENTIDAD, guardada.getIdConfiguracion(),
            "Actualizó la cotización", antes, guardada, CAMPOS);
        return aDTO(guardada);
    }

    @Transactional
    public CotizacionConfigDTO refrescarDesdeApi() {
        ConfiguracionCotizacion config = obtenerConfiguracion();
        if (!config.isApiHabilitada()) {
            throw new IllegalStateException(
                "La actualización automática desde la API está deshabilitada.");
        }

        BigDecimal factor = consultarFactorApi();
        conversionMonedaService.registrarFactor(
            config.getMonedaOrigen(),
            config.getMonedaDestino(),
            factor,
            ORIGEN_API,
            LocalDateTime.now());

        config.setFechaActualizacion(LocalDateTime.now());
        ConfiguracionCotizacion guardada = configuracionRepository.save(config);
        auditoria.registrar(
            AccionesAdmin.COTIZACION_REFRESCAR, ENTIDAD, guardada.getIdConfiguracion(),
            "Actualizó la cotización desde la API (factor " + factor + ")",
            "Actualización desde la API");
        return aDTO(guardada);
    }

    public BigDecimal consultarFactorApi() {
        BigDecimal factor = apiClient.obtenerFactorVenta();
        log.info("Cotización obtenida desde la API: {}", factor);
        return factor;
    }

    private void aplicarMonedasPorDefecto(ConfiguracionCotizacion config) {
        Moneda destino = monedaService.predeterminada();
        if (destino == null) {
            List<Moneda> activas = monedaService.activas();
            if (activas.isEmpty()) {
                throw new IllegalStateException(
                    "No hay monedas configuradas. Creá al menos una moneda antes de configurar la cotización.");
            }
            destino = activas.get(0);
        }
        final Moneda destinoFinal = destino;

        Moneda origen = monedaService.obtenerPorCodigo(CODIGO_ORIGEN_PREFERIDO);
        if (origen == null || origen.getIdMoneda().equals(destinoFinal.getIdMoneda())) {
            origen = monedaService.activas().stream()
                .filter(m -> !m.getIdMoneda().equals(destinoFinal.getIdMoneda()))
                .findFirst()
                .orElse(destinoFinal);
        }
        config.setMonedaOrigen(origen);
        config.setMonedaDestino(destinoFinal);
    }

    private CotizacionConfigDTO aDTO(ConfiguracionCotizacion config) {
        ConversionMoneda vigente = conversionMonedaService.factorAbierto(
            config.getMonedaOrigen(), config.getMonedaDestino());
        return CotizacionConfigDTO.desde(config, vigente);
    }
}
