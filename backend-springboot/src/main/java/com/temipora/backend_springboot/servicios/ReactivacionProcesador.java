package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.CampanaReactivacion;
import com.temipora.backend_springboot.modelo.ConfiguracionInactividad;
import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.EstadoCampanaReactivacion;
import com.temipora.backend_springboot.modelo.EstadoOfertaReactivacion;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.OfertaReactivacion;
import com.temipora.backend_springboot.modelo.PropositoToken;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CampanaReactivacionRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class ReactivacionProcesador {

    private static final Logger log =
        LoggerFactory.getLogger(ReactivacionProcesador.class);

    private static final List<EstadoSolicitud> ESTADOS_ACTIVOS = List.of(
        EstadoSolicitud.PENDIENTE,
        EstadoSolicitud.EN_LISTA_ESPERA,
        EstadoSolicitud.PRESUPUESTADA,
        EstadoSolicitud.PAGADA);

    private static final String TIPO_NOTIFICACION = "OFERTA_REACTIVACION";
    private static final String TIPO_REFERENCIA = "OFERTA_REACTIVACION";

    private final UsuarioRepository usuarioRepository;
    private final SolicitudRepository solicitudRepository;
    private final ComisionService comisionService;
    private final RecomendacionService recomendacionService;
    private final CampanaReactivacionRepository campanaRepository;
    private final NotificacionService notificacionService;
    private final EnvioEmailService envioEmailService;
    private final EmailPlantillaService emailPlantillaService;
    private final MonedaService monedaService;
    private final AccesoEnlaceService accesoEnlaceService;

    @Value("${temipora.mail.base-url:http://localhost:5173}")
    private String baseUrl;

    public ReactivacionProcesador(
            UsuarioRepository usuarioRepository,
            SolicitudRepository solicitudRepository,
            ComisionService comisionService,
            RecomendacionService recomendacionService,
            CampanaReactivacionRepository campanaRepository,
            NotificacionService notificacionService,
            EnvioEmailService envioEmailService,
            EmailPlantillaService emailPlantillaService,
            MonedaService monedaService,
            AccesoEnlaceService accesoEnlaceService) {
        this.usuarioRepository = usuarioRepository;
        this.solicitudRepository = solicitudRepository;
        this.comisionService = comisionService;
        this.recomendacionService = recomendacionService;
        this.campanaRepository = campanaRepository;
        this.notificacionService = notificacionService;
        this.envioEmailService = envioEmailService;
        this.emailPlantillaService = emailPlantillaService;
        this.monedaService = monedaService;
        this.accesoEnlaceService = accesoEnlaceService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int procesar(
            Long idUsuario,
            ConfiguracionInactividad config,
            List<Servicio> candidatos,
            LocalDateTime ahora) {
        Usuario usuario = usuarioRepository.findByIdUsuario(idUsuario)
            .orElse(null);
        if (usuario == null || !usuario.isEstadoCuenta()) {
            return 0;
        }

        Set<Long> serviciosConSolicitud = new HashSet<>(
            solicitudRepository.serviciosConSolicitudActiva(
                idUsuario, ESTADOS_ACTIVOS));
        if (!serviciosConSolicitud.isEmpty()) {
            return 0;
        }

        boolean ocultarSensibles = usuario.getPreferenciaSensible()
            == EPreferenciaContenidoSensible.OCULTAR;

        Moneda moneda = monedaService.predeterminada();
        RecomendacionService.ResultadoRanking ranking =
            recomendacionService.rankear(candidatos, idUsuario, moneda);
        List<RecomendacionService.ServicioPuntuado> items =
            ranking.personalizado()
                ? ranking.items()
                : recomendacionService.rankearPorCalidad(candidatos, moneda);

        List<RecomendacionService.ServicioPuntuado> seleccionados = new ArrayList<>();
        Map<Long, String> motivos = new HashMap<>();
        for (RecomendacionService.ServicioPuntuado item : items) {
            Servicio servicio = item.servicio();
            if (servicio == null
                    || servicio.getArtista() == null
                    || servicio.getArtista().getIdUsuario().equals(idUsuario)
                    || serviciosConSolicitud.contains(servicio.getIdServicio())
                    || (ocultarSensibles && servicio.isContenidoSensible())) {
                continue;
            }
            seleccionados.add(item);
            motivos.put(servicio.getIdServicio(), item.motivo());
            if (seleccionados.size() >= Math.max(
                    1, config.getMaxOfertasPorUsuario())) {
                break;
            }
        }
        if (seleccionados.isEmpty()) {
            return 0;
        }

        BigDecimal comisionPorcentaje = comisionService.obtener()
            .getPorcentajeServicio()
            .setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal porcentajeEfectivo = config.getPorcentajeDescuento()
            .min(comisionPorcentaje)
            .max(BigDecimal.ZERO)
            .setScale(2, java.math.RoundingMode.HALF_UP);

        CampanaReactivacion campana = new CampanaReactivacion();
        campana.setUsuario(usuario);
        campana.setToken(UUID.randomUUID().toString());
        campana.setEstado(EstadoCampanaReactivacion.ACTIVA);
        campana.setFechaCreacion(ahora);
        campana.setFechaVencimiento(
            ahora.plusHours(Math.max(1, config.getVigenciaHoras())));

        for (RecomendacionService.ServicioPuntuado item : seleccionados) {
            Servicio servicio = item.servicio();
            OfertaReactivacion oferta = new OfertaReactivacion();
            oferta.setServicio(servicio);
            oferta.setArtista(servicio.getArtista());
            oferta.setPorcentajeDescuento(porcentajeEfectivo);
            oferta.setComisionTopePorcentaje(comisionPorcentaje);
            oferta.setMotivo(item.motivo());
            oferta.setEstado(EstadoOfertaReactivacion.OFRECIDA);
            oferta.setFechaCreacion(ahora);
            oferta.setFechaVencimiento(campana.getFechaVencimiento());
            campana.agregarOferta(oferta);
        }
        CampanaReactivacion guardada = campanaRepository.save(campana);
        String tokenAcceso = accesoEnlaceService.generar(
            usuario,
            PropositoToken.OFERTA,
            guardada.getIdCampana(),
            Math.max(1, config.getVigenciaHoras()));

        String porcentaje = porcentajeEfectivo
            .stripTrailingZeros()
            .toPlainString();
        notificacionService.crearParaUsuario(
            idUsuario,
            TIPO_NOTIFICACION,
            "Tenés ofertas para retomar",
            "Preparamos " + seleccionados.size() + " propuesta(s) con "
                + porcentaje + "% de descuento para que vuelvas a Temipora.",
            campana.getIdCampana(),
            TIPO_REFERENCIA);

        if (usuario.isRecibirEmailsPromocionales()) {
            enviarEmail(usuario, guardada, motivos, tokenAcceso);
        }

        log.info("Campaña {} generada para el usuario {} con {} oferta(s).",
            guardada.getIdCampana(), idUsuario, seleccionados.size());
        return 1;
    }

    private void enviarEmail(
            Usuario usuario,
            CampanaReactivacion campana,
            Map<Long, String> motivos,
            String tokenAcceso) {
        if (usuario.getTokenBajaEmail() == null
                || usuario.getTokenBajaEmail().isBlank()) {
            usuario.setTokenBajaEmail(UUID.randomUUID().toString());
            usuarioRepository.save(usuario);
        }

        List<OfertaEmail> ofertas = new ArrayList<>();
        for (OfertaReactivacion oferta : campana.getOfertas()) {
            Servicio servicio = oferta.getServicio();
            String imagen = servicio.getImagenPortada();
            if ((imagen == null || imagen.isBlank())
                    && servicio.getImagenes() != null
                    && !servicio.getImagenes().isEmpty()) {
                imagen = servicio.getImagenes().get(0).getUrl();
            }
            String url = baseUrl + "/ofertas/" + campana.getToken()
                + "?acceso=" + tokenAcceso
                + "&oferta=" + oferta.getIdOferta();
            ofertas.add(new OfertaEmail(
                oferta.getIdOferta(),
                servicio.getNombre(),
                servicio.getArtista() != null
                    ? servicio.getArtista().getNombreUsuario()
                    : "",
                imagen,
                servicio.getPrecioBase() != null
                    ? servicio.getPrecioBase().toPlainString()
                    : "—",
                servicio.getMoneda() != null
                    ? servicio.getMoneda().getCodigo()
                    : "",
                oferta.getPorcentajeDescuento(),
                motivos.getOrDefault(
                    servicio.getIdServicio(), "Tiene cupos disponibles"),
                url));
        }

        String html = emailPlantillaService.reactivacion(
            nombre(usuario), ofertas, usuario.getTokenBajaEmail());
        envioEmailService.enviar(
            usuario,
            "REACTIVACION",
            campana.getIdCampana(),
            "CAMPANA_REACTIVACION",
            EmailPlantillaService.ASUNTO_REACTIVACION,
            html,
            "reactivacion:" + campana.getToken());
    }

    private String nombre(Usuario usuario) {
        if (usuario.getNombreDisplay() != null
                && !usuario.getNombreDisplay().isBlank()) {
            return usuario.getNombreDisplay();
        }
        return usuario.getNombreUsuario() != null
            ? usuario.getNombreUsuario()
            : "artista";
    }
}
