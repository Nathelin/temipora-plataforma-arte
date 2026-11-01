package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
public class ConfiguracionInactividadDTO {

    @Min(0)
    private int diasSinCompra;

    @Min(0)
    private int diasSinConexion;

    @Min(0)
    private int diasCooldown;

    @Min(0)
    private int diasMinimosCuenta;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("50.00")
    private BigDecimal porcentajeDescuento;

    @Min(1)
    private int vigenciaHoras;

    @Min(1)
    private int maxOfertasPorUsuario;

    @Min(1)
    private int maxUsuariosPorCorrida;

    public static ConfiguracionInactividadDTO desdeEntidad(
            ConfiguracionInactividad config) {
        return ConfiguracionInactividadDTO.builder()
            .diasSinCompra(config.getDiasSinCompra())
            .diasSinConexion(config.getDiasSinConexion())
            .diasCooldown(config.getDiasCooldown())
            .diasMinimosCuenta(config.getDiasMinimosCuenta())
            .porcentajeDescuento(config.getPorcentajeDescuento())
            .vigenciaHoras(config.getVigenciaHoras())
            .maxOfertasPorUsuario(config.getMaxOfertasPorUsuario())
            .maxUsuariosPorCorrida(config.getMaxUsuariosPorCorrida())
            .build();
    }
}
