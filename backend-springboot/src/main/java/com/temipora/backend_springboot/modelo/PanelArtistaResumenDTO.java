package com.temipora.backend_springboot.modelo;

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
public class PanelArtistaResumenDTO {

    private MetricasPanelDTO metricas;
    private DineroPanelDTO dinero;
    private long mensajesNoLeidos;
    private long solicitudesMensajePendientes;
    private boolean cuentaCobrosConectada;
    private SaludCuentaDTO saludCuenta;

    private List<PedidoResumenDTO> pedidosActivos;
    private List<HitoPedidoDTO> hitosAtencion;
    private List<SolicitudResumenDTO> solicitudesRecientes;
    private List<SolicitudResumenDTO> solicitudesPorResponder;
    private List<SolicitudResumenDTO> presupuestosPorVencer;
}
