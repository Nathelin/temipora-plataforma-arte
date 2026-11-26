package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Estado de cuenta del usuario logueado, con independencia del rol. Alimenta el
 * aviso global del cliente con pagos pendientes y la pantalla restringida.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiEstadoCuentaDTO {

    private boolean sancionado;
    private String alcance;
    private String motivo;
    private LocalDateTime fechaFin;
    private Long diasRestantes;
    private boolean apelacionPendiente;
    private List<DeudaClienteDTO> deudas;
    private BigDecimal totalAdeudado;
}
