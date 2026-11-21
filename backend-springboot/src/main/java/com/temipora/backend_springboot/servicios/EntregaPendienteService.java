package com.temipora.backend_springboot.servicios;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.modelo.EstadoEntregaHito;
import com.temipora.backend_springboot.modelo.EstadoPagoHito;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.TipoHito;

/**
 * Reglas comunes de entrega: qué le corresponde entregar al artista, para
 * cuándo, y si está en mora. Las comparten los recordatorios, el job de mora y
 * las sanciones automáticas para que no diverjan.
 */
@Service
public class EntregaPendienteService {

    /**
     * El artista todavía tiene algo por subir. Es la condición de los
     * recordatorios: un hito entregado deja de recordarse aunque no esté
     * aprobado.
     */
    public boolean artistaDebeEntregar(Pedido pedido) {
        if (pedido == null || pedido.getHitos() == null) {
            return false;
        }
        for (HitoPedido hito : pedido.getHitos()) {
            if (hito == null || hito.getHitoPresupuesto() == null
                    || !hito.getHitoPresupuesto().isRequiereEntrega()) {
                continue;
            }
            EstadoEntregaHito estado = hito.getEstadoEntrega();
            if (estado == EstadoEntregaHito.ENTREGADO
                    || estado == EstadoEntregaHito.APROBADO
                    || estado == EstadoEntregaHito.APROBADO_AUTOMATICAMENTE) {
                continue;
            }
            if (hito.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA
                    || hito.getEstadoPago() == EstadoPagoHito.PAGADO) {
                return true;
            }
        }
        return false;
    }

    /**
     * El primer hito cuya entrega le corresponde al artista y que todavía no
     * está aprobado, incluido uno ya subido pero sin aprobación.
     */
    public HitoPedido hitoExigible(Pedido pedido) {
        if (pedido == null || pedido.getHitos() == null) {
            return null;
        }
        for (HitoPedido hito : pedido.getHitos()) {
            if (esExigibleAlArtista(pedido, hito)) {
                return hito;
            }
        }
        return null;
    }

    /**
     * Un hito exige entrega del artista cuando requiere un entregable, no está
     * aprobado y el artista ya puede producirlo: la cuota está paga, o es la
     * entrega final con la etapa anterior aprobada.
     */
    public boolean esExigibleAlArtista(Pedido pedido, HitoPedido hito) {
        if (pedido == null || hito == null || hito.getHitoPresupuesto() == null
                || !hito.getHitoPresupuesto().isRequiereEntrega()) {
            return false;
        }
        if (estaAprobado(hito)) {
            return false;
        }
        if (hito.getHitoPresupuesto().getTipo() == TipoHito.ENTREGA) {
            HitoPedido anterior = hitoAnterior(pedido, hito);
            boolean anteriorOk = anterior == null || estaAprobado(anterior);
            return anteriorOk
                && hito.getEstadoPago() == EstadoPagoHito.PENDIENTE;
        }
        return hito.getEstadoPago() == EstadoPagoHito.PAGADO;
    }

    /**
     * Plazo vigente del hito. En una revisión manda el plazo de reentrega; en el
     * resto, la entrega prevista del pedido (que ya suma la demora del cliente).
     */
    public LocalDateTime plazoVigente(Pedido pedido, HitoPedido hito) {
        if (hito == null) {
            return null;
        }
        if (hito.getEstadoEntrega() == EstadoEntregaHito.CON_REVISION
                && hito.getFechaLimiteReentrega() != null) {
            return hito.getFechaLimiteReentrega();
        }
        if (!esExigibleAlArtista(pedido, hito)) {
            return null;
        }
        return plazoPrevisto(pedido);
    }

    /**
     * Plazo más antiguo entre los hitos exigibles. Es el que dispara la mora.
     */
    public LocalDateTime plazoVigente(Pedido pedido) {
        if (pedido == null || pedido.getHitos() == null) {
            return null;
        }
        LocalDateTime masAntiguo = null;
        for (HitoPedido hito : pedido.getHitos()) {
            LocalDateTime plazo = plazoVigente(pedido, hito);
            if (plazo == null) {
                continue;
            }
            if (masAntiguo == null || plazo.isBefore(masAntiguo)) {
                masAntiguo = plazo;
            }
        }
        return masAntiguo;
    }

    public boolean estaEnMora(Pedido pedido, LocalDateTime ahora) {
        LocalDateTime plazo = plazoVigente(pedido);
        return plazo != null && ahora.isAfter(plazo);
    }

    public LocalDateTime plazoPrevisto(Pedido pedido) {
        if (pedido == null) {
            return null;
        }
        LocalDate prevista = PedidoFechas.entregaPrevista(pedido);
        return prevista != null ? prevista.atTime(23, 59, 59) : null;
    }

    private boolean estaAprobado(HitoPedido hito) {
        return hito.getEstadoEntrega() == EstadoEntregaHito.APROBADO
            || hito.getEstadoEntrega()
                == EstadoEntregaHito.APROBADO_AUTOMATICAMENTE;
    }

    private HitoPedido hitoAnterior(Pedido pedido, HitoPedido hito) {
        if (hito.getOrden() == null || hito.getOrden() <= 0) {
            return null;
        }
        int orden = hito.getOrden() - 1;
        return pedido.getHitos().stream()
            .filter(candidato -> candidato.getOrden() != null
                && candidato.getOrden() == orden)
            .findFirst()
            .orElse(null);
    }
}
