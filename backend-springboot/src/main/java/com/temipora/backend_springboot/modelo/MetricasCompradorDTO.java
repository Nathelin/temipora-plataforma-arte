package com.temipora.backend_springboot.modelo;

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
public class MetricasCompradorDTO {

    private long pedidosEnCurso;
    private long pedidosPendientesPago;
    private long entregasPorAprobar;
    private long pagosPendientes;
    private long pagosVencidos;
    private long comprasPendientes;
    private long presupuestosRecibidos;
    private long presupuestosPorVencer;
    private long reseniasPendientes;
}
