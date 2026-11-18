package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Licencia tal como quedó congelada en la compra. No se lee de la entidad
 * actual: se parsea el snapshot aceptado al momento de pagar.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenciaCompraDTO {

    private Long idLicenciaProducto;
    private Integer version;
    private boolean permiteModificaciones;
    private boolean requiereCredito;
    private BigDecimal porcentajeRegalia;
    private String textoAdicional;

    @Builder.Default
    private List<DerechoCompraDTO> derechos = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DerechoCompraDTO {
        private Long idDerechoTermino;
        private String codigo;
        private String nombre;
    }
}
