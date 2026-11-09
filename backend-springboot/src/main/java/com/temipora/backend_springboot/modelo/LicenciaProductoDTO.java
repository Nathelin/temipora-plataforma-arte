package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.util.List;

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
public class LicenciaProductoDTO {

    private Long idLicenciaProducto;
    private Integer version;
    private boolean vigente;
    private List<DerechoTerminoDTO> derechos;
    private boolean permiteModificaciones;
    private boolean requiereCredito;
    private BigDecimal porcentajeRegalia;
    private String textoAdicional;
    private boolean permiteReventa;
}
