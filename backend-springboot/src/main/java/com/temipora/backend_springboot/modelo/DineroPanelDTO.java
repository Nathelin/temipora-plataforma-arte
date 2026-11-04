package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

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
public class DineroPanelDTO {

    private BigDecimal porCobrar;
    private BigDecimal cobradoMes;
    private BigDecimal cobradoTotal;
    private BigDecimal pendienteAcreditar;
    private String monedaMostrada;
}
