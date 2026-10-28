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
public class PagoDTO {

    private Long idPago;
    private String estado;
    private BigDecimal monto;
    private BigDecimal montoCobro;
    private String moneda;
    private String monedaCobro;
    private String proveedor;
    private String instruccion;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaAcreditacion;

    public static PagoDTO desdePago(Pago pago, boolean incluirInstruccion) {
        boolean pendiente = pago.getEstado() == EstadoPago.PENDIENTE;
        return PagoDTO.builder()
            .idPago(pago.getIdPago())
            .estado(pago.getEstado() != null ? pago.getEstado().name() : null)
            .monto(pago.getMonto())
            .montoCobro(pago.getMontoCobro())
            .moneda(pago.getMoneda() != null ? pago.getMoneda().getCodigo() : null)
            .monedaCobro(pago.getMonedaCobro() != null
                ? pago.getMonedaCobro().getCodigo()
                : null)
            .proveedor(pago.getProveedor())
            .instruccion(incluirInstruccion && pendiente ? pago.getPayload() : null)
            .fechaCreacion(pago.getFechaCreacion())
            .fechaAcreditacion(pago.getFechaAcreditacion())
            .build();
    }
}
