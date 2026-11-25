package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

/**
 * Evento histórico de búsqueda de alternativas. Incluye las etiquetas ya
 * resueltas para que la tabla, el PDF y los filtros muestren lo mismo.
 */
public record SustitucionHistorialDTO(
        Long idSustitucion,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaResolucion,
        String estado,
        String estadoEtiqueta,
        String motivo,
        String motivoEtiqueta,
        String disparador,
        String disparadorEtiqueta,
        Long idCliente,
        String clienteUsuario,
        String clienteNombre,
        Long idServicioOrigen,
        String servicioOrigen,
        Long idArtistaOrigen,
        String artistaOrigen,
        int cantidadOpciones,
        Long idServicioElegido,
        String servicioElegido,
        Long idSolicitud,
        Long idPedido,
        String resultado) {
}
