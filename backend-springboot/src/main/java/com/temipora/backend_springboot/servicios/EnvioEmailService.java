package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EnvioEmail;
import com.temipora.backend_springboot.modelo.EstadoEnvioEmail;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EnvioEmailRepository;

@Service
public class EnvioEmailService {

    private static final Logger log = LoggerFactory.getLogger(EnvioEmailService.class);
    private static final int MAX_ERROR = 500;

    private final EnvioEmailRepository envioEmailRepository;
    private final EmailService emailService;

    public EnvioEmailService(
            EnvioEmailRepository envioEmailRepository,
            EmailService emailService) {
        this.envioEmailRepository = envioEmailRepository;
        this.emailService = emailService;
    }

    @Transactional
    public EnvioEmail enviar(
            Usuario usuario,
            String tipo,
            Long idReferencia,
            String tipoReferencia,
            String asunto,
            String html,
            String claveIdempotencia) {
        EnvioEmail existente = envioEmailRepository
            .findByClaveIdempotencia(claveIdempotencia)
            .orElse(null);
        if (existente != null) {
            log.debug("Envío ya registrado para {}", claveIdempotencia);
            return existente;
        }

        EnvioEmail envio = new EnvioEmail();
        envio.setUsuario(usuario);
        envio.setTipo(tipo);
        envio.setIdReferencia(idReferencia);
        envio.setTipoReferencia(tipoReferencia);
        envio.setDestinatario(
            usuario.getCorreo() != null ? usuario.getCorreo() : "");
        envio.setAsunto(asunto);
        envio.setClaveIdempotencia(claveIdempotencia);
        envio.setEstado(EstadoEnvioEmail.PENDIENTE);
        envio.setFechaCreacion(LocalDateTime.now());
        envio = envioEmailRepository.save(envio);

        if (usuario.getCorreo() == null || usuario.getCorreo().isBlank()) {
            envio.setEstado(EstadoEnvioEmail.FALLIDO);
            envio.setError("El usuario no tiene un correo configurado.");
            return envioEmailRepository.save(envio);
        }

        try {
            boolean enviado = emailService.enviar(
                new EmailMensaje(usuario.getCorreo(), asunto, html));
            if (enviado) {
                envio.setEstado(EstadoEnvioEmail.ENVIADO);
                envio.setProveedor("smtp");
                envio.setFechaEnvio(LocalDateTime.now());
            } else {
                envio.setEstado(EstadoEnvioEmail.PENDIENTE);
                envio.setError("El envío de emails está deshabilitado.");
            }
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el email {}: {}", claveIdempotencia, e.getMessage());
            envio.setEstado(EstadoEnvioEmail.FALLIDO);
            envio.setError(truncar(e.getMessage()));
        }
        return envioEmailRepository.save(envio);
    }

    private String truncar(String texto) {
        if (texto == null) {
            return "Error desconocido";
        }
        return texto.length() <= MAX_ERROR ? texto : texto.substring(0, MAX_ERROR);
    }
}
