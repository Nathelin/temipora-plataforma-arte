package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
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
public class SaludCuentaDTO {

    private String estado;
    private Long idSancion;
    private String motivo;
    private String alcance;
    private LocalDateTime fechaFin;
    private LocalDateTime fechaEscalada;
    private Integer diasGraciaEntrega;
    private Integer diasGraciaRestantes;
    private boolean apelacionPendiente;
    private List<CatalogoPausadoDTO> pausados;
}
