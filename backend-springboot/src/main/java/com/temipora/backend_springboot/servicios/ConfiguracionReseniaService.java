package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionResenia;
import com.temipora.backend_springboot.modelo.ConfiguracionReseniaDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionReseniaRequest;
import com.temipora.backend_springboot.repositorios.ConfiguracionReseniaRepository;

@Service
public class ConfiguracionReseniaService {

    private final ConfiguracionReseniaRepository configuracionRepository;

    public ConfiguracionReseniaService(
            ConfiguracionReseniaRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    @Transactional
    public ConfiguracionResenia obtener() {
        return configuracionRepository.findById(ConfiguracionResenia.ID_UNICO)
            .orElseGet(() -> {
                ConfiguracionResenia config = new ConfiguracionResenia();
                config.setIdConfiguracion(ConfiguracionResenia.ID_UNICO);
                config.setDiasVentanaCreacion(
                    ConfiguracionResenia.DIAS_VENTANA_CREACION_POR_DEFECTO);
                config.setDiasEdicion(
                    ConfiguracionResenia.DIAS_EDICION_POR_DEFECTO);
                config.setMinimoReseniasEtiqueta(
                    ConfiguracionResenia.MINIMO_RESENIAS_ETIQUETA_POR_DEFECTO);
                config.setMaxImagenesResenia(
                    ConfiguracionResenia.MAX_IMAGENES_RESENIA_POR_DEFECTO);
                config.setUmbralExcelente(
                    ConfiguracionResenia.UMBRAL_EXCELENTE_POR_DEFECTO);
                config.setUmbralBuena(
                    ConfiguracionResenia.UMBRAL_BUENA_POR_DEFECTO);
                return configuracionRepository.save(config);
            });
    }

    @Transactional
    public ConfiguracionReseniaDTO obtenerEstado() {
        return ConfiguracionReseniaDTO.desdeConfiguracion(obtener());
    }

    @Transactional
    public ConfiguracionReseniaDTO actualizar(ConfiguracionReseniaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                "La configuración de reseñas es obligatoria.");
        }
        validarRango(request.getDiasVentanaCreacion(), 0, 365,
            "La ventana para dejar una reseña debe estar entre 0 y 365 días.");
        validarRango(request.getDiasEdicion(), 1, 30,
            "La ventana de edición debe estar entre 1 y 30 días.");
        validarRango(request.getMinimoReseniasEtiqueta(), 1, 50,
            "El mínimo de reseñas para mostrar la etiqueta debe estar entre 1 y 50.");
        validarRango(request.getMaxImagenesResenia(), 0, 10,
            "El máximo de imágenes por reseña debe estar entre 0 y 10.");

        BigDecimal excelente = decimal(request.getUmbralExcelente(),
            "El umbral de excelencia es obligatorio.");
        BigDecimal buena = decimal(request.getUmbralBuena(),
            "El umbral de buena reputación es obligatorio.");
        validarUmbral(excelente, "El umbral de excelencia");
        validarUmbral(buena, "El umbral de buena reputación");

        if (excelente.compareTo(buena) <= 0) {
            throw new IllegalArgumentException(
                "El umbral de excelencia debe ser mayor que el de buena reputación.");
        }

        ConfiguracionResenia config = obtener();
        config.setDiasVentanaCreacion(request.getDiasVentanaCreacion());
        config.setDiasEdicion(request.getDiasEdicion());
        config.setMinimoReseniasEtiqueta(request.getMinimoReseniasEtiqueta());
        config.setMaxImagenesResenia(request.getMaxImagenesResenia());
        config.setUmbralExcelente(excelente.setScale(2, RoundingMode.HALF_UP));
        config.setUmbralBuena(buena.setScale(2, RoundingMode.HALF_UP));
        config.setFechaActualizacion(LocalDateTime.now());

        return ConfiguracionReseniaDTO.desdeConfiguracion(
            configuracionRepository.save(config));
    }

    private void validarRango(
            Integer valor, int minimo, int maximo, String mensaje) {
        if (valor == null || valor < minimo || valor > maximo) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private void validarUmbral(BigDecimal valor, String etiqueta) {
        if (valor.compareTo(BigDecimal.ONE) < 0
                || valor.compareTo(new BigDecimal("5.00")) > 0) {
            throw new IllegalArgumentException(
                etiqueta + " debe estar entre 1.00 y 5.00.");
        }
    }

    private BigDecimal decimal(BigDecimal valor, String mensaje) {
        if (valor == null) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor;
    }
}
