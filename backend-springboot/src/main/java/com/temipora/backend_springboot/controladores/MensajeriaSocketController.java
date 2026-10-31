package com.temipora.backend_springboot.controladores;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.temipora.backend_springboot.servicios.ConversacionService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@Controller
public class MensajeriaSocketController {

    private final ConversacionService conversacionService;
    private final UsuarioService usuarioService;

    public MensajeriaSocketController(
            ConversacionService conversacionService,
            UsuarioService usuarioService) {
        this.conversacionService = conversacionService;
        this.usuarioService = usuarioService;
    }

    @MessageMapping("/conversacion/{idConversacion}/escribiendo")
    public void escribiendo(
            @DestinationVariable Long idConversacion,
            Principal principal) {
        if (principal == null) {
            return;
        }
        try {
            Long idUsuario = usuarioService
                .getByNombreUsuario(principal.getName())
                .getIdUsuario();
            conversacionService.marcarEscribiendo(idConversacion, idUsuario);
        } catch (RuntimeException ignorada) {
            // El latido de escritura es efímero y best-effort: si la conversación
            // no existe o el usuario no participa, se descarta sin cortar la sesión.
        }
    }
}
