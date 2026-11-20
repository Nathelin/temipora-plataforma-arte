package com.temipora.backend_springboot.servicios;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.TipoHito;

/**
 * Proyección de las fechas de un pedido.
 *
 * <p>El artista promete una duración; la plataforma proyecta una fecha. La
 * entrega prometida ({@code pedido.limiteEntrega}) se congela al confirmar y la
 * entrega prevista cuenta además los días en que el cliente demoró el pago de
 * una cuota que bloqueaba al artista.</p>
 */
public final class PedidoFechas {

    private static final int PISO_VENTANA_HORAS = 24;

    private PedidoFechas() {
    }

    /**
     * Días que el cliente demoró pagar cuotas que bloqueaban el trabajo.
     * La seña no cuenta (el reloj arranca con su pago) ni la entrega final
     * (se paga después de entregar).
     */
    public static int diasDemoraCliente(Pedido pedido) {
        if (pedido == null || pedido.getHitos() == null) {
            return 0;
        }
        int total = 0;
        for (HitoPedido hito : pedido.getHitos()) {
            if (hito == null || hito.getHitoPresupuesto() == null) {
                continue;
            }
            if (hito.getHitoPresupuesto().getTipo() != TipoHito.CUOTA) {
                continue;
            }
            if (hito.getEstadoPago() != EstadoPagoHito.PAGADO) {
                continue;
            }
            if (hito.getFechaPago() == null || hito.getFechaLimitePago() == null) {
                continue;
            }
            long dias = ChronoUnit.DAYS.between(
                hito.getFechaLimitePago().toLocalDate(),
                hito.getFechaPago().toLocalDate());
            if (dias > 0) {
                total += (int) dias;
            }
        }
        return total;
    }

    /**
     * Fecha de entrega vigente. Devuelve {@code null} si el pedido todavía no
     * arrancó (no tiene entrega prometida).
     */
    public static LocalDate entregaPrevista(Pedido pedido) {
        if (pedido == null || pedido.getLimiteEntrega() == null) {
            return null;
        }
        return pedido.getLimiteEntrega().plusDays(diasDemoraCliente(pedido));
    }

    public static boolean entregaAtrasada(Pedido pedido, LocalDate hoy) {
        if (pedido == null || hoy == null) {
            return false;
        }
        if (pedido.getEstado() != EstadoPedido.EN_CURSO
                && pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO) {
            return false;
        }
        LocalDate prevista = entregaPrevista(pedido);
        return prevista != null && hoy.isAfter(prevista);
    }

    /**
     * Plazo máximo para pagar un hito que todavía no bloquea trabajo
     * (seña o cuota). Nunca supera la entrega prevista y siempre deja al menos
     * 24 horas para pagar.
     */
    public static LocalDateTime ventanaPago(
            Pedido pedido,
            LocalDateTime ahora,
            int diasPagoCuota) {
        Objects.requireNonNull(ahora, "La fecha de referencia es obligatoria.");
        int dias = Math.max(1, diasPagoCuota);
        LocalDateTime base = ahora.plusDays(dias);

        LocalDate prevista = entregaPrevista(pedido);
        if (prevista != null) {
            LocalDateTime finDelDia = prevista.atTime(23, 59, 59);
            if (finDelDia.isBefore(base)) {
                base = finDelDia;
            }
        }

        LocalDateTime piso = ahora.plusHours(PISO_VENTANA_HORAS);
        return base.isBefore(piso) ? piso : base;
    }
}
