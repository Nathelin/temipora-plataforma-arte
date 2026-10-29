package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReputacionClienteDTO {

    private Long idCliente;
    private long cantidadResenias;
    private BigDecimal promedio;
    private EtiquetaReputacion etiqueta;
    private boolean hayDatos;
    private int minimoResenias;
}
