package com.temipora.backend_springboot.servicios;

public interface EmailService {

    /**
     * Envía un mensaje HTML.
     *
     * @return true si el mensaje salió hacia el proveedor; false si el envío
     *         está deshabilitado por configuración.
     */
    boolean enviar(EmailMensaje mensaje);
}
