package com.temipora.backend_springboot.modelo;


public enum EstadoPedido{
    PENDIENTE_ACEPTACION,
    PENDIENTE_PAGO,
    CANCELADO,
    TERMINADO,
    ETAPA_INICIAL

    /* TODO: tengo que ver esto. me gustaría que los artistas pudieran definir sus propios estados de avance
    según el proceso que manejen y que se controle sobre eso............
    
    Los PENDIENTE en realidad serían de la solicitud, no?........*/

}