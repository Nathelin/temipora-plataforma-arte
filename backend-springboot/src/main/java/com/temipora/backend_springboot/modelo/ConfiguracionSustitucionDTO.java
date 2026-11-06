package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class ConfiguracionSustitucionDTO {

    private boolean habilitado;

    @Min(1)
    @Max(20)
    private int maxAlternativas;

    @Min(1)
    @Max(5)
    private int maxPorArtista;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("200.00")
    private BigDecimal rangoPrecioPct;

    @NotNull
    @DecimalMin("0.000")
    @DecimalMax("1.000")
    private BigDecimal bonusFavorito;

    @NotNull
    @DecimalMin("1.00")
    @DecimalMax("10.00")
    private BigDecimal factorTiempoMax;

    private boolean mostrarEnExplorar;

    @Min(0)
    @Max(1440)
    private int minutosDedupe;

    @Min(0)
    private int diasRetencion;

    private boolean emailCierreHabilitado;

    @Min(0)
    @Max(1440)
    private int emailCierreMinutos;

    @Min(1)
    @Max(20)
    private int emailCierreMax;

    @Min(1)
    @Max(720)
    private int emailCierreVigenciaHoras;

    public static ConfiguracionSustitucionDTO desdeEntidad(
            ConfiguracionSustitucion config) {
        return ConfiguracionSustitucionDTO.builder()
            .habilitado(config.isHabilitado())
            .maxAlternativas(config.getMaxAlternativas())
            .maxPorArtista(config.getMaxPorArtista())
            .rangoPrecioPct(config.getRangoPrecioPct())
            .bonusFavorito(config.getBonusFavorito())
            .factorTiempoMax(config.getFactorTiempoMax())
            .mostrarEnExplorar(config.isMostrarEnExplorar())
            .minutosDedupe(config.getMinutosDedupe())
            .diasRetencion(config.getDiasRetencion())
            .emailCierreHabilitado(config.isEmailCierreHabilitado())
            .emailCierreMinutos(config.getEmailCierreMinutos())
            .emailCierreMax(config.getEmailCierreMax())
            .emailCierreVigenciaHoras(config.getEmailCierreVigenciaHoras())
            .build();
    }
}
