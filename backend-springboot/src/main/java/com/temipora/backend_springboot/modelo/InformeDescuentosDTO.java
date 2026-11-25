package com.temipora.backend_springboot.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Informe de descuentos de reactivación. La lista {@code pedidos} es la página
 * pedida; el resumen y las ofertas siempre reflejan todo el período.
 */
public record InformeDescuentosDTO(
        LocalDate desde,
        LocalDate hasta,
        long dias,
        String moneda,
        String filtro,
        LocalDateTime generadoEn,
        ResumenDescuentosDTO resumen,
        OfertasReactivacionResumenDTO ofertas,
        List<DescuentoPedidoDTO> pedidos,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas,
        boolean hayMas) {
}
