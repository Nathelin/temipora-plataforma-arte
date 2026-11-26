package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.config.ContactoPlataforma;
import com.temipora.backend_springboot.config.JwtUtils;
import com.temipora.backend_springboot.modelo.BajaCuentaResultadoDTO;
import com.temipora.backend_springboot.modelo.CambioAuditoriaDTO;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.UsuarioDTO;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class BajaCuentaService {

    private static final String ENTIDAD_USUARIO = "Usuario";

    private static final List<EstadoPedido> ESTADOS_PEDIDO_QUE_BLOQUEAN = List.of(
        EstadoPedido.PENDIENTE_PAGO,
        EstadoPedido.EN_CURSO,
        EstadoPedido.IMPAGO);

    private static final List<EstadoSolicitud> ESTADOS_SOLICITUD_QUE_BLOQUEAN = List.of(
        EstadoSolicitud.PENDIENTE,
        EstadoSolicitud.EN_LISTA_ESPERA,
        EstadoSolicitud.PRESUPUESTADA);

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final SolicitudRepository solicitudRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConfiguracionBajaCuentaService configuracionBajaCuentaService;
    private final AuditoriaService auditoria;
    private final JwtUtils jwtUtils;
    private final ContactoPlataforma contacto;
    private final SancionService sancionService;

    public BajaCuentaService(
            UsuarioRepository usuarioRepository,
            PedidoRepository pedidoRepository,
            SolicitudRepository solicitudRepository,
            PasswordEncoder passwordEncoder,
            ConfiguracionBajaCuentaService configuracionBajaCuentaService,
            AuditoriaService auditoria,
            JwtUtils jwtUtils,
            ContactoPlataforma contacto,
            SancionService sancionService) {
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.solicitudRepository = solicitudRepository;
        this.passwordEncoder = passwordEncoder;
        this.configuracionBajaCuentaService = configuracionBajaCuentaService;
        this.auditoria = auditoria;
        this.jwtUtils = jwtUtils;
        this.contacto = contacto;
        this.sancionService = sancionService;
    }

    public LocalDateTime reactivarHasta(Usuario usuario) {
        if (usuario == null || usuario.getFechaBajaVoluntaria() == null) {
            return null;
        }
        return usuario.getFechaBajaVoluntaria()
            .plusDays(configuracionBajaCuentaService.diasParaReactivar());
    }

    public boolean puedeReactivar(Usuario usuario, LocalDateTime ahora) {
        LocalDateTime hasta = reactivarHasta(usuario);
        return hasta != null && !ahora.isAfter(hasta);
    }

    @Transactional
    public BajaCuentaResultadoDTO darDeBaja(Long idUsuario, String contrasenia) {
        Usuario usuario = usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new IllegalArgumentException(
                "Usuario no encontrado: " + idUsuario));

        if (contrasenia == null
                || !passwordEncoder.matches(contrasenia, usuario.getContrasenia())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }

        if (usuario.getRol() != null && usuario.getRol().getNombre() == ERol.ADMIN) {
            throw new ConflictoException(
                "La cuenta de administración no puede darse de baja desde acá.");
        }
        if (!usuario.isEstadoCuenta()) {
            throw new ConflictoException("Tu cuenta ya está inactiva.");
        }
        if (sancionService.tieneAlgunaSancionVigente(idUsuario)) {
            throw new ConflictoException(
                "No podés darte de baja mientras tengas una sanción vigente.");
        }

        long pedidos = pedidoRepository.countByClienteIdUsuarioAndEstadoIn(
                idUsuario, ESTADOS_PEDIDO_QUE_BLOQUEAN)
            + pedidoRepository.countByArtistaIdUsuarioAndEstadoIn(
                idUsuario, ESTADOS_PEDIDO_QUE_BLOQUEAN);
        long solicitudes = solicitudRepository.countByClienteIdUsuarioAndEstadoIn(
                idUsuario, ESTADOS_SOLICITUD_QUE_BLOQUEAN)
            + solicitudRepository.countByArtistaIdUsuarioAndEstadoIn(
                idUsuario, ESTADOS_SOLICITUD_QUE_BLOQUEAN);

        if (pedidos > 0 || solicitudes > 0) {
            throw new ConflictoException(mensajeCompromisos(pedidos, solicitudes));
        }

        LocalDateTime fechaBaja = LocalDateTime.now();
        usuario.marcarBajaVoluntaria(fechaBaja);
        usuarioRepository.save(usuario);

        auditoria.registrarCambios(
            AccionesAdmin.USUARIO_BAJA_VOLUNTARIA, ENTIDAD_USUARIO, idUsuario,
            "Dio de baja su cuenta voluntariamente",
            List.of(
                new CambioAuditoriaDTO("estadoCuenta", "Cuenta activa", "true", "false"),
                new CambioAuditoriaDTO("fechaBajaVoluntaria", "Fecha de baja voluntaria",
                    null, fechaBaja.toString())));

        return new BajaCuentaResultadoDTO(
            "Tu cuenta quedó inactiva.", reactivarHasta(usuario));
    }

    @Transactional
    public Map<String, Object> reactivarConCredenciales(
            String nombreUsuario, String contrasenia) {
        String nombre = nombreUsuario != null ? nombreUsuario.trim() : "";
        Usuario usuario = usuarioRepository.findByNombreUsuarioIgnoreCase(nombre)
            .orElse(null);

        if (usuario == null || contrasenia == null
                || !passwordEncoder.matches(contrasenia, usuario.getContrasenia())) {
            throw new CredencialesInvalidasException();
        }
        if (usuario.isEstadoCuenta()) {
            throw new ConflictoException("La cuenta ya está activa.");
        }
        if (usuario.getFechaBajaVoluntaria() == null) {
            throw new ConflictoException(
                "Esta cuenta no está dada de baja voluntariamente. Escribinos a "
                    + contacto.getEmail() + ".");
        }

        LocalDateTime hasta = reactivarHasta(usuario);
        if (LocalDateTime.now().isAfter(hasta)) {
            throw new ReactivacionVencidaException(contacto.getEmail(), hasta);
        }

        usuario.reactivarCuenta();
        usuario.setFechaUltimaConexion(LocalDateTime.now());
        usuarioRepository.save(usuario);

        String rol = rolDe(usuario);
        auditoria.registrarActor(
            AccionesAdmin.USUARIO_REACTIVAR_CUENTA, ENTIDAD_USUARIO,
            usuario.getIdUsuario(),
            "Reactivó su cuenta desde el inicio de sesión",
            usuario.getNombreUsuario(), rol);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("token",
            jwtUtils.generarTokenAcceso(usuario.getNombreUsuario(), rol));
        respuesta.put("Message", "Cuenta reactivada correctamente.");
        respuesta.put("NombreUsuario", usuario.getNombreUsuario());
        respuesta.put("rol", rol);
        respuesta.put("idUsuario", usuario.getIdUsuario());
        respuesta.put("urlFotoPerfil", usuario.getUrlFotoPerfil());
        return respuesta;
    }

    @Transactional
    public UsuarioDTO reactivarComoAdmin(Long idUsuario) {
        Usuario usuario = usuarioRepository.findByIdUsuario(idUsuario)
            .orElseThrow(() -> new IllegalArgumentException(
                "Usuario no encontrado: " + idUsuario));

        if (usuario.isEstadoCuenta()) {
            throw new ConflictoException("La cuenta ya está activa.");
        }

        LocalDateTime fechaBaja = usuario.getFechaBajaVoluntaria();
        usuario.reactivarCuenta();
        usuarioRepository.save(usuario);

        auditoria.registrarCambios(
            AccionesAdmin.USUARIO_REACTIVAR_CUENTA_ADMIN, ENTIDAD_USUARIO, idUsuario,
            "Reactivó la cuenta de @" + usuario.getNombreUsuario(),
            List.of(
                new CambioAuditoriaDTO("estadoCuenta", "Cuenta activa", "false", "true"),
                new CambioAuditoriaDTO("fechaBajaVoluntaria", "Fecha de baja voluntaria",
                    fechaBaja != null ? fechaBaja.toString() : null, null)));

        return UsuarioDTO.desdeUsuario(
            usuario, configuracionBajaCuentaService.diasParaReactivar());
    }

    private String rolDe(Usuario usuario) {
        return usuario.getRol() != null && usuario.getRol().getNombre() != null
            ? usuario.getRol().getNombre().name()
            : "";
    }

    private String mensajeCompromisos(long pedidos, long solicitudes) {
        List<String> partes = new ArrayList<>();
        if (pedidos > 0) {
            partes.add(pedidos + (pedidos == 1
                ? " pedido abierto"
                : " pedidos abiertos"));
        }
        if (solicitudes > 0) {
            partes.add(solicitudes + (solicitudes == 1
                ? " solicitud abierta"
                : " solicitudes abiertas"));
        }
        return "No podés darte de baja: tenés " + String.join(" y ", partes)
            + ". Terminá o cancelá esos compromisos primero.";
    }
}
