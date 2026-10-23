package com.temipora.backend_springboot.controladores;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.NotificacionDTO;
import com.temipora.backend_springboot.servicios.NotificacionService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final UsuarioService usuarioService;

    public NotificacionController(
            NotificacionService notificacionService,
            UsuarioService usuarioService) {
        this.notificacionService = notificacionService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<NotificacionDTO> listar(Authentication authentication) {
        return notificacionService.listar(idAutenticado(authentication));
    }

    @GetMapping("/contador")
    public Map<String, Long> contador(Authentication authentication) {
        return Map.of("noLeidas", notificacionService.contarNoLeidas(idAutenticado(authentication)));
    }

    @PostMapping("/{idNotificacionUsuario}/leer")
    public NotificacionDTO marcarLeida(
            @PathVariable Long idNotificacionUsuario,
            Authentication authentication) {
        return notificacionService.marcarLeida(
            idNotificacionUsuario, idAutenticado(authentication));
    }

    private Long idAutenticado(Authentication authentication) {
        return usuarioService.getByNombreUsuario(authentication.getName()).getIdUsuario();
    }
}
