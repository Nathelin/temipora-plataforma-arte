package com.temipora.backend_springboot.controladores;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.RecordatorioPruebaService;
import com.temipora.backend_springboot.servicios.RecordatorioService;
import com.temipora.backend_springboot.servicios.SancionAutomaticaService;
import com.temipora.backend_springboot.servicios.UsuarioService;

@RestController
@RequestMapping("/api/admin/recordatorios")
@PreAuthorize("hasRole('ADMIN')")
public class RecordatorioAdminController {

    private final RecordatorioService recordatorioService;
    private final RecordatorioPruebaService pruebaService;
    private final SancionAutomaticaService sancionAutomaticaService;
    private final UsuarioService usuarioService;

    public RecordatorioAdminController(
            RecordatorioService recordatorioService,
            RecordatorioPruebaService pruebaService,
            SancionAutomaticaService sancionAutomaticaService,
            UsuarioService usuarioService) {
        this.recordatorioService = recordatorioService;
        this.pruebaService = pruebaService;
        this.sancionAutomaticaService = sancionAutomaticaService;
        this.usuarioService = usuarioService;
    }

    /**
     * Fuerza la ejecución del job de recordatorios de pagos y entregas.
     */
    @PostMapping("/procesar")
    public Map<String, Object> procesar() {
        int encolados = recordatorioService.encolar();
        int enviados = recordatorioService.procesarPendientes();
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("encolados", encolados);
        respuesta.put("enviados", enviados);
        return respuesta;
    }

    /**
     * Reabre los recordatorios ya enviados y los vuelve a mandar si todavía
     * cumplen las condiciones. También reenvía el aviso de sanción a las
     * cuentas con una restricción automática vigente, con un enlace de acceso
     * fresco a la pantalla correspondiente.
     */
    @PostMapping("/reenviar")
    public Map<String, Object> reenviar() {
        int reabiertos = recordatorioService.reabrirEnviados();
        int encolados = recordatorioService.encolar();
        int enviados = recordatorioService.procesarPendientes();
        int avisosSancion = sancionAutomaticaService.reenviarAvisosVigentes();
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("reabiertos", reabiertos);
        respuesta.put("encolados", encolados);
        respuesta.put("enviados", enviados);
        respuesta.put("avisosSancion", avisosSancion);
        return respuesta;
    }

    /**
     * Manda dos muestras al correo del admin: una de pagos y otra de entregas.
     * No encola nada ni toca pedidos.
     */
    @PostMapping("/prueba")
    public Map<String, Object> enviarPrueba(Authentication authentication) {
        Usuario admin = usuarioService.getByNombreUsuario(
            authentication.getName());
        int enviados = pruebaService.enviar(admin);
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("enviados", enviados);
        respuesta.put("correo",
            admin.getCorreo() != null ? admin.getCorreo() : "");
        return respuesta;
    }
}
