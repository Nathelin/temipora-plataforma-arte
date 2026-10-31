package com.temipora.backend_springboot.servicios;

public interface IndicadorEscritura {

    void marcar(Long idConversacion, Long idUsuario);

    void limpiar(Long idConversacion, Long idUsuario);

    boolean estaEscribiendo(Long idConversacion, Long idUsuario);
}
