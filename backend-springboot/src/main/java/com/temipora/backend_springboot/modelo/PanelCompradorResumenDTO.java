package com.temipora.backend_springboot.modelo;

import java.util.List;

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
public class PanelCompradorResumenDTO {

    private MetricasCompradorDTO metricas;
    private DineroCompradorDTO dinero;
    private long mensajesNoLeidos;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MiEstadoCuentaDTO estadoCuenta;

    private List<PedidoResumenDTO> pedidosActivos;
    private List<HitoPedidoDTO> hitosAtencion;
    private List<CompraResumenDTO> comprasPendientes;
    private List<SolicitudResumenDTO> presupuestosRecibidos;
    private List<ReseniaPendienteDTO> reseniasPorDejar;
}
