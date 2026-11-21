package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.modelo.ApelacionSancion;
import com.temipora.backend_springboot.modelo.ApelacionSancionDTO;
import com.temipora.backend_springboot.modelo.ConfiguracionPedido;
import com.temipora.backend_springboot.modelo.EstadoApelacion;
import com.temipora.backend_springboot.modelo.EstadoSancion;
import com.temipora.backend_springboot.modelo.OrigenSancion;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.TipoSancion;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ApelacionSancionRepository;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ApelacionServiceTest {

    @Mock
    private ApelacionSancionRepository apelacionRepository;

    @Mock
    private SancionCuentaRepository sancionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ConfiguracionPedidoService configuracionPedidoService;

    @Mock
    private SancionAutomaticaService sancionAutomaticaService;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private ApelacionService service;

    @Test
    void crear_ok_guardaLaApelacionPendiente() {
        SancionCuenta sancion = sancion();
        Usuario cliente = cliente();
        prepararCreacion(sancion);
        when(configuracionPedidoService.obtener()).thenReturn(configuracion(7));
        when(apelacionRepository.findFirstBySancionIdSancionOrderByCreadaEnDesc(9L))
            .thenReturn(Optional.empty());
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(cliente));
        when(apelacionRepository.save(any(ApelacionSancion.class)))
            .thenAnswer(inv -> {
                ApelacionSancion guardada = inv.getArgument(0);
                guardada.setIdApelacion(5L);
                return guardada;
            });

        ApelacionSancionDTO dto = service.crear(1L, 9L, "  No pude entregar  ");

        assertThat(dto.getIdApelacion()).isEqualTo(5L);
        assertThat(dto.getEstado()).isEqualTo(EstadoApelacion.PENDIENTE);
        assertThat(dto.getNombreUsuario()).isEqualTo("cliente");
        verify(auditoria).registrar(
            eq(AccionesAdmin.APELACION_CREAR), eq("ApelacionSancion"), eq(5L),
            any(), any());
    }

    @Test
    void crear_deOtroUsuario_lanzaAccesoDenegado() {
        when(sancionRepository.findById(9L))
            .thenReturn(Optional.of(sancion()));

        assertThatThrownBy(() -> service.crear(2L, 9L, "motivo"))
            .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void crear_sancionManual_lanzaConflicto() {
        SancionCuenta sancion = sancion();
        sancion.setOrigen(OrigenSancion.MANUAL);
        when(sancionRepository.findById(9L)).thenReturn(Optional.of(sancion));

        assertThatThrownBy(() -> service.crear(1L, 9L, "motivo"))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("automáticas");
    }

    @Test
    void crear_conApelacionPendiente_lanzaConflicto() {
        SancionCuenta sancion = sancion();
        prepararCreacion(sancion);
        when(apelacionRepository.findFirstBySancionIdSancionAndEstado(
                9L, EstadoApelacion.PENDIENTE))
            .thenReturn(Optional.of(new ApelacionSancion()));

        assertThatThrownBy(() -> service.crear(1L, 9L, "motivo"))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("pendiente");
    }

    @Test
    void crear_dentroDeLaEspera_lanzaConflicto() {
        SancionCuenta sancion = sancion();
        prepararCreacion(sancion);
        when(configuracionPedidoService.obtener()).thenReturn(configuracion(7));
        ApelacionSancion ultima = new ApelacionSancion();
        ultima.setCreadaEn(LocalDateTime.now().minusDays(1));
        when(apelacionRepository.findFirstBySancionIdSancionOrderByCreadaEnDesc(9L))
            .thenReturn(Optional.of(ultima));

        assertThatThrownBy(() -> service.crear(1L, 9L, "motivo"))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("7 días");
    }

    @Test
    void resolver_aceptada_revierteLaSancionYNotifica() {
        SancionCuenta sancion = sancion();
        ApelacionSancion apelacion = apelacionPendiente(sancion);
        prepararResolucion(apelacion);

        ApelacionSancionDTO dto = service.resolver(99L, 5L, true, "Corresponde");

        assertThat(dto.getEstado()).isEqualTo(EstadoApelacion.ACEPTADA);
        verify(sancionAutomaticaService).revertirYRestaurar(
            eq(9L), contains("Apelación aceptada"));
        verify(notificacionService).crearParaUsuario(
            eq(1L), eq("APELACION_RESUELTA"), any(), any(), any(), eq("PEDIDO"));
    }

    @Test
    void resolver_rechazada_noRevierteLaSancion() {
        SancionCuenta sancion = sancion();
        ApelacionSancion apelacion = apelacionPendiente(sancion);
        prepararResolucion(apelacion);

        ApelacionSancionDTO dto = service.resolver(99L, 5L, false, "No corresponde");

        assertThat(dto.getEstado()).isEqualTo(EstadoApelacion.RECHAZADA);
        verify(sancionAutomaticaService, never())
            .revertirYRestaurar(any(), anyString());
    }

    @Test
    void resolver_yaResuelta_lanzaConflicto() {
        ApelacionSancion apelacion = apelacionPendiente(sancion());
        apelacion.setEstado(EstadoApelacion.ACEPTADA);
        when(apelacionRepository.findById(5L)).thenReturn(Optional.of(apelacion));

        assertThatThrownBy(() -> service.resolver(99L, 5L, true, null))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("ya fue resuelta");
    }

    private void prepararCreacion(SancionCuenta sancion) {
        when(sancionRepository.findById(9L)).thenReturn(Optional.of(sancion));
        when(apelacionRepository.findFirstBySancionIdSancionAndEstado(
                9L, EstadoApelacion.PENDIENTE))
            .thenReturn(Optional.empty());
    }

    private void prepararResolucion(ApelacionSancion apelacion) {
        when(apelacionRepository.findById(5L)).thenReturn(Optional.of(apelacion));
        when(usuarioRepository.findByIdUsuario(99L))
            .thenReturn(Optional.of(admin()));
        when(apelacionRepository.save(any(ApelacionSancion.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    private ConfiguracionPedido configuracion(int diasReapelacion) {
        ConfiguracionPedido config = new ConfiguracionPedido();
        config.setDiasReapelacion(diasReapelacion);
        return config;
    }

    private SancionCuenta sancion() {
        return SancionCuenta.builder()
            .idSancion(9L)
            .usuario(cliente())
            .tipo(TipoSancion.INHABILITACION)
            .alcance(AlcanceSancion.ENTREGAS)
            .origen(OrigenSancion.AUTOMATICA)
            .motivo("No entregó")
            .fechaInicio(LocalDateTime.now().minusDays(3))
            .estado(EstadoSancion.ACTIVA)
            .build();
    }

    private ApelacionSancion apelacionPendiente(SancionCuenta sancion) {
        ApelacionSancion apelacion = new ApelacionSancion();
        apelacion.setIdApelacion(5L);
        apelacion.setSancion(sancion);
        apelacion.setUsuario(cliente());
        apelacion.setMensaje("Estuve internado");
        apelacion.setEstado(EstadoApelacion.PENDIENTE);
        apelacion.setCreadaEn(LocalDateTime.now().minusDays(2));
        return apelacion;
    }

    private Usuario cliente() {
        return Usuario.builder()
            .idUsuario(1L)
            .nombreUsuario("cliente")
            .correo("cliente@mail.com")
            .build();
    }

    private Usuario admin() {
        return Usuario.builder()
            .idUsuario(99L)
            .nombreUsuario("admin")
            .correo("admin@mail.com")
            .build();
    }
}
