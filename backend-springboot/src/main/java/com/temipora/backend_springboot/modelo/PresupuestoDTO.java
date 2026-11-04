package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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

    // Descuento de reactivación congelado en el presupuesto.
    private BigDecimal descuentoPorcentaje;
    private BigDecimal descuentoMonto;
    private BigDecimal descuentoMontoCobro;
    private BigDecimal descuentoMostrado;
    private BigDecimal precioFinalCliente;
    private BigDecimal precioFinalClienteCobro;
    private BigDecimal precioFinalClienteMostrado;

    // Comisión de la plataforma, descontada del precio del artista.
    // Solo se envía al artista dueño o a un admin: el cliente no debe verla.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal comisionPorcentaje;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal comisionMonto;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal comisionMontoCobro;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal comisionMontoMostrado;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNetoArtista;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNetoArtistaCobro;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNetoArtistaMostrado;

    // Arancel estimado de MercadoPago, informado solo al artista/admin.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal arancelMpPorcentaje;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal arancelMpEstimado;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal arancelMpEstimadoCobro;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal arancelMpEstimadoMostrado;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNetoArtistaConArancel;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNetoArtistaConArancelCobro;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal montoNetoArtistaConArancelMostrado;

    private Integer tiempoEntregaValor;
    private String tiempoEntregaUnidad;
    private Integer tiempoEntregaDias;
    private LocalDate fechaInicioEstimada;
    private String mensaje;
    private LocalDateTime fechaEmision;
    private LocalDateTime fechaVencimiento;
    private List<ItemPresupuestoDTO> items;
    private List<HitoPresupuestoDTO> hitos;

    public static PresupuestoDTO desdePresupuesto(Presupuesto presupuesto) {
        return desdePresupuesto(presupuesto, null, null, null, false);
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
        return desdePresupuesto(
            presupuesto, monedaDestino, factorOrigenDestino, factorCobroDestino, false);
    }

    /**
     * @param incluirComision true solo para el artista dueño o un admin. El
     *        cliente no debe recibir la comisión ni el neto del artista.
     */
    public static PresupuestoDTO desdePresupuesto(
            Presupuesto presupuesto,
            Moneda monedaDestino,
            BigDecimal factorOrigenDestino,
            BigDecimal factorCobroDestino,
            boolean incluirComision) {
        Moneda origen = presupuesto.getMoneda();
        Moneda cobro = presupuesto.getMonedaCobro();
        Moneda destino = monedaDestino != null ? monedaDestino : origen;

        // Compatibilidad con presupuestos creados antes de la V58: si el
        // importe del cliente quedó en cero, se usa el bruto del artista.
        BigDecimal finalCliente = presupuesto.getPrecioFinalCliente();
        if (finalCliente == null || finalCliente.signum() == 0) {
            finalCliente = presupuesto.getPrecioFinal();
        }
        BigDecimal finalClienteCobro = presupuesto.getPrecioFinalClienteCobro();
        if (finalClienteCobro == null || finalClienteCobro.signum() == 0) {
            finalClienteCobro = presupuesto.getPrecioFinalCobro();
        }

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
        BigDecimal descuentoMostrado = totalMostrado(
            presupuesto.getDescuentoMonto(),
            presupuesto.getDescuentoMontoCobro(),
            muestraOrigen,
            muestraCobro,
            factorCobroDestino,
            factorOrigenDestino);
        BigDecimal finalClienteMostrado = totalMostrado(
            finalCliente,
            finalClienteCobro,
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

        BigDecimal comisionMostrada = incluirComision
            ? totalMostrado(
                presupuesto.getComisionMonto(),
                presupuesto.getComisionMontoCobro(),
                muestraOrigen,
                muestraCobro,
                factorCobroDestino,
                factorOrigenDestino)
            : null;
        BigDecimal netoMostrado = incluirComision
            ? totalMostrado(
                presupuesto.getMontoNetoArtista(),
                presupuesto.getMontoNetoArtistaCobro(),
                muestraOrigen,
                muestraCobro,
                factorCobroDestino,
                factorOrigenDestino)
            : null;

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
            .descuentoPorcentaje(presupuesto.getDescuentoPorcentaje())
            .descuentoMonto(presupuesto.getDescuentoMonto())
            .descuentoMontoCobro(presupuesto.getDescuentoMontoCobro())
            .descuentoMostrado(descuentoMostrado)
            .precioFinalCliente(finalCliente)
            .precioFinalClienteCobro(finalClienteCobro)
            .precioFinalClienteMostrado(finalClienteMostrado)
            .comisionPorcentaje(incluirComision
                ? presupuesto.getComisionPorcentaje() : null)
            .comisionMonto(incluirComision
                ? presupuesto.getComisionMonto() : null)
            .comisionMontoCobro(incluirComision
                ? presupuesto.getComisionMontoCobro() : null)
            .comisionMontoMostrado(comisionMostrada)
            .montoNetoArtista(incluirComision
                ? presupuesto.getMontoNetoArtista() : null)
            .montoNetoArtistaCobro(incluirComision
                ? presupuesto.getMontoNetoArtistaCobro() : null)
            .montoNetoArtistaMostrado(netoMostrado)
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
            .hitos(presupuesto.getHitos().stream()
                .map(hito -> HitoPresupuestoDTO.desdeHito(
                    hito,
                    muestraOrigen,
                    muestraCobro,
                    factorCobroDestino,
                    factorOrigenDestino,
                    incluirComision))
                .toList())
            .build();
    }

    public void aplicarArancelMp(BigDecimal porcentajeTotal) {
        if (porcentajeTotal == null || porcentajeTotal.signum() < 0) {
            return;
        }

        BigDecimal base = precioFinalCliente != null
            ? precioFinalCliente
            : precioFinal;
        BigDecimal baseCobro = precioFinalClienteCobro != null
            ? precioFinalClienteCobro
            : precioFinalCobro;
        BigDecimal baseMostrado = precioFinalClienteMostrado != null
            ? precioFinalClienteMostrado
            : precioFinalMostrado;

        this.arancelMpPorcentaje = porcentajeTotal.setScale(4, RoundingMode.HALF_UP);
        this.arancelMpEstimado = parte(base, porcentajeTotal);
        this.arancelMpEstimadoCobro = parte(baseCobro, porcentajeTotal);
        this.arancelMpEstimadoMostrado = parte(baseMostrado, porcentajeTotal);
        this.montoNetoArtistaConArancel = restar(
            montoNetoArtista, arancelMpEstimado);
        this.montoNetoArtistaConArancelCobro = restar(
            montoNetoArtistaCobro, arancelMpEstimadoCobro);
        this.montoNetoArtistaConArancelMostrado = restar(
            montoNetoArtistaMostrado, arancelMpEstimadoMostrado);

        if (hitos != null) {
            for (HitoPresupuestoDTO hito : hitos) {
                hito.aplicarArancelMp(porcentajeTotal);
            }
        }
    }

    static BigDecimal parte(BigDecimal base, BigDecimal porcentaje) {
        if (base == null) {
            return null;
        }
        return base.multiply(porcentaje)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    static BigDecimal restar(BigDecimal total, BigDecimal parte) {
        if (total == null || parte == null) {
            return total;
        }
        return total.subtract(parte)
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal totalMostrado(
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

    public static boolean mismaMoneda(Moneda a, Moneda b) {
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
