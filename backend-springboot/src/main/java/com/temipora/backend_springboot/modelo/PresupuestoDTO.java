package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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

    // Importe congelado en la moneda de cobro (ARS) al momento de emitir.
    private String monedaCobro;
    private BigDecimal cotizacionCobro;
    private BigDecimal precioBaseTotalCobro;
    private BigDecimal precioFinalCobro;

    // Importes listos para mostrar según la preferencia del usuario.
    private BigDecimal precioBaseTotalMostrado;
    private BigDecimal precioFinalMostrado;
    private String monedaMostrada;

    private Integer tiempoEntregaValor;
    private String tiempoEntregaUnidad;
    private Integer tiempoEntregaDias;
    private LocalDate fechaInicioEstimada;
    private String mensaje;
    private LocalDateTime fechaEmision;
    private LocalDateTime fechaVencimiento;
    private List<ItemPresupuestoDTO> items;

    public static PresupuestoDTO desdePresupuesto(Presupuesto presupuesto) {
        return desdePresupuesto(presupuesto, null, null, null);
    }

    /**
     * @param monedaDestino moneda preferida por el usuario que mira el presupuesto.
     * @param factorOrigenDestino factor de la moneda nativa del presupuesto a la
     *        moneda destino (se usa como respaldo si no hay snapshot de cobro).
     * @param factorCobroDestino factor de la moneda de cobro a la moneda destino.
     */
    public static PresupuestoDTO desdePresupuesto(
            Presupuesto presupuesto,
            Moneda monedaDestino,
            BigDecimal factorOrigenDestino,
            BigDecimal factorCobroDestino) {
        Moneda origen = presupuesto.getMoneda();
        Moneda cobro = presupuesto.getMonedaCobro();
        Moneda destino = monedaDestino != null ? monedaDestino : origen;

        boolean muestraOrigen = mismaMoneda(destino, origen);
        boolean muestraCobro = !muestraOrigen && mismaMoneda(destino, cobro);
        String codigoMostrado = destino != null
            ? destino.getCodigo()
            : (origen != null ? origen.getCodigo() : null);

        BigDecimal baseMostrado = totalMostrado(
            presupuesto.getPrecioBaseTotal(),
            presupuesto.getPrecioBaseTotalCobro(),
            muestraOrigen,
            muestraCobro,
            factorCobroDestino,
            factorOrigenDestino);
        BigDecimal finalMostrado = totalMostrado(
            presupuesto.getPrecioFinal(),
            presupuesto.getPrecioFinalCobro(),
            muestraOrigen,
            muestraCobro,
            factorCobroDestino,
            factorOrigenDestino);

        BigDecimal factorItem = factorItem(
            presupuesto,
            muestraOrigen,
            muestraCobro,
            factorCobroDestino,
            factorOrigenDestino);

        return PresupuestoDTO.builder()
            .idPresupuesto(presupuesto.getIdPresupuesto())
            .precioBaseTotal(presupuesto.getPrecioBaseTotal())
            .precioFinal(presupuesto.getPrecioFinal())
            .moneda(origen != null ? origen.getCodigo() : null)
            .monedaCobro(cobro != null ? cobro.getCodigo() : null)
            .cotizacionCobro(presupuesto.getCotizacionCobro())
            .precioBaseTotalCobro(presupuesto.getPrecioBaseTotalCobro())
            .precioFinalCobro(presupuesto.getPrecioFinalCobro())
            .precioBaseTotalMostrado(baseMostrado)
            .precioFinalMostrado(finalMostrado)
            .monedaMostrada(codigoMostrado)
            .tiempoEntregaValor(presupuesto.getTiempoEntregaValor())
            .tiempoEntregaUnidad(presupuesto.getTiempoEntregaUnidad() != null
                ? presupuesto.getTiempoEntregaUnidad().name()
                : null)
            .tiempoEntregaDias(presupuesto.getTiempoEntregaDias())
            .fechaInicioEstimada(presupuesto.getFechaInicioEstimada())
            .mensaje(presupuesto.getMensaje())
            .fechaEmision(presupuesto.getFechaEmision())
            .fechaVencimiento(presupuesto.getFechaVencimiento())
            .items(presupuesto.getItems().stream()
                .map(item -> ItemPresupuestoDTO.desdeItem(item, factorItem))
                .toList())
            .build();
    }

    private static BigDecimal totalMostrado(
            BigDecimal nativo,
            BigDecimal cobro,
            boolean muestraOrigen,
            boolean muestraCobro,
            BigDecimal factorCobroDestino,
            BigDecimal factorOrigenDestino) {
        if (muestraOrigen) {
            return nativo;
        }
        if (muestraCobro && cobro != null) {
            return cobro;
        }
        if (cobro != null && factorCobroDestino != null) {
            return convertir(cobro, factorCobroDestino);
        }
        if (factorOrigenDestino != null) {
            return convertir(nativo, factorOrigenDestino);
        }
        return nativo;
    }

    private static BigDecimal factorItem(
            Presupuesto presupuesto,
            boolean muestraOrigen,
            boolean muestraCobro,
            BigDecimal factorCobroDestino,
            BigDecimal factorOrigenDestino) {
        if (muestraOrigen) {
            return BigDecimal.ONE;
        }
        BigDecimal cotizacion = presupuesto.getCotizacionCobro();
        if (muestraCobro && cotizacion != null) {
            return cotizacion;
        }
        if (cotizacion != null && factorCobroDestino != null) {
            return cotizacion.multiply(factorCobroDestino);
        }
        if (factorOrigenDestino != null) {
            return factorOrigenDestino;
        }
        return BigDecimal.ONE;
    }

    static BigDecimal convertir(BigDecimal monto, BigDecimal factor) {
        if (monto == null || factor == null) {
            return monto;
        }
        return monto.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    static boolean mismaMoneda(Moneda a, Moneda b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.getIdMoneda() != null && b.getIdMoneda() != null) {
            return a.getIdMoneda().equals(b.getIdMoneda());
        }
        return Objects.equals(a.getCodigo(), b.getCodigo());
    }
}
