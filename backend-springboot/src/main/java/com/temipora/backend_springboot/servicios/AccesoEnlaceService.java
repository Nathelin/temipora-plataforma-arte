package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.config.JwtUtils;
import com.temipora.backend_springboot.modelo.PropositoToken;
import com.temipora.backend_springboot.modelo.TokenAcceso;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.TokenAccesoRepository;

/**
 * Acceso por enlace enviado por mail: un token aleatorio, de un solo uso y
 * con vencimiento que se cambia por una sesión. Sirve tanto para las ofertas
 * de reactivación como para los mails de cierre.
 */
@Service
public class AccesoEnlaceService {

    private final TokenAccesoRepository tokenRepository;
    private final UsuarioService usuarioService;
    private final JwtUtils jwtUtils;
    private final AuditoriaService auditoria;

    public AccesoEnlaceService(
            TokenAccesoRepository tokenRepository,
            UsuarioService usuarioService,
            JwtUtils jwtUtils,
            AuditoriaService auditoria) {
        this.tokenRepository = tokenRepository;
        this.usuarioService = usuarioService;
        this.jwtUtils = jwtUtils;
        this.auditoria = auditoria;
    }

    @Transactional
    public String generar(
            Usuario usuario,
            PropositoToken proposito,
            Long idReferencia,
            int vigenciaHoras) {
        if (usuario == null || usuario.getIdUsuario() == null) {
            throw new IllegalArgumentException(
                "No se puede generar un enlace sin usuario.");
        }
        String crudo = TokenAccesoUtil.generar();
        TokenAcceso token = new TokenAcceso();
        token.setUsuario(usuario);
        token.setProposito(proposito);
        token.setIdReferencia(idReferencia);
        token.setTokenHash(TokenAccesoUtil.hash(crudo));
        token.setExpira(LocalDateTime.now()
            .plusHours(Math.max(1, vigenciaHoras)));
        tokenRepository.save(token);
        return crudo;
    }

    @Transactional
    public Map<String, Object> consumir(String tokenCrudo) {
        LocalDateTime ahora = LocalDateTime.now();
        if (tokenCrudo == null || tokenCrudo.isBlank()) {
            auditarFallo(null, null, "Token vacío");
            throw new AccesoEnlaceInvalidoException(
                "El enlace de acceso no es válido.");
        }

        TokenAcceso token = tokenRepository
            .findByTokenHashForUpdate(TokenAccesoUtil.hash(tokenCrudo))
            .orElse(null);
        if (token == null) {
            auditarFallo(null, null, "Token inválido");
            throw new AccesoEnlaceInvalidoException(
                "El enlace de acceso no es válido.");
        }

        Usuario usuario = token.getUsuario();
        String nombreUsuario = usuario != null ? usuario.getNombreUsuario() : null;
        String proposito = token.getProposito() != null
            ? token.getProposito().name()
            : null;

        if (token.getUsadoEn() != null) {
            auditarFallo(nombreUsuario, proposito, "Enlace ya usado");
            throw new AccesoEnlaceInvalidoException(
                "Este enlace ya fue usado. Iniciá sesión manualmente.");
        }
        if (token.getExpira() == null || !token.getExpira().isAfter(ahora)) {
            auditarFallo(nombreUsuario, proposito, "Enlace vencido");
            throw new AccesoEnlaceInvalidoException(
                "Este enlace venció. Iniciá sesión manualmente.");
        }
        if (usuario == null || !usuario.isEstadoCuenta()) {
            auditarFallo(nombreUsuario, proposito, "Cuenta inactiva");
            throw new AccesoEnlaceInvalidoException("La cuenta no está activa.");
        }

        token.setUsadoEn(ahora);
        tokenRepository.save(token);
        usuarioService.actualizarUltimaConexion(usuario.getNombreUsuario());

        String rol = usuario.getRol() != null
                && usuario.getRol().getNombre() != null
            ? usuario.getRol().getNombre().name()
            : "";
        String jwt = jwtUtils.generarTokenAcceso(
            usuario.getNombreUsuario(), rol);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("token", jwt);
        respuesta.put("Message", "Autenticación realizada correctamente");
        respuesta.put("NombreUsuario", usuario.getNombreUsuario());
        respuesta.put("rol", rol);
        respuesta.put("idUsuario", usuario.getIdUsuario());
        respuesta.put("urlFotoPerfil", usuario.getUrlFotoPerfil());
        respuesta.put("proposito", proposito);
        respuesta.put("idReferencia", token.getIdReferencia());

        auditarFallo(nombreUsuario, proposito, null);
        return respuesta;
    }

    private void auditarFallo(String usuario, String proposito, String motivo) {
        auditoria.registrarAccesoEnlace(
            usuario, proposito, motivo == null, motivo);
    }
}
