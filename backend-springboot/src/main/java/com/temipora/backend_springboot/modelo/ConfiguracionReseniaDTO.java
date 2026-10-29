package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionReseniaDTO {

    private Integer diasVentanaCreacion;
    private Integer diasEdicion;
    private Integer minimoReseniasEtiqueta;
    private Integer maxImagenesResenia;
    private BigDecimal umbralExcelente;
    private BigDecimal umbralBuena;
    private LocalDateTime fechaActualizacion;

    public static ConfiguracionReseniaDTO desdeConfiguracion(
            ConfiguracionResenia config) {
        return ConfiguracionReseniaDTO.builder()
            .diasVentanaCreacion(config.getDiasVentanaCreacion())
            .diasEdicion(config.getDiasEdicion())
            .minimoReseniasEtiqueta(config.getMinimoReseniasEtiqueta())
            .maxImagenesResenia(config.getMaxImagenesResenia())
            .umbralExcelente(config.getUmbralExcelente())
            .umbralBuena(config.getUmbralBuena())
            .fechaActualizacion(config.getFechaActualizacion())
            .build();
    }
}
