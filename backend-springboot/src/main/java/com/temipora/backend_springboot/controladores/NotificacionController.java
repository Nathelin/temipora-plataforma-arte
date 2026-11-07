package com.temipora.backend_springboot.controladores;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.ContadorNotificacionesDTO;
import com.temipora.backend_springboot.modelo.NotificacionDTO;
import com.temipora.backend_springboot.modelo.PaginaDTO;
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
    public PaginaDTO<NotificacionDTO> listar(
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "false") boolean soloNoLeidas,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio,
            Authentication authentication) {
        return notificacionService.listar(
            idAutenticado(authentication),
            categoria,
            soloNoLeidas,
            pagina,
            tamanio);
    }

    @GetMapping("/recientes")
    public List<NotificacionDTO> recientes(
            @RequestParam(required = false) Integer limite,
            Authentication authentication) {
        return notificacionService.recientes(
            idAutenticado(authentication), limite);
    }

    @GetMapping("/contador")
    public ContadorNotificacionesDTO contador(Authentication authentication) {
        return notificacionService.contador(idAutenticado(authentication));
    }

    @PostMapping("/leer-todas")
    public Map<String, Integer> marcarTodas(Authentication authentication) {
        return Map.of(
            "marcadas",
            notificacionService.marcarTodasLeidas(idAutenticado(authentication)));
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
