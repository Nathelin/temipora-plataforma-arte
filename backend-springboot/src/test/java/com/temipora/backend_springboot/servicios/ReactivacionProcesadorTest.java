package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.temipora.backend_springboot.modelo.CampanaReactivacion;
import com.temipora.backend_springboot.modelo.ConfiguracionComision;
import com.temipora.backend_springboot.modelo.ConfiguracionInactividad;
import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioImagen;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CampanaReactivacionRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ReactivacionProcesadorTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private ComisionService comisionService;

    @Mock
    private RecomendacionService recomendacionService;

    @Mock
    private CampanaReactivacionRepository campanaRepository;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private EnvioEmailService envioEmailService;

    @Mock
    private EmailPlantillaService emailPlantillaService;

    @Mock
    private MonedaService monedaService;

    @Mock
    private AccesoEnlaceService accesoEnlaceService;

    @Mock
    private SancionService sancionService;

    @InjectMocks
    private ReactivacionProcesador procesador;

    private final LocalDateTime ahora = LocalDateTime.of(2026, 6, 1, 9, 0);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(procesador, "baseUrl", "http://localhost:5173");
    }

    @Test
    void procesar_usuarioInexistente_devuelveCero() {
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.empty());

        assertThat(procesar()).isZero();
    }

    @Test
    void procesar_usuarioInactivo_devuelveCero() {
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(usuario(1L, false, true, EPreferenciaContenidoSensible.ADVERTIR)));

        assertThat(procesar()).isZero();
    }

    @Test
    void procesar_administrador_devuelveCeroSinCampanaNiCorreo() {
        Usuario admin = usuario(1L, true, true,
            EPreferenciaContenidoSensible.ADVERTIR);
        admin.setRol(Rol.builder().nombre(ERol.ADMIN).build());
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(admin));

        assertThat(procesar()).isZero();

        verify(campanaRepository, never()).save(any());
        verify(notificacionService, never()).crearParaUsuario(
            any(), any(), any(), any(), any(), any());
        verify(envioEmailService, never()).enviar(
            any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void procesar_usuarioSancionado_devuelveCeroSinCampanaNiCorreo() {
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(usuario(1L, true, true,
                EPreferenciaContenidoSensible.ADVERTIR)));
        when(sancionService.obtenerVigente(1L))
            .thenReturn(Optional.of(new SancionCuenta()));

        assertThat(procesar()).isZero();

        verify(campanaRepository, never()).save(any());
        verify(notificacionService, never()).crearParaUsuario(
            any(), any(), any(), any(), any(), any());
        verify(envioEmailService, never()).enviar(
            any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void procesar_conSolicitudActiva_devuelveCero() {
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(usuario(1L, true, true, EPreferenciaContenidoSensible.ADVERTIR)));
        when(solicitudRepository.serviciosConSolicitudActiva(eq(1L), any()))
            .thenReturn(List.of(9L));

        assertThat(procesar()).isZero();
    }

    @Test
    void procesar_sinCandidatosSeleccionables_devuelveCero() {
        prepararUsuario();
        when(monedaService.predeterminada()).thenReturn(moneda());
        when(recomendacionService.rankear(any(), eq(1L), any()))
            .thenReturn(new RecomendacionService.ResultadoRanking(List.of(), true));

        assertThat(procesar()).isZero();
        verify(campanaRepository, never()).save(any());
    }

    @Test
    void procesar_rankingNoPersonalizado_usaElFallbackDeCalidad() {
        prepararUsuario();
        when(monedaService.predeterminada()).thenReturn(moneda());
        when(recomendacionService.rankear(any(), eq(1L), any()))
            .thenReturn(new RecomendacionService.ResultadoRanking(List.of(), false));
        when(recomendacionService.rankearPorCalidad(any(), any()))
            .thenReturn(List.of(puntuado(servicio(10L, usuario(2L), false), "m")));
        prepararPersistencia();

        assertThat(procesar()).isEqualTo(1);
        verify(recomendacionService).rankearPorCalidad(any(), any());
    }

    @Test
    void procesar_omiteServicioPropioYSensibles() {
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(usuario(1L, true, false, EPreferenciaContenidoSensible.OCULTAR)));
        when(solicitudRepository.serviciosConSolicitudActiva(eq(1L), any()))
            .thenReturn(List.of());
        when(monedaService.predeterminada()).thenReturn(moneda());

        Servicio propio = servicio(10L, usuario(1L), false);
        Servicio sensible = servicio(11L, usuario(2L), true);
        Servicio valido = servicio(12L, usuario(3L), false);
        when(recomendacionService.rankear(any(), eq(1L), any()))
            .thenReturn(new RecomendacionService.ResultadoRanking(
                List.of(puntuado(propio, "a"), puntuado(sensible, "b"), puntuado(valido, "c")),
                true));
        prepararPersistencia();

        assertThat(procesar()).isEqualTo(1);
        assertThat(campanaCapturada().getOfertas()).hasSize(1);
        assertThat(campanaCapturada().getOfertas().get(0).getServicio().getIdServicio())
            .isEqualTo(12L);
    }

    @Test
    void procesar_limitaLaCantidadDeOfertas() {
        prepararUsuario();
        when(monedaService.predeterminada()).thenReturn(moneda());
        when(recomendacionService.rankear(any(), eq(1L), any()))
            .thenReturn(new RecomendacionService.ResultadoRanking(
                List.of(puntuado(servicio(10L, usuario(2L), false), "a"),
                    puntuado(servicio(11L, usuario(3L), false), "b")),
                true));
        prepararPersistencia();
        ConfiguracionInactividad config = config();
        config.setMaxOfertasPorUsuario(1);

        assertThat(procesador.procesar(1L, config, candidatos(), ahora)).isEqualTo(1);
        assertThat(campanaCapturada().getOfertas()).hasSize(1);
    }

    @Test
    void procesar_exito_notificaYEnviaElEmail() {
        Usuario usuario = usuario(1L, true, true, EPreferenciaContenidoSensible.ADVERTIR);
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(solicitudRepository.serviciosConSolicitudActiva(eq(1L), any()))
            .thenReturn(List.of());
        when(monedaService.predeterminada()).thenReturn(moneda());
        Servicio servicio = servicio(10L, usuario(2L), false);
        when(recomendacionService.rankear(any(), eq(1L), any()))
            .thenReturn(new RecomendacionService.ResultadoRanking(
                List.of(puntuado(servicio, "m")), true));
        prepararPersistencia();

        assertThat(procesar()).isEqualTo(1);

        verify(notificacionService).crearParaUsuario(
            eq(1L), eq("OFERTA_REACTIVACION"), anyString(), anyString(), any(), any());
        verify(envioEmailService).enviar(
            eq(usuario), eq("REACTIVACION"), any(), eq("CAMPANA_REACTIVACION"),
            anyString(), any(), anyString());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void procesar_sinEmailsPromocionales_noEnviaCorreo() {
        prepararUsuario();
        when(monedaService.predeterminada()).thenReturn(moneda());
        when(recomendacionService.rankear(any(), eq(1L), any()))
            .thenReturn(new RecomendacionService.ResultadoRanking(
                List.of(puntuado(servicio(10L, usuario(2L), false), "m")), true));
        prepararPersistencia();

        assertThat(procesar()).isEqualTo(1);

        verify(envioEmailService, never()).enviar(
            any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void procesar_emailConTokenExistenteYGaleriaDeRespaldo() {
        Usuario usuario = usuario(1L, true, true, EPreferenciaContenidoSensible.ADVERTIR);
        usuario.setTokenBajaEmail("token-existente");
        usuario.setNombreDisplay("Cosmo");
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(usuario));
        when(solicitudRepository.serviciosConSolicitudActiva(eq(1L), any()))
            .thenReturn(List.of());
        when(monedaService.predeterminada()).thenReturn(moneda());

        Servicio servicio = servicio(10L, usuario(2L), false);
        servicio.setImagenPortada(null);
        ServicioImagen imagen = new ServicioImagen();
        imagen.setUrl("galeria.png");
        imagen.setOrden(0);
        servicio.getImagenes().add(imagen);

        when(recomendacionService.rankear(any(), eq(1L), any()))
            .thenReturn(new RecomendacionService.ResultadoRanking(
                List.of(puntuado(servicio, "m")), true));
        prepararPersistencia();

        assertThat(procesar()).isEqualTo(1);

        verify(envioEmailService).enviar(
            eq(usuario), eq("REACTIVACION"), any(), eq("CAMPANA_REACTIVACION"),
            anyString(), any(), anyString());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private int procesar() {
        return procesador.procesar(1L, config(), candidatos(), ahora);
    }

    private void prepararUsuario() {
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(usuario(1L, true, false, EPreferenciaContenidoSensible.ADVERTIR)));
        when(solicitudRepository.serviciosConSolicitudActiva(eq(1L), any()))
            .thenReturn(List.of());
    }

    private void prepararPersistencia() {
        ConfiguracionComision comision = new ConfiguracionComision();
        comision.setPorcentajeServicio(new BigDecimal("10.00"));
        when(comisionService.obtener()).thenReturn(comision);
        when(campanaRepository.save(any(CampanaReactivacion.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(accesoEnlaceService.generar(any(), any(), any(), anyInt()))
            .thenReturn("token-acceso");
    }

    private CampanaReactivacion campanaCapturada() {
        ArgumentCaptor<CampanaReactivacion> captor =
            ArgumentCaptor.forClass(CampanaReactivacion.class);
        verify(campanaRepository).save(captor.capture());
        return captor.getValue();
    }

    private List<Servicio> candidatos() {
        return List.of(
            servicio(10L, usuario(2L), false),
            servicio(11L, usuario(3L), false));
    }

    private Usuario usuario(Long id) {
        return usuario(id, true, false, EPreferenciaContenidoSensible.ADVERTIR);
    }

    private Usuario usuario(
            Long id, boolean estado, boolean emails, EPreferenciaContenidoSensible pref) {
        return Usuario.builder()
            .idUsuario(id)
            .nombreUsuario("u" + id)
            .correo("u" + id + "@mail.com")
            .estadoCuenta(estado)
            .recibirEmailsPromocionales(emails)
            .preferenciaSensible(pref)
            .rol(Rol.builder().nombre(ERol.CLIENTE).build())
            .build();
    }

    private Servicio servicio(Long id, Usuario artista, boolean sensible) {
        Servicio servicio = new Servicio();
        servicio.setIdServicio(id);
        servicio.setArtista(artista);
        servicio.setNombre("Servicio " + id);
        servicio.setDescripcion("desc");
        servicio.setPrecioBase(new BigDecimal("100.00"));
        servicio.setMoneda(moneda());
        servicio.setContenidoSensible(sensible);
        servicio.setImagenPortada("portada.png");
        return servicio;
    }

    private RecomendacionService.ServicioPuntuado puntuado(Servicio servicio, String motivo) {
        return new RecomendacionService.ServicioPuntuado(servicio, 1.0, motivo);
    }

    private Moneda moneda() {
        return Moneda.builder().idMoneda(1L).codigo("ARS").nombre("Peso").build();
    }

    private ConfiguracionInactividad config() {
        ConfiguracionInactividad config = new ConfiguracionInactividad();
        config.setMaxOfertasPorUsuario(3);
        config.setPorcentajeDescuento(new BigDecimal("10.00"));
        config.setVigenciaHoras(72);
        return config;
    }
}
