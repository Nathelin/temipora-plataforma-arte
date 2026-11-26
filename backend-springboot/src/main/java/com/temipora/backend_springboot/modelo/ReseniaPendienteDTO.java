package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

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
public class ReseniaPendienteDTO {

    private String tipo;
    private Long idPedido;
    private Long idCompra;
    private Long idProducto;
    private String titulo;
    private String contraparte;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String imagenUrl;

    private LocalDateTime fechaLimite;
}
