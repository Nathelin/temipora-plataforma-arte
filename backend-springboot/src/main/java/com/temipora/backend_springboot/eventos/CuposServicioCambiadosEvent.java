package com.temipora.backend_springboot.eventos;

/**
 * Se publica cuando cambia la cantidad de cupos ocupados de un servicio o su
 * capacidad. El reconciliador lo escucha después del commit para ajustar el
 * modo de recepción sin acoplar los flujos de presupuesto/pedido.
 */
public record CuposServicioCambiadosEvent(Long idServicio) {
}
