package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.EnvioEmail;
import com.temipora.backend_springboot.modelo.EstadoEnvioEmail;
import com.temipora.backend_springboot.modelo.PropositoToken;
import com.temipora.backend_springboot.modelo.Usuario;

/**
 * Manda dos mails de muestra distintos al correo del administrador: uno de
 * pagos y otro de entregas. Usa la plantilla real y no toca pedidos ni la cola.
 */
@Service
public class RecordatorioPruebaService {

    private static final int VIGENCIA_ENLACE_HORAS = 24;
    private static final DateTimeFormatter FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmailPlantillaService emailPlantillaService;
    private final EnvioEmailService envioEmailService;
    private final AccesoEnlaceService accesoEnlaceService;
    private final ConfiguracionPedidoService configuracionPedidoService;

    public RecordatorioPruebaService(
            EmailPlantillaService emailPlantillaService,
            EnvioEmailService envioEmailService,
            AccesoEnlaceService accesoEnlaceService,
            ConfiguracionPedidoService configuracionPedidoService) {
        this.emailPlantillaService = emailPlantillaService;
        this.envioEmailService = envioEmailService;
        this.accesoEnlaceService = accesoEnlaceService;
        this.configuracionPedidoService = configuracionPedidoService;
    }

    public int enviar(Usuario usuario) {
        if (usuario == null || usuario.getCorreo() == null
                || usuario.getCorreo().isBlank()) {
            return 0;
        }
        int enviados = 0;
        ConfiguracionPedido config = configuracionPedidoService.obtener();
        int gracia = config.getDiasGraciaEntrega() != null
            ? config.getDiasGraciaEntrega()
            : ConfiguracionPedido.DIAS_GRACIA_ENTREGA_POR_DEFECTO;
        if (enviarGrupo(usuario, pagosDeMuestra(), true, false, "pago")) {
            enviados++;
        }
        if (enviarGrupo(usuario, entregasDeMuestra(gracia), false, true,
                "entrega")) {
            enviados++;
        }
        return enviados;
    }

    private boolean enviarGrupo(
            Usuario usuario,
            List<EmailPlantillaService.RecordatorioEmail> items,
            boolean hayPagos,
            boolean hayEntregas,
            String sufijo) {
        String token = accesoEnlaceService.generar(
            usuario, PropositoToken.RECORDATORIO, null,
            VIGENCIA_ENLACE_HORAS);
        String asunto = "[Prueba] " + emailPlantillaService.asuntoRecordatorio(
            hayPagos, hayEntregas, hayUrgente(items));
        String html = hayPagos
            ? emailPlantillaService.recordatorio(
                nombre(usuario), items, List.of(), token)
            : emailPlantillaService.recordatorio(
                nombre(usuario), List.of(), items, token);
        EnvioEmail envio = envioEmailService.enviar(
            usuario,
            "RECORDATORIO_PRUEBA",
            null,
            null,
            asunto,
            html,
            "recordatorio-prueba-" + sufijo + ":"
                + usuario.getIdUsuario() + ":" + UUID.randomUUID());
        return envio != null
            && envio.getEstado() == EstadoEnvioEmail.ENVIADO;
    }

    private List<EmailPlantillaService.RecordatorioEmail> pagosDeMuestra() {
        return List.of(new EmailPlantillaService.RecordatorioEmail(
            "PAGO",
            null,
            "Retrato en acuarela",
            null,
            "artista_demo",
            "Cuota 2",
            FECHA.format(LocalDate.now().plusDays(1)),
            new BigDecimal("1200"),
            "ARS",
            true,
            false,
            null));
    }

    private List<EmailPlantillaService.RecordatorioEmail> entregasDeMuestra(
            int gracia) {
        return List.of(new EmailPlantillaService.RecordatorioEmail(
            "ENTREGA",
            null,
            "Ilustración digital",
            null,
            "cliente_demo",
            "Boceto",
            FECHA.format(LocalDate.now()),
            null,
            "ARS",
            true,
            false,
            consecuenciaEntrega(gracia)));
    }

    private String consecuenciaEntrega(int gracia) {
        return "Vence hoy. Si no entregás, en " + dias(gracia)
            + " se suspenden tus ventas. Si la demora continúa, tu cuenta "
            + "queda limitada solo a entregar los pedidos abiertos: no podés "
            + "usar el resto del sitio hasta que un administrador te reintegre.";
    }

    private String dias(int cantidad) {
        return cantidad == 1 ? "1 día" : cantidad + " días";
    }

    private boolean hayUrgente(
            List<EmailPlantillaService.RecordatorioEmail> items) {
        for (EmailPlantillaService.RecordatorioEmail item : items) {
            if (item.urgente()) {
                return true;
            }
        }
        return false;
    }

    private String nombre(Usuario usuario) {
        if (usuario.getNombreDisplay() != null
                && !usuario.getNombreDisplay().isBlank()) {
            return usuario.getNombreDisplay();
        }
        return usuario.getNombreUsuario() != null
            ? usuario.getNombreUsuario()
            : "";
    }
}
