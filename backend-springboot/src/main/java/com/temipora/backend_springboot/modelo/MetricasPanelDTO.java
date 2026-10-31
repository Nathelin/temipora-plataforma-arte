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
public class MetricasPanelDTO {

    private long pedidosEnCurso;
    private long pedidosPendientesPago;
    private long pedidosImpagos;
    private long entregasPendientes;
    private long entregasConRevision;
    private long solicitudesPorResponder;
    private long solicitudesEnEspera;
    private long presupuestosPorVencer;
    private long hitosPorVencer;
}
