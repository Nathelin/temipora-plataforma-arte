package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
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
public class PresupuestoDTO {

    private Long idPresupuesto;
    private BigDecimal precioBaseTotal;
    private BigDecimal precioFinal;
    private String moneda;
    private Integer tiempoEntregaValor;
    private String tiempoEntregaUnidad;
    private Integer tiempoEntregaDias;
    private String mensaje;
    private LocalDateTime fechaEmision;
    private LocalDateTime fechaVencimiento;
    private List<ItemPresupuestoDTO> items;

    public static PresupuestoDTO desdePresupuesto(Presupuesto presupuesto) {
        return PresupuestoDTO.builder()
            .idPresupuesto(presupuesto.getIdPresupuesto())
            .precioBaseTotal(presupuesto.getPrecioBaseTotal())
            .precioFinal(presupuesto.getPrecioFinal())
            .moneda(presupuesto.getMoneda() != null
                ? presupuesto.getMoneda().getCodigo()
                : null)
            .tiempoEntregaValor(presupuesto.getTiempoEntregaValor())
            .tiempoEntregaUnidad(presupuesto.getTiempoEntregaUnidad() != null
                ? presupuesto.getTiempoEntregaUnidad().name()
                : null)
            .tiempoEntregaDias(presupuesto.getTiempoEntregaDias())
            .mensaje(presupuesto.getMensaje())
            .fechaEmision(presupuesto.getFechaEmision())
            .fechaVencimiento(presupuesto.getFechaVencimiento())
            .items(presupuesto.getItems().stream()
                .map(ItemPresupuestoDTO::desdeItem)
                .toList())
            .build();
    }
}
