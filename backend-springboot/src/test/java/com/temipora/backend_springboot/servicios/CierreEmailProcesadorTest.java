package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.temipora.backend_springboot.modelo.CierreSustitucionDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionSustitucion;
import com.temipora.backend_springboot.modelo.EmailCierrePendiente;
import com.temipora.backend_springboot.modelo.EnvioEmail;
import com.temipora.backend_springboot.modelo.EstadoEmailCierre;
import com.temipora.backend_springboot.modelo.EstadoEnvioEmail;
import com.temipora.backend_springboot.modelo.MotivoSustitucion;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.SustitucionOpcionDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EmailCierrePendienteRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class CierreEmailProcesadorTest {

    @Mock
    private EmailCierrePendienteRepository repository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ConfiguracionSustitucionService configuracionService;
    @Mock
    private SustitucionService sustitucionService;
    @Mock
    private EmailPlantillaService emailPlantillaService;
    @Mock
    private EnvioEmailService envioEmailService;
    @Mock
    private AccesoEnlaceService accesoEnlaceService;
    @Mock
    private SancionService sancionService;

    @InjectMocks
    private CierreEmailProcesador procesador;

    @Test
    void procesar_enviaUnMailYMarcaLosCierres() {
        Usuario usuario = usuario("a@mail.com");
        EmailCierrePendiente fila = fila(usuario, 5L);
        prepararUsuario(usuario, fila);
        when(sustitucionService.detalleCierrePorSolicitud(5L, 1L))
            .thenReturn(detalle());
        when(accesoEnlaceService.generar(any(), any(), any(), anyInt()))
            .thenReturn("token");
        when(emailPlantillaService.asuntoCierre(anyList()))
            .thenReturn("asunto");
        when(emailPlantillaService.cierre(
            anyString(), anyList(), anyList(), anyString()))
            .thenReturn("<html>");
        when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(envioEmailService.enviar(
            any(), anyString(), any(), any(), anyString(), anyString(), anyString()))
            .thenReturn(envioEnviado());

        boolean enviado = procesador.procesar(1L);

        assertThat(enviado).isTrue();
        assertThat(fila.getEstado()).isEqualTo(EstadoEmailCierre.ENVIADO);
        assertThat(fila.getEnviadoEn()).isNotNull();
        verify(envioEmailService).enviar(
            eq(usuario), eq("CIERRE"), any(), any(),
            eq("asunto"), eq("<html>"), anyString());
    }

    @Test
    void procesar_detalleInvalido_marcaOmitidoSinEnviar() {
        Usuario usuario = usuario("a@mail.com");
        EmailCierrePendiente fila = fila(usuario, 5L);
        prepararUsuario(usuario, fila);
        when(sustitucionService.detalleCierrePorSolicitud(5L, 1L))
            .thenThrow(new ConflictoException("ya no aplica"));

        boolean enviado = procesador.procesar(1L);

        assertThat(enviado).isFalse();
        assertThat(fila.getEstado()).isEqualTo(EstadoEmailCierre.OMITIDO);
        verify(envioEmailService, never()).enviar(
            any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void procesar_noOfreceServiciosQueElMailInformaComoCerrados() {
        Usuario usuario = usuario("a@mail.com");
        EmailCierrePendiente fila5 = fila(usuario, 5L);
        EmailCierrePendiente fila6 = fila(usuario, 6L);
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(usuario));
        when(configuracionService.obtener()).thenReturn(config());
        when(repository.findByUsuarioIdUsuarioAndEstadoOrderByCreadoEnAsc(
            eq(1L), eq(EstadoEmailCierre.PENDIENTE), any(Pageable.class)))
            .thenReturn(List.of(fila5, fila6));
        when(sustitucionService.detalleCierrePorSolicitud(5L, 1L))
            .thenReturn(detalleConOpciones(
                5L, 10L, "Retrato",
                opcion(11L, "Otro retrato"),
                opcion(20L, "Servicio que se informa cerrado")));
        when(sustitucionService.detalleCierrePorSolicitud(6L, 1L))
            .thenReturn(detalleConOpciones(
                6L, 20L, "Servicio que se informa cerrado"));
        when(accesoEnlaceService.generar(any(), any(), any(), anyInt()))
            .thenReturn("token");
        when(emailPlantillaService.asuntoCierre(anyList())).thenReturn("asunto");
        ArgumentCaptor<List<EmailPlantillaService.AlternativaEmail>> captor =
            ArgumentCaptor.forClass(List.class);
        when(emailPlantillaService.cierre(
            anyString(), anyList(), captor.capture(), anyString()))
            .thenReturn("<html>");
        when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(envioEmailService.enviar(
            any(), anyString(), any(), any(), anyString(), anyString(), anyString()))
            .thenReturn(envioEnviado());

        procesador.procesar(1L);

        assertThat(captor.getValue())
            .extracting(EmailPlantillaService.AlternativaEmail::idServicio)
            .containsExactly(11L);
    }

    @Test
    void procesar_usuarioSancionado_noEnviaCorreo() {
        Usuario usuario = usuario("a@mail.com");
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(sancionService.obtenerVigente(1L))
            .thenReturn(Optional.of(new SancionCuenta()));

        boolean enviado = procesador.procesar(1L);

        assertThat(enviado).isFalse();
        verify(envioEmailService, never()).enviar(
            any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void procesar_usuarioSinCorreo_marcaOmitido() {
        Usuario usuario = usuario(null);
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(usuario));
        when(repository.findByUsuarioIdUsuarioAndEstadoOrderByCreadoEnAsc(
            eq(1L), eq(EstadoEmailCierre.PENDIENTE), any(Pageable.class)))
            .thenReturn(List.of(fila(usuario, 5L)));

        boolean enviado = procesador.procesar(1L);

        assertThat(enviado).isFalse();
        verify(envioEmailService, never()).enviar(
            any(), any(), any(), any(), any(), any(), any());
    }

    private void prepararUsuario(Usuario usuario, EmailCierrePendiente fila) {
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(usuario));
        when(configuracionService.obtener()).thenReturn(config());
        when(repository.findByUsuarioIdUsuarioAndEstadoOrderByCreadoEnAsc(
            eq(1L), eq(EstadoEmailCierre.PENDIENTE), any(Pageable.class)))
            .thenReturn(List.of(fila));
    }

    private ConfiguracionSustitucion config() {
        ConfiguracionSustitucion config = new ConfiguracionSustitucion();
        config.setEmailCierreHabilitado(true);
        config.setEmailCierreMax(5);
        config.setEmailCierreVigenciaHoras(72);
        return config;
    }

    private Usuario usuario(String correo) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1L);
        usuario.setNombreUsuario("cliente");
        usuario.setNombreDisplay("Cliente");
        usuario.setCorreo(correo);
        usuario.setEstadoCuenta(true);
        return usuario;
    }

    private EmailCierrePendiente fila(Usuario usuario, Long idSolicitud) {
        Solicitud solicitud = new Solicitud();
        solicitud.setIdSolicitud(idSolicitud);
        EmailCierrePendiente fila = new EmailCierrePendiente();
        fila.setUsuario(usuario);
        fila.setSolicitud(solicitud);
        fila.setMotivo(MotivoSustitucion.SOLICITUD_RECHAZADA);
        fila.setEstado(EstadoEmailCierre.PENDIENTE);
        fila.setFechaEvento(LocalDateTime.now());
        return fila;
    }

    private CierreSustitucionDTO detalle() {
        ServicioDTO servicio = ServicioDTO.builder()
            .idServicio(10L)
            .nombre("Retrato")
            .nombreArtista("artista")
            .precioBaseMostrado(new BigDecimal("100.00"))
            .monedaMostrada("ARS")
            .build();
        SustitucionOpcionDTO opcion = SustitucionOpcionDTO.builder()
            .idServicio(11L)
            .orden(0)
            .motivo("Mismo estilo")
            .disponible(true)
            .servicio(ServicioDTO.builder()
                .idServicio(11L)
                .nombre("Otro retrato")
                .nombreArtista("otro")
                .precioBaseMostrado(new BigDecimal("120.00"))
                .monedaMostrada("ARS")
                .build())
            .build();
        return CierreSustitucionDTO.builder()
            .contexto("SOLICITUD")
            .idSolicitud(5L)
            .motivo("SOLICITUD_RECHAZADA")
            .titulo("El artista rechazó tu solicitud")
            .detalle("Podés buscar otro artista.")
            .fecha(LocalDateTime.now())
            .servicio(servicio)
            .procesoHabilitado(true)
            .opciones(List.of(opcion))
            .build();
    }

    private CierreSustitucionDTO detalleConOpciones(
            Long idSolicitud,
            Long idServicio,
            String nombre,
            SustitucionOpcionDTO... opciones) {
        ServicioDTO servicio = ServicioDTO.builder()
            .idServicio(idServicio)
            .nombre(nombre)
            .nombreArtista("artista")
            .precioBaseMostrado(new BigDecimal("100.00"))
            .monedaMostrada("ARS")
            .build();
        return CierreSustitucionDTO.builder()
            .contexto("SOLICITUD")
            .idSolicitud(idSolicitud)
            .idSustitucion(99L)
            .motivo("SOLICITUD_RECHAZADA")
            .titulo("El artista rechazó tu solicitud")
            .detalle("Podés buscar otro artista.")
            .fecha(LocalDateTime.now())
            .servicio(servicio)
            .procesoHabilitado(true)
            .opciones(List.of(opciones))
            .build();
    }

    private SustitucionOpcionDTO opcion(Long idServicio, String nombre) {
        return SustitucionOpcionDTO.builder()
            .idServicio(idServicio)
            .servicio(ServicioDTO.builder()
                .idServicio(idServicio)
                .nombre(nombre)
                .nombreArtista("otro")
                .precioBaseMostrado(new BigDecimal("120.00"))
                .monedaMostrada("ARS")
                .build())
            .build();
    }

    private EnvioEmail envioEnviado() {
        EnvioEmail envio = new EnvioEmail();
        envio.setEstado(EstadoEnvioEmail.ENVIADO);
        return envio;
    }
}
