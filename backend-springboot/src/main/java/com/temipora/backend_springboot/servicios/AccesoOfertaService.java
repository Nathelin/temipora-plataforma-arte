package com.temipora.backend_springboot.servicios;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.config.JwtUtils;
import com.temipora.backend_springboot.modelo.AccesoOfertaVerificacionDTO;
import com.temipora.backend_springboot.modelo.CampanaReactivacion;
import com.temipora.backend_springboot.modelo.EstadoCampanaReactivacion;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CampanaReactivacionRepository;

@Service
public class AccesoOfertaService {

    private final CampanaReactivacionRepository campanaRepository;
    private final UsuarioService usuarioService;
    private final JwtUtils jwtUtils;

    public AccesoOfertaService(
            CampanaReactivacionRepository campanaRepository,
            UsuarioService usuarioService,
            JwtUtils jwtUtils) {
        this.campanaRepository = campanaRepository;
        this.usuarioService = usuarioService;
        this.jwtUtils = jwtUtils;
    }

    @Transactional(readOnly = true)
    public AccesoOfertaVerificacionDTO verificar(
            String tokenCampana,
            String tokenAcceso) {
        CampanaReactivacion campana = campanaRepository.findByToken(tokenCampana)
            .orElseThrow(() -> new AccesoOfertaInvalidoException(
                "El enlace de acceso no es válido."));
        validar(campana, tokenAcceso, LocalDateTime.now());
        Usuario usuario = campana.getUsuario();
        return new AccesoOfertaVerificacionDTO(
            usuario.getIdUsuario(),
            usuario.getNombreUsuario(),
            usuario.getNombreDisplay());
    }

    @Transactional
    public Map<String, Object> consumir(
            String tokenCampana,
            String tokenAcceso) {
        CampanaReactivacion campana = campanaRepository
            .findByTokenParaActualizar(tokenCampana)
            .orElseThrow(() -> new AccesoOfertaInvalidoException(
                "El enlace de acceso no es válido."));
        LocalDateTime ahora = LocalDateTime.now();
        validar(campana, tokenAcceso, ahora);

        campana.setTokenAccesoUsadoEn(ahora);
        campanaRepository.save(campana);

        Usuario usuario = campana.getUsuario();
        usuarioService.actualizarUltimaConexion(usuario.getNombreUsuario());

        String rol = usuario.getRol() != null
                && usuario.getRol().getNombre() != null
            ? usuario.getRol().getNombre().name()
            : "";
        String token = jwtUtils.generarTokenAcceso(
            usuario.getNombreUsuario(), rol);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("token", token);
        respuesta.put("Message", "Autenticación realizada correctamente");
        respuesta.put("NombreUsuario", usuario.getNombreUsuario());
        respuesta.put("rol", rol);
        respuesta.put("idUsuario", usuario.getIdUsuario());
        respuesta.put("urlFotoPerfil", usuario.getUrlFotoPerfil());
        return respuesta;
    }

    private void validar(
            CampanaReactivacion campana,
            String tokenAcceso,
            LocalDateTime ahora) {
        if (tokenAcceso == null || campana.getTokenAccesoHash() == null
                || !MessageDigest.isEqual(
                    campana.getTokenAccesoHash()
                        .getBytes(StandardCharsets.UTF_8),
                    TokenAccesoOferta.hash(tokenAcceso)
                        .getBytes(StandardCharsets.UTF_8))) {
            throw new AccesoOfertaInvalidoException(
                "El enlace de acceso no es válido.");
        }
        if (campana.getTokenAccesoUsadoEn() != null) {
            throw new AccesoOfertaInvalidoException(
                "Este enlace ya fue usado. Iniciá sesión manualmente.");
        }
        if (campana.getEstado() != EstadoCampanaReactivacion.ACTIVA
                || campana.getFechaVencimiento() == null
                || !campana.getFechaVencimiento().isAfter(ahora)
                || campana.getTokenAccesoExpira() == null
                || !campana.getTokenAccesoExpira().isAfter(ahora)) {
            throw new AccesoOfertaInvalidoException(
                "Este enlace venció. Iniciá sesión manualmente.");
        }
        Usuario usuario = campana.getUsuario();
        if (usuario == null || !usuario.isEstadoCuenta()) {
            throw new AccesoOfertaInvalidoException(
                "La cuenta no está activa.");
        }
    }
}
