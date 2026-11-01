package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.temipora.backend_springboot.modelo.HitoPresupuesto;
import com.temipora.backend_springboot.modelo.HitoPresupuestoRequest;
import com.temipora.backend_springboot.modelo.TipoHito;

/**
 * Arma el plan de pagos de un presupuesto. El tipo de cada hito se deriva de
 * su posición: el primero es la seña, el último la entrega final y los del
 * medio son cuotas. Un presupuesto sin hitos genera un único hito de entrega
 * al 100%.
 */
public final class HitoCalculadora {

    public record Limites(
            int maxHitos,
            BigDecimal porcentajeMaxPrimerHito,
            BigDecimal montoMinimoCuota) {
    }

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final int MAX_NOMBRE = 60;
    private static final int MAX_DESCRIPCION = 250;

    private HitoCalculadora() {
    }

    public static List<HitoPresupuesto> construir(
            List<HitoPresupuestoRequest> requests,
            BigDecimal precioFinal,
            BigDecimal precioFinalCobro,
            BigDecimal comisionMonto,
            BigDecimal comisionMontoCobro,
            Limites limites) {
        return construir(requests, precioFinal, precioFinalCobro,
            comisionMonto, comisionMontoCobro, BigDecimal.ZERO, limites);
    }

    /**
     * Construye el plan de pagos aplicando un descuento financiado por la
     * plataforma. El monto del cliente y la comisión bajan; el neto del
     * artista queda igual que sin descuento. El último hito absorbe el
     * redondeo para que la suma cierre exacta.
     */
    public static List<HitoPresupuesto> construir(
            List<HitoPresupuestoRequest> requests,
            BigDecimal precioFinal,
            BigDecimal precioFinalCobro,
            BigDecimal comisionMonto,
            BigDecimal comisionMontoCobro,
            BigDecimal descuentoPorcentaje,
            Limites limites) {

        List<HitoPresupuestoRequest> normalizados = normalizar(requests);
        int maxHitos = limites != null && limites.maxHitos() > 0
            ? limites.maxHitos()
            : 6;
        if (normalizados.size() > maxHitos) {
            throw new IllegalArgumentException(
                "Podés definir como máximo " + maxHitos + " hitos.");
        }

        List<BigDecimal> porcentajes = new ArrayList<>();
        BigDecimal suma = BigDecimal.ZERO;
        for (HitoPresupuestoRequest request : normalizados) {
            if (request == null || request.getPorcentaje() == null) {
                throw new IllegalArgumentException(
                    "Cada hito debe indicar su porcentaje.");
            }
            BigDecimal porcentaje = request.getPorcentaje()
                .setScale(2, RoundingMode.HALF_UP);
            if (porcentaje.signum() <= 0 || porcentaje.compareTo(CIEN) > 0) {
                throw new IllegalArgumentException(
                    "El porcentaje de cada hito debe ser mayor a 0 y hasta 100.");
            }
            porcentajes.add(porcentaje);
            suma = suma.add(porcentaje);
        }

        if (suma.compareTo(CIEN) != 0) {
            throw new IllegalArgumentException(
                "Los porcentajes de los hitos deben sumar 100%.");
        }

        if (porcentajes.size() > 1 && limites != null
                && limites.porcentajeMaxPrimerHito() != null) {
            BigDecimal primera = porcentajes.get(0);
            if (primera.compareTo(limites.porcentajeMaxPrimerHito()) > 0) {
                throw new IllegalArgumentException(
                    "La primera cuota no puede superar el "
                        + limites.porcentajeMaxPrimerHito()
                            .setScale(2, RoundingMode.HALF_UP).toPlainString()
                        + "%.");
            }
        }

        BigDecimal descuentoTotal = calcularDescuento(
            precioFinal, comisionMonto, descuentoPorcentaje);
        BigDecimal descuentoTotalCobro = calcularDescuento(
            precioFinalCobro, comisionMontoCobro, descuentoPorcentaje);

        List<HitoPresupuesto> hitos = new ArrayList<>();
        BigDecimal acumuladoMonto = BigDecimal.ZERO;
        BigDecimal acumuladoCobro = BigDecimal.ZERO;
        BigDecimal acumuladoComision = BigDecimal.ZERO;
        BigDecimal acumuladoComisionCobro = BigDecimal.ZERO;
        BigDecimal acumuladoDescuento = BigDecimal.ZERO;
        BigDecimal acumuladoDescuentoCobro = BigDecimal.ZERO;

        for (int i = 0; i < porcentajes.size(); i++) {
            BigDecimal porcentaje = porcentajes.get(i);
            boolean ultimo = i == porcentajes.size() - 1;

            BigDecimal monto = ultimo
                ? precioFinal.subtract(acumuladoMonto)
                : parte(precioFinal, porcentaje);
            BigDecimal montoCobro = ultimo
                ? precioFinalCobro.subtract(acumuladoCobro)
                : parte(precioFinalCobro, porcentaje);
            BigDecimal comision = ultimo
                ? comisionMonto.subtract(acumuladoComision)
                : parte(comisionMonto, porcentaje);
            BigDecimal comisionCobro = ultimo
                ? comisionMontoCobro.subtract(acumuladoComisionCobro)
                : parte(comisionMontoCobro, porcentaje);

            if (monto.signum() < 0 || montoCobro.signum() < 0
                    || comision.signum() < 0 || comisionCobro.signum() < 0) {
                throw new IllegalArgumentException(
                    "Los porcentajes de los hitos no son válidos.");
            }

            BigDecimal descuento = ultimo
                ? descuentoTotal.subtract(acumuladoDescuento)
                : parte(descuentoTotal, porcentaje);
            BigDecimal descuentoCobro = ultimo
                ? descuentoTotalCobro.subtract(acumuladoDescuentoCobro)
                : parte(descuentoTotalCobro, porcentaje);
            descuento = descuento.min(comision).max(BigDecimal.ZERO);
            descuentoCobro = descuentoCobro.min(comisionCobro).max(BigDecimal.ZERO);

            BigDecimal montoCliente = monto.subtract(descuento);
            BigDecimal montoCobroCliente = montoCobro.subtract(descuentoCobro);
            BigDecimal comisionCliente = comision.subtract(descuento);
            BigDecimal comisionCobroCliente = comisionCobro.subtract(descuentoCobro);
            if (montoCliente.signum() < 0 || montoCobroCliente.signum() < 0) {
                throw new IllegalArgumentException(
                    "El descuento no puede superar el importe del hito.");
            }

            if (porcentajes.size() > 1 && limites != null
                    && limites.montoMinimoCuota() != null
                    && limites.montoMinimoCuota().signum() > 0
                    && precioFinalCobro != null
                    && precioFinalCobro.signum() > 0
                    && montoCobro.compareTo(limites.montoMinimoCuota()) < 0) {
                throw new IllegalArgumentException(
                    "Cada cuota debe ser de al menos "
                        + limites.montoMinimoCuota()
                            .setScale(2, RoundingMode.HALF_UP).toPlainString()
                        + " en la moneda de cobro.");
            }

            HitoPresupuestoRequest request = normalizados.get(i);
            TipoHito tipo = tipoDe(i, porcentajes.size());
            String nombre = limpiar(request.getNombre(), MAX_NOMBRE,
                "El nombre del hito no puede superar los "
                    + MAX_NOMBRE + " caracteres.");
            if (nombre == null) {
                nombre = nombrePorDefecto(tipo, i, porcentajes.size());
            }
            String descripcion = limpiar(request.getDescripcion(), MAX_DESCRIPCION,
                "La descripción del hito no puede superar los "
                    + MAX_DESCRIPCION + " caracteres.");
            boolean requiereEntrega = resolverRequiereEntrega(
                tipo, porcentajes.size(), request.getRequiereEntrega());

            HitoPresupuesto hito = new HitoPresupuesto();
            hito.setOrden(i);
            hito.setNombre(nombre);
            hito.setDescripcion(descripcion);
            hito.setTipo(tipo);
            hito.setPorcentaje(porcentaje);
            hito.setMonto(montoCliente.setScale(2, RoundingMode.HALF_UP));
            hito.setMontoCobro(montoCobroCliente.setScale(2, RoundingMode.HALF_UP));
            hito.setComisionMonto(comisionCliente.setScale(2, RoundingMode.HALF_UP));
            hito.setComisionMontoCobro(
                comisionCobroCliente.setScale(2, RoundingMode.HALF_UP));
            hito.setMontoNeto(montoCliente.subtract(comisionCliente)
                .setScale(2, RoundingMode.HALF_UP));
            hito.setMontoNetoCobro(montoCobroCliente.subtract(comisionCobroCliente)
                .setScale(2, RoundingMode.HALF_UP));
            hito.setRequiereEntrega(requiereEntrega);
            hitos.add(hito);

            acumuladoMonto = acumuladoMonto.add(monto);
            acumuladoCobro = acumuladoCobro.add(montoCobro);
            acumuladoComision = acumuladoComision.add(comision);
            acumuladoComisionCobro = acumuladoComisionCobro.add(comisionCobro);
            acumuladoDescuento = acumuladoDescuento.add(descuento);
            acumuladoDescuentoCobro = acumuladoDescuentoCobro.add(descuentoCobro);
        }

        return hitos;
    }

    private static BigDecimal calcularDescuento(
            BigDecimal total,
            BigDecimal comision,
            BigDecimal porcentaje) {
        if (total == null || comision == null || porcentaje == null
                || porcentaje.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal limitado = porcentaje.min(CIEN);
        BigDecimal descuento = parte(total, limitado);
        return descuento.min(comision).max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private static List<HitoPresupuestoRequest> normalizar(
            List<HitoPresupuestoRequest> requests) {
        if (requests != null && !requests.isEmpty()) {
            return requests;
        }
        HitoPresupuestoRequest unico = new HitoPresupuestoRequest();
        unico.setPorcentaje(CIEN);
        return List.of(unico);
    }

    private static TipoHito tipoDe(int indice, int total) {
        if (total == 1) {
            return TipoHito.SENA;
        }
        if (indice == 0) {
            return TipoHito.SENA;
        }
        if (indice == total - 1) {
            return TipoHito.ENTREGA;
        }
        return TipoHito.CUOTA;
    }

    private static boolean resolverRequiereEntrega(
            TipoHito tipo, int total, Boolean solicitado) {
        if (tipo == TipoHito.ENTREGA) {
            return true;
        }
        if (total == 1) {
            // El pago único incluye la entrega final, que el cliente aprueba.
            return true;
        }
        if (tipo == TipoHito.SENA) {
            return solicitado == null || solicitado;
        }
        return true;
    }

    private static String nombrePorDefecto(TipoHito tipo, int indice, int total) {
        if (total == 1) {
            return "Pago único";
        }
        return switch (tipo) {
            case SENA -> "Seña";
            case ENTREGA -> "Entrega final";
            case CUOTA -> "Cuota " + (indice + 1);
        };
    }

    private static BigDecimal parte(BigDecimal total, BigDecimal porcentaje) {
        if (total == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return total.multiply(porcentaje)
            .divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    private static String limpiar(String valor, int maximo, String mensaje) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.isEmpty()) {
            return null;
        }
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException(mensaje);
        }
        return limpio;
    }
}
