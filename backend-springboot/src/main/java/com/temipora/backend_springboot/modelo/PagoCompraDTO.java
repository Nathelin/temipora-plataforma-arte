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
public class PagoCompraDTO {

    private Long idIntentoCobro;
    private Long idCompra;
    private Long idCompraGrupo;
    private String proveedor;
    private EstadoIntentoCobro estado;
    private String instruccion;
    private String idPagoExterno;
    private BigDecimal monto;
    private String moneda;
    private LocalDateTime expiraEn;
}
