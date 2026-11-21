package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ApelacionSancion;
import com.temipora.backend_springboot.modelo.ApelacionSancionDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.EstadoApelacion;
import com.temipora.backend_springboot.modelo.EstadoSancion;
import com.temipora.backend_springboot.modelo.OrigenSancion;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ApelacionSancionRepository;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class ApelacionService {

    private static final String ENTIDAD = "ApelacionSancion";
    private static final String NOTIF_RESUELTA = "APELACION_RESUELTA";
    private static final String TIPO_REFERENCIA = "PEDIDO";
    private static final int LARGO_MENSAJE = 2000;
    private static final int LARGO_RESPUESTA = 1000;

    private final ApelacionSancionRepository apelacionRepository;
    private final SancionCuentaRepository sancionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionPedidoService configuracionPedidoService;
    private final SancionAutomaticaService sancionAutomaticaService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoria;

    public ApelacionService(
            ApelacionSancionRepository apelacionRepository,
            SancionCuentaRepository sancionRepository,
            UsuarioRepository usuarioRepository,
            ConfiguracionPedidoService configuracionPedidoService,
            SancionAutomaticaService sancionAutomaticaService,
            NotificacionService notificacionService,
            AuditoriaService auditoria) {
        this.apelacionRepository = apelacionRepository;
        this.sancionRepository = sancionRepository;
        this.usuarioRepository = usuarioRepository;
        this.configuracionPedidoService = configuracionPedidoService;
        this.sancionAutomaticaService = sancionAutomaticaService;
        this.notificacionService = notificacionService;
        this.auditoria = auditoria;
    }

    @Transactional
    public ApelacionSancionDTO crear(
            Long idUsuario, Long idSancion, String mensaje) {
        LocalDateTime ahora = LocalDateTime.now();
        SancionCuenta sancion = sancionRepository.findById(idSancion)
            .orElseThrow(() -> new NoSuchElementException(
                "Sanción no encontrada: " + idSancion));
        if (sancion.getUsuario() == null
                || !sancion.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new AccesoDenegadoException(
                "No podés apelar esta sanción.");
        }
        if (sancion.getOrigen() != OrigenSancion.AUTOMATICA) {
            throw new ConflictoException(
                "Solo se apelan las sanciones automáticas por incumplimiento.");
        }
        if (sancion.getEstado() != EstadoSancion.ACTIVA) {
            throw new ConflictoException("La sanción ya no está activa.");
        }
        if (apelacionRepository.findFirstBySancionIdSancionAndEstado(
                idSancion, EstadoApelacion.PENDIENTE).isPresent()) {
            throw new ConflictoException(
                "Ya tenés una apelación pendiente para esta sanción.");
        }
        ConfiguracionPedido config = configuracionPedidoService.obtener();
        int dias = config.getDiasReapelacion() != null
            ? config.getDiasReapelacion()
            : ConfiguracionPedido.DIAS_REAPELACION_POR_DEFECTO;
        var ultima = apelacionRepository
            .findFirstBySancionIdSancionOrderByCreadaEnDesc(idSancion);
        if (ultima.isPresent()
                && ahora.isBefore(ultima.get().getCreadaEn().plusDays(dias))) {
            throw new ConflictoException(
                "Podés volver a apelar dentro de " + dias + " días.");
        }

        Usuario usuario = usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + idUsuario));
        ApelacionSancion apelacion = new ApelacionSancion();
        apelacion.setSancion(sancion);
        apelacion.setUsuario(usuario);
        apelacion.setMensaje(limpiar(mensaje, LARGO_MENSAJE,
            "Escribí el motivo de tu apelación."));
        apelacion.setEstado(EstadoApelacion.PENDIENTE);
        apelacion.setCreadaEn(ahora);
        ApelacionSancion guardada = apelacionRepository.save(apelacion);
        auditoria.registrar(
            AccionesAdmin.APELACION_CREAR, ENTIDAD, guardada.getIdApelacion(),
            "Apeló la sanción #" + idSancion + " de @" + usuario.getNombreUsuario(),
            "Apelación del usuario");
        return ApelacionSancionDTO.desde(guardada, ahora);
    }

    @Transactional(readOnly = true)
    public List<ApelacionSancionDTO> mias(Long idUsuario) {
        LocalDateTime ahora = LocalDateTime.now();
        return apelacionRepository
            .findByUsuarioIdUsuarioOrderByCreadaEnDesc(idUsuario).stream()
            .map(apelacion -> ApelacionSancionDTO.desde(apelacion, ahora))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ApelacionSancionDTO> listar(EstadoApelacion estado) {
        LocalDateTime ahora = LocalDateTime.now();
        return apelacionRepository
            .findByEstadoOrderByCreadaEnAsc(
                estado != null ? estado : EstadoApelacion.PENDIENTE)
            .stream()
            .map(apelacion -> ApelacionSancionDTO.desde(apelacion, ahora))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ApelacionSancionDTO> historial(Long idSancion) {
        LocalDateTime ahora = LocalDateTime.now();
        return apelacionRepository
            .findBySancionIdSancionOrderByCreadaEnDesc(idSancion).stream()
            .map(apelacion -> ApelacionSancionDTO.desde(apelacion, ahora))
            .toList();
    }

    @Transactional
    public ApelacionSancionDTO resolver(
            Long idAdmin, Long idApelacion, boolean aceptada, String respuesta) {
        LocalDateTime ahora = LocalDateTime.now();
        ApelacionSancion apelacion = apelacionRepository.findById(idApelacion)
            .orElseThrow(() -> new NoSuchElementException(
                "Apelación no encontrada: " + idApelacion));
        if (apelacion.getEstado() != EstadoApelacion.PENDIENTE) {
            throw new ConflictoException("La apelación ya fue resuelta.");
        }
        Usuario admin = usuarioRepository.findByIdUsuario(idAdmin)
            .orElseThrow(() -> new NoSuchElementException(
                "Administrador no encontrado: " + idAdmin));

        apelacion.setEstado(aceptada
            ? EstadoApelacion.ACEPTADA
            : EstadoApelacion.RECHAZADA);
        apelacion.setResueltaEn(ahora);
        apelacion.setAdminResolvio(admin);
        apelacion.setRespuesta(limpiar(respuesta, LARGO_RESPUESTA, null));
        ApelacionSancion guardada = apelacionRepository.save(apelacion);

        SancionCuenta sancion = guardada.getSancion();
        if (aceptada && sancion != null) {
            sancionAutomaticaService.revertirYRestaurar(
                sancion.getIdSancion(),
                "Apelación aceptada por @" + admin.getNombreUsuario()
                    + (guardada.getRespuesta() != null
                        ? ": " + guardada.getRespuesta()
                        : ""));
        }

        notificacionService.crearParaUsuario(
            guardada.getUsuario().getIdUsuario(),
            NOTIF_RESUELTA,
            "Apelación resuelta",
            aceptada
                ? "Se aceptó tu apelación. La sanción quedó sin efecto."
                : "Se rechazó tu apelación."
                    + (guardada.getRespuesta() != null
                        ? " Respuesta: " + guardada.getRespuesta()
                        : ""),
            sancion != null && sancion.getPedido() != null
                ? sancion.getPedido().getIdPedido()
                : null,
            TIPO_REFERENCIA);

        auditoria.registrar(
            AccionesAdmin.APELACION_RESOLVER, ENTIDAD, guardada.getIdApelacion(),
            (aceptada ? "Aceptó" : "Rechazó") + " la apelación de @"
                + guardada.getUsuario().getNombreUsuario(),
            respuesta);
        return ApelacionSancionDTO.desde(guardada, ahora);
    }

    private String limpiar(String valor, int maximo, String mensajeVacio) {
        if (valor == null || valor.isBlank()) {
            if (mensajeVacio == null) {
                return null;
            }
            throw new IllegalArgumentException(mensajeVacio);
        }
        String limpio = valor.trim();
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException(
                "El texto no puede superar los " + maximo + " caracteres.");
        }
        return limpio;
    }
}
