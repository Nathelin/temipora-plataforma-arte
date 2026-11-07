package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ConfiguracionSustitucion;
import com.temipora.backend_springboot.modelo.EmailCierrePendiente;
import com.temipora.backend_springboot.modelo.EstadoEmailCierre;
import com.temipora.backend_springboot.modelo.MotivoSustitucion;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EmailCierrePendienteRepository;

@ExtendWith(MockitoExtension.class)
class CierreEmailServiceTest {

    @Mock
    private EmailCierrePendienteRepository repository;

    @Mock
    private ConfiguracionSustitucionService configuracionService;

    @Mock
    private CierreEmailProcesador procesador;

    @InjectMocks
    private CierreEmailService service;

    @Test
    void encolar_procesoDeshabilitado_noGuarda() {
        when(configuracionService.obtener()).thenReturn(config(false));

        service.encolar(usuario("a@mail.com"), solicitud(5L), null,
            MotivoSustitucion.SOLICITUD_RECHAZADA, LocalDateTime.now());

        verify(repository, never()).save(any());
    }

    @Test
    void encolar_sinCorreo_noGuarda() {
        service.encolar(usuario(null), solicitud(5L), null,
            MotivoSustitucion.SOLICITUD_RECHAZADA, LocalDateTime.now());

        verify(repository, never()).save(any());
    }

    @Test
    void encolar_duplicado_noGuarda() {
        when(configuracionService.obtener()).thenReturn(config(true));
        when(repository.existsBySolicitudIdSolicitudAndMotivo(
            5L, MotivoSustitucion.SOLICITUD_RECHAZADA)).thenReturn(true);

        service.encolar(usuario("a@mail.com"), solicitud(5L), null,
            MotivoSustitucion.SOLICITUD_RECHAZADA, LocalDateTime.now());

        verify(repository, never()).save(any());
    }

    @Test
    void encolar_valido_guardaPendiente() {
        when(configuracionService.obtener()).thenReturn(config(true));
        when(repository.existsBySolicitudIdSolicitudAndMotivo(
            5L, MotivoSustitucion.SOLICITUD_RECHAZADA)).thenReturn(false);
        when(repository.save(any(EmailCierrePendiente.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        service.encolar(usuario("a@mail.com"), solicitud(5L), null,
            MotivoSustitucion.SOLICITUD_RECHAZADA, LocalDateTime.now());

        verify(repository).save(any(EmailCierrePendiente.class));
    }

    private ConfiguracionSustitucion config(boolean habilitado) {
        ConfiguracionSustitucion config = new ConfiguracionSustitucion();
        config.setEmailCierreHabilitado(habilitado);
        return config;
    }

    private Usuario usuario(String correo) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);
        usuario.setNombreUsuario("cliente");
        usuario.setCorreo(correo);
        return usuario;
    }

    private Solicitud solicitud(Long id) {
        Solicitud solicitud = new Solicitud();
        solicitud.setIdSolicitud(id);
        return solicitud;
    }
}
