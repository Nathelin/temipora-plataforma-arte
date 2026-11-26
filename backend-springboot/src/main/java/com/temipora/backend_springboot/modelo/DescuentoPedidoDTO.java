package com.temipora.backend_springboot.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Un pedido con descuento de reactivación aplicado. Importes en la moneda de
 * cobro del presupuesto (hoy ARS). La fila es siempre un pedido: si la oferta
 * se usó en un presupuesto que no llegó a pedido, no aparece acá y se cuenta
 * sólo en el resumen de ofertas.
 */
public record DescuentoPedidoDTO(
        Long idPedido,
        LocalDateTime fechaPedido,
        String estado,
        Long idCliente,
        String clienteUsuario,
        String clienteNombre,
        Long idArtista,
        String artistaUsuario,
        Long idServicio,
        String servicioNombre,
        Long idCampana,
        LocalDateTime campanaFecha,
        Long idOferta,
        BigDecimal ofertaPorcentaje,
        String ofertaMotivo,
        String ofertaEstado,
        LocalDateTime ofertaFechaReserva,
        LocalDateTime ofertaFechaUtilizada,
        BigDecimal descuentoPorcentaje,
        BigDecimal descuentoMontoCobro,
        BigDecimal precioFinalCobro,
        BigDecimal precioFinalClienteCobro,
        BigDecimal comisionPorcentaje,
        BigDecimal comisionMontoCobro,
        BigDecimal comisionNominalCobro,
        String monedaCobro,
        boolean comisionCero) {
}
