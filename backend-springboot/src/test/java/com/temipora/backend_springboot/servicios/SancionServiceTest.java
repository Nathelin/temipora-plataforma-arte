package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.temipora.backend_springboot.modelo.AlcanceSancion;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EstadoSancion;
import com.temipora.backend_springboot.modelo.OrigenSancion;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.SancionCuenta;
import com.temipora.backend_springboot.modelo.SancionCuentaDTO;
import com.temipora.backend_springboot.modelo.SancionRequest;
import com.temipora.backend_springboot.modelo.TipoSancion;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.SancionCuentaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class SancionServiceTest {

    @Mock
    private SancionCuentaRepository sancionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private SancionService sancionService;

    @Test
    void aplicar_suspensionTemporalValida_guardaYAudita() {
        Usuario admin = usuario(99L, "admin", ERol.ADMIN);
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE);
        when(usuarioRepository.findByIdUsuario(99L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(cliente));
        when(sancionRepository.findVigentesByUsuarioId(eq(1L), eq(EstadoSancion.ACTIVA), any()))
            .thenReturn(List.of());
        when(sancionRepository.save(any(SancionCuenta.class))).thenAnswer(inv -> {
            SancionCuenta guardada = inv.getArgument(0);
            guardada.setIdSancion(10L);
            return guardada;
        });

        SancionRequest request = request(1L, TipoSancion.SUSPENSION,
            LocalDateTime.now().plusDays(7), "Incumplimiento de las normas de la plataforma");

        SancionCuentaDTO dto = sancionService.aplicar(99L, request);

        assertThat(dto.getIdSancion()).isEqualTo(10L);
        assertThat(dto.getEstadoEfectivo()).isEqualTo("ACTIVA");
        assertThat(dto.isVigente()).isTrue();
        assertThat(dto.getTipo()).isEqualTo(TipoSancion.SUSPENSION);
        assertThat(dto.getNombreUsuario()).isEqualTo("cliente");
        verify(auditoria).registrar(
            eq(AccionesAdmin.CUENTA_SANCIONAR), eq("SancionCuenta"), eq(10L),
            any(), any(), any(), any());
    }

    @Test
    void aplicarSistema_conClaveRevertida_creaUnaSancionNueva() {
        String clave = "AUTO:ENTREGA:31:NIVEL1";
        Usuario artista = usuario(880L, "ArtistaMoroso", ERol.ARTISTA);
        SancionCuenta anterior = SancionCuenta.builder()
            .idSancion(5L)
            .usuario(artista)
            .alcance(AlcanceSancion.ENTREGAS)
            .origen(OrigenSancion.AUTOMATICA)
            .estado(EstadoSancion.REVERTIDA)
            .motivo("anterior")
            .clave(clave)
            .fechaInicio(LocalDateTime.now().minusDays(10))
            .build();

        when(sancionRepository.findByClave(clave))
            .thenReturn(Optional.of(anterior));
        when(sancionRepository.findFirstByUsuarioIdUsuarioAndEstado(
                eq(880L), eq(EstadoSancion.ACTIVA)))
            .thenReturn(Optional.empty());
        when(usuarioRepository.findByIdUsuario(880L))
            .thenReturn(Optional.of(artista));
        when(sancionRepository.save(any(SancionCuenta.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SancionCuenta nueva = sancionService.aplicarSistema(
            880L,
            TipoSancion.SUSPENSION,
            AlcanceSancion.ENTREGAS,
            "nuevo incumplimiento",
            LocalDateTime.now().plusDays(14),
            clave,
            null,
            null,
            LocalDateTime.now().minusDays(5));

        assertThat(anterior.getClave()).isNull();
        assertThat(nueva.getEstado()).isEqualTo(EstadoSancion.ACTIVA);
        assertThat(nueva.getClave()).isEqualTo(clave);
        assertThat(nueva.getAlcance()).isEqualTo(AlcanceSancion.ENTREGAS);
    }

    @Test
    void aplicar_suspensionSinFechaFin_lanzaErrorYNoGuarda() {
        Usuario admin = usuario(99L, "admin", ERol.ADMIN);
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE);
        when(usuarioRepository.findByIdUsuario(99L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(cliente));
        when(sancionRepository.findVigentesByUsuarioId(eq(1L), eq(EstadoSancion.ACTIVA), any()))
            .thenReturn(List.of());

        assertThatThrownBy(() -> sancionService.aplicar(99L,
            request(1L, TipoSancion.SUSPENSION, null, "motivo")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha de fin");

        verify(sancionRepository, never()).save(any());
    }

    @Test
    void aplicar_inhabilitacionConFechaFin_lanzaError() {
        Usuario admin = usuario(99L, "admin", ERol.ADMIN);
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE);
        when(usuarioRepository.findByIdUsuario(99L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(cliente));
        when(sancionRepository.findVigentesByUsuarioId(eq(1L), eq(EstadoSancion.ACTIVA), any()))
            .thenReturn(List.of());

        assertThatThrownBy(() -> sancionService.aplicar(99L,
            request(1L, TipoSancion.INHABILITACION, LocalDateTime.now().plusDays(1), "motivo")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no lleva fecha de fin");

        verify(sancionRepository, never()).save(any());
    }

    @Test
    void aplicar_cuentaAdministrador_lanzaError() {
        Usuario admin = usuario(99L, "admin", ERol.ADMIN);
        Usuario otroAdmin = usuario(1L, "otroAdmin", ERol.ADMIN);
        when(usuarioRepository.findByIdUsuario(99L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(otroAdmin));

        assertThatThrownBy(() -> sancionService.aplicar(99L,
            request(1L, TipoSancion.INHABILITACION, null, "motivo")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("administrador");

        verify(sancionRepository, never()).save(any());
    }

    @Test
    void aplicar_yaTieneSancionVigente_lanzaConflicto() {
        Usuario admin = usuario(99L, "admin", ERol.ADMIN);
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE);
        when(usuarioRepository.findByIdUsuario(99L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(cliente));
        when(sancionRepository.findVigentesByUsuarioId(eq(1L), eq(EstadoSancion.ACTIVA), any()))
            .thenReturn(List.of(SancionCuenta.builder().idSancion(5L).build()));

        assertThatThrownBy(() -> sancionService.aplicar(99L,
            request(1L, TipoSancion.INHABILITACION, null, "motivo")))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("sanción activa");

        verify(sancionRepository, never()).save(any());
    }

    @Test
    void revertir_sancionActiva_laMarcaRevertidaYAudita() {
        Usuario admin = usuario(99L, "admin", ERol.ADMIN);
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE);
        SancionCuenta sancion = SancionCuenta.builder()
            .idSancion(10L)
            .usuario(cliente)
            .adminAplico(admin)
            .tipo(TipoSancion.INHABILITACION)
            .motivo("motivo")
            .fechaInicio(LocalDateTime.now().minusDays(1))
            .estado(EstadoSancion.ACTIVA)
            .build();

        when(usuarioRepository.findByIdUsuario(99L)).thenReturn(Optional.of(admin));
        when(sancionRepository.findById(10L)).thenReturn(Optional.of(sancion));
        when(sancionRepository.save(any(SancionCuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        SancionCuentaDTO dto = sancionService.revertir(99L, 10L, "Revisado, fue un error");

        assertThat(sancion.getEstado()).isEqualTo(EstadoSancion.REVERTIDA);
        assertThat(sancion.getAdminRevertio()).isEqualTo(admin);
        assertThat(sancion.getObservacionReversion()).isEqualTo("Revisado, fue un error");
        assertThat(dto.getEstadoEfectivo()).isEqualTo("REVERTIDA");
        verify(auditoria).registrar(
            eq(AccionesAdmin.CUENTA_REVERTIR_SANCION), eq("SancionCuenta"), eq(10L),
            any(), any(), any(), any());
    }

    @Test
    void revertir_sancionVencida_lanzaError() {
        Usuario admin = usuario(99L, "admin", ERol.ADMIN);
        SancionCuenta sancion = SancionCuenta.builder()
            .idSancion(10L)
            .usuario(usuario(1L, "cliente", ERol.CLIENTE))
            .adminAplico(admin)
            .tipo(TipoSancion.SUSPENSION)
            .motivo("motivo")
            .fechaInicio(LocalDateTime.now().minusDays(10))
            .fechaFin(LocalDateTime.now().minusDays(1))
            .estado(EstadoSancion.ACTIVA)
            .build();

        when(usuarioRepository.findByIdUsuario(99L)).thenReturn(Optional.of(admin));
        when(sancionRepository.findById(10L)).thenReturn(Optional.of(sancion));

        assertThatThrownBy(() -> sancionService.revertir(99L, 10L, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("venció");

        verify(sancionRepository, never()).save(any());
    }

    @Test
    void estadoUsuario_conSancionVigente_devuelveSancionado() {
        Usuario cliente = usuario(1L, "cliente", ERol.CLIENTE);
        SancionCuenta sancion = SancionCuenta.builder()
            .idSancion(10L)
            .usuario(cliente)
            .tipo(TipoSancion.INHABILITACION)
            .motivo("motivo")
            .fechaInicio(LocalDateTime.now().minusHours(1))
            .estado(EstadoSancion.ACTIVA)
            .build();
        when(sancionRepository.findVigentesByUsuarioId(eq(1L), eq(EstadoSancion.ACTIVA), any()))
            .thenReturn(List.of(sancion));

        var estado = sancionService.estadoUsuario(1L);

        assertThat(estado.isSancionado()).isTrue();
        assertThat(estado.getSancionActual().getIdSancion()).isEqualTo(10L);
    }

    @Test
    void restriccionesVigentes_detectanVentasEntregasYCompras() {
        Usuario usuario = usuario(1L, "cliente", ERol.CLIENTE);
        SancionCuenta restriccion = sancion(
            10L, usuario, AlcanceSancion.ENTREGAS);
        when(sancionRepository.findVigentesByUsuarioId(
                eq(1L), eq(EstadoSancion.ACTIVA), any()))
            .thenReturn(List.of(restriccion));

        assertThat(sancionService.tieneSancionQueImpideVender(1L)).isTrue();
        assertThat(sancionService.tieneSancionDeEntrega(1L)).isTrue();
        assertThat(sancionService.tieneSancionDeCompra(1L)).isFalse();
        assertThat(sancionService.tieneAlgunaSancionVigente(1L)).isTrue();
    }

    @Test
    void vigentesDe_mapeaPorUsuario() {
        Usuario usuario = usuario(1L, "cliente", ERol.CLIENTE);
        SancionCuenta restriccion = sancion(
            10L, usuario, AlcanceSancion.ENTREGAS);
        when(sancionRepository.findVigentesByUsuarioIds(
                any(), eq(EstadoSancion.ACTIVA), any()))
            .thenReturn(List.of(restriccion));

        assertThat(sancionService.vigentesDe(List.of(1L)))
            .containsKey(1L);
    }

    @Test
    void historial_mapeaSanciones() {
        SancionCuenta restriccion = sancion(
            10L, usuario(1L, "cliente", ERol.CLIENTE),
            AlcanceSancion.ENTREGAS);
        when(sancionRepository.findByUsuarioIdUsuarioOrderByFechaInicioDesc(1L))
            .thenReturn(List.of(restriccion));

        assertThat(sancionService.historial(1L)).hasSize(1);
    }

    @Test
    void listar_devuelveLaPaginaDelRepositorio() {
        SancionCuenta restriccion = sancion(
            10L, usuario(1L, "cliente", ERol.CLIENTE),
            AlcanceSancion.ENTREGAS);
        when(sancionRepository.findAll(
                any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(
                List.of(restriccion), PageRequest.of(0, 10), 1));

        PaginaDTO<SancionCuentaDTO> pagina = sancionService.listar(
            null, null, null, null, 0, 10);

        assertThat(pagina.getContenido()).hasSize(1);
        assertThat(pagina.getTotalElementos()).isEqualTo(1);
    }

    @Test
    void escalarSistema_actualizaAlcanceMotivoYFechas() {
        SancionCuenta sancion = sancion(
            10L, usuario(1L, "cliente", ERol.CLIENTE),
            AlcanceSancion.VENTAS);
        when(sancionRepository.findById(10L)).thenReturn(Optional.of(sancion));
        when(sancionRepository.save(any(SancionCuenta.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SancionCuenta escalada = sancionService.escalarSistema(
            10L,
            AlcanceSancion.ACCESO,
            "nuevo motivo",
            "AUTO:NUEVA",
            null,
            null,
            LocalDateTime.now().minusDays(2));

        assertThat(escalada.getTipo()).isEqualTo(TipoSancion.INHABILITACION);
        assertThat(escalada.getAlcance()).isEqualTo(AlcanceSancion.ACCESO);
        assertThat(escalada.getMotivo()).isEqualTo("nuevo motivo");
        assertThat(escalada.getFechaFin()).isNull();
        assertThat(escalada.getClave()).isEqualTo("AUTO:NUEVA");
        assertThat(escalada.getFechaEscalada()).isNotNull();
    }

    @Test
    void revertirSistema_liberaLaClaveYRegistraLaReversion() {
        SancionCuenta sancion = sancion(
            10L, usuario(1L, "cliente", ERol.CLIENTE),
            AlcanceSancion.ENTREGAS);
        sancion.setClave("AUTO:ENTREGA:31:NIVEL1");
        when(sancionRepository.findById(10L)).thenReturn(Optional.of(sancion));
        when(sancionRepository.save(any(SancionCuenta.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SancionCuenta revertida = sancionService.revertirSistema(
            10L, "Se curó la mora");

        assertThat(revertida.getEstado())
            .isEqualTo(EstadoSancion.REVERTIDA);
        assertThat(revertida.getClave()).isNull();
        assertThat(revertida.getFechaReversion()).isNotNull();
        assertThat(revertida.getObservacionReversion())
            .isEqualTo("Se curó la mora");
    }

    private SancionCuenta sancion(
            Long id, Usuario usuario, AlcanceSancion alcance) {
        return SancionCuenta.builder()
            .idSancion(id)
            .usuario(usuario)
            .tipo(TipoSancion.SUSPENSION)
            .alcance(alcance)
            .estado(EstadoSancion.ACTIVA)
            .motivo("motivo")
            .fechaInicio(LocalDateTime.now().minusHours(1))
            .fechaFin(LocalDateTime.now().plusDays(5))
            .build();
    }

    private SancionRequest request(
            Long idUsuario, TipoSancion tipo, LocalDateTime fechaFin, String motivo) {
        SancionRequest request = new SancionRequest();
        request.setIdUsuario(idUsuario);
        request.setTipo(tipo);
        request.setFechaFin(fechaFin);
        request.setMotivo(motivo);
        return request;
    }

    private Usuario usuario(Long id, String nombre, ERol rol) {
        return Usuario.builder()
            .idUsuario(id)
            .nombreUsuario(nombre)
            .correo(nombre + "@mail.com")
            .estadoCuenta(true)
            .rol(Rol.builder().idRol(3L).nombre(rol).build())
            .build();
    }
}
