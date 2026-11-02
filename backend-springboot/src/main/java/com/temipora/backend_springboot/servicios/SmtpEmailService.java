package com.temipora.backend_springboot.servicios;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;

    @Value("${temipora.mail.habilitado:true}")
    private boolean habilitado;

    @Value("${temipora.mail.remitente:no-responder@temipora.local}")
    private String remitente;

    @Value("${temipora.mail.remitente-nombre:Temipora}")
    private String remitenteNombre;

    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public boolean enviar(EmailMensaje mensaje) {
        if (!habilitado) {
            log.info("Email deshabilitado: se omite el envío a {}", mensaje.destinatario());
            return false;
        }
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(remitente, remitenteNombre);
            helper.setTo(mensaje.destinatario());
            helper.setSubject(mensaje.asunto());
            helper.setText(mensaje.html(), true);
            mailSender.send(mime);
            return true;
        } catch (Exception e) {
            throw new EmailEnvioException(
                "No se pudo enviar el email a " + mensaje.destinatario(), e);
        }
    }
}
