package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.temipora.backend_springboot.modelo.AccionReporteResenia;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoReporteResenia;
import com.temipora.backend_springboot.modelo.EstadoResenia;
import com.temipora.backend_springboot.modelo.MotivoReporteResenia;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.ReporteResenia;
import com.temipora.backend_springboot.modelo.ReporteReseniaDTO;
import com.temipora.backend_springboot.modelo.ReporteReseniaHistorialDTO;
import com.temipora.backend_springboot.modelo.ReporteReseniaRequest;
import com.temipora.backend_springboot.modelo.Resenia;
import com.temipora.backend_springboot.modelo.ReseniaReportadaDTO;
import com.temipora.backend_springboot.modelo.ReseniaVersion;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ReporteReseniaRepository;
import com.temipora.backend_springboot.repositorios.ReseniaOpcionRepository;
import com.temipora.backend_springboot.repositorios.ReseniaRepository;
import com.temipora.backend_springboot.repositorios.ReseniaValorRepository;
import com.temipora.backend_springboot.repositorios.ReseniaVersionRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ReseniaModeracionServiceTest {

    @Mock
    private ReseniaRepository reseniaRepository;

    @Mock
    private ReporteReseniaRepository reporteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ReseniaVersionRepository versionRepository;

    @Mock
    private ReseniaValorRepository valorRepository;

    @Mock
    private ReseniaOpcionRepository opcionRepository;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ReseniaModeracionService moderacionService;

    // -----------------------------------------------------------------
    // reportar
    // -----------------------------------------------------------------

    @Test
    void reportar_reseniaPublicaDeOtro_creaReportePendienteYNotificaAdmins() {
        Resenia resenia = resenia(EstadoResenia.PUBLICADA);
        Usuario reportante = usuario(30L, "reportante30");
        when(reseniaRepository.findById(7L)).thenReturn(Optional.of(resenia));
        when(reporteRepository
            .existsByReseniaIdReseniaAndReportanteIdUsuario(7L, 30L))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(30L))
            .thenReturn(Optional.of(reportante));
        when(reporteRepository.countByReseniaIdReseniaAndEstado(
                7L, EstadoReporteResenia.PENDIENTE))
            .thenReturn(0L);
        when(reporteRepository.save(any(ReporteResenia.class)))
            .thenAnswer(inv -> {
                ReporteResenia reporte = inv.getArgument(0);
                reporte.setIdReporte(1L);
                return reporte;
            });
        when(usuarioRepository.findByRol_Nombre(ERol.ADMIN))
            .thenReturn(List.of(usuario(1L, "admin")));

        ReporteReseniaDTO dto = moderacionService.reportar(
            7L, 30L, request(MotivoReporteResenia.SPAM, "  publicidad  "));

        assertThat(dto.getIdReporte()).isEqualTo(1L);
        assertThat(dto.getEstado()).isEqualTo(EstadoReporteResenia.PENDIENTE);
        assertThat(dto.getMotivo()).isEqualTo(MotivoReporteResenia.SPAM);
        assertThat(dto.getEtiquetaMotivo()).isEqualTo("Spam o publicidad");
        assertThat(dto.getComentario()).isEqualTo("publicidad");
        verify(notificacionService).crearParaUsuario(
            eq(1L), eq("RESENIA_REPORTADA"), anyString(), anyString(),
            eq(7L), eq("RESENIA"));
    }

    @Test
    void reportar_segundoReportePendiente_noVuelveANotificar() {
        Resenia resenia = resenia(EstadoResenia.PUBLICADA);
        when(reseniaRepository.findById(7L)).thenReturn(Optional.of(resenia));
        when(reporteRepository
            .existsByReseniaIdReseniaAndReportanteIdUsuario(7L, 30L))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(30L))
            .thenReturn(Optional.of(usuario(30L, "reportante30")));
        when(reporteRepository.countByReseniaIdReseniaAndEstado(
                7L, EstadoReporteResenia.PENDIENTE))
            .thenReturn(2L);
        when(reporteRepository.save(any(ReporteResenia.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        moderacionService.reportar(
            7L, 30L, request(MotivoReporteResenia.OTRO, null));

        verify(notificacionService, never()).crearParaUsuario(
            any(), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void reportar_reseniaPropia_lanzaError() {
        when(reseniaRepository.findById(7L))
            .thenReturn(Optional.of(resenia(EstadoResenia.PUBLICADA)));

        assertThatThrownBy(() -> moderacionService.reportar(
                7L, 10L, request(MotivoReporteResenia.OFENSIVA, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("propia");
    }

    @Test
    void reportar_reseniaNoPublicada_lanzaError() {
        when(reseniaRepository.findById(7L))
            .thenReturn(Optional.of(resenia(EstadoResenia.OCULTA)));

        assertThatThrownBy(() -> moderacionService.reportar(
                7L, 30L, request(MotivoReporteResenia.OFENSIVA, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("publicadas");
    }

    @Test
    void reportar_reseniaNoPublica_lanzaError() {
        Resenia resenia = resenia(EstadoResenia.PUBLICADA);
        resenia.setAutor(usuario(20L, "artista20"));
        when(reseniaRepository.findById(7L)).thenReturn(Optional.of(resenia));

        assertThatThrownBy(() -> moderacionService.reportar(
                7L, 30L, request(MotivoReporteResenia.OFENSIVA, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("públicas");
    }

    @Test
    void reportar_duplicado_lanzaConflicto() {
        when(reseniaRepository.findById(7L))
            .thenReturn(Optional.of(resenia(EstadoResenia.PUBLICADA)));
        when(reporteRepository
            .existsByReseniaIdReseniaAndReportanteIdUsuario(7L, 30L))
            .thenReturn(true);

        assertThatThrownBy(() -> moderacionService.reportar(
                7L, 30L, request(MotivoReporteResenia.SPAM, null)))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("Ya reportaste");
    }

    @Test
    void reportar_usuarioInexistente_lanzaNoSuchElement() {
        when(reseniaRepository.findById(7L))
            .thenReturn(Optional.of(resenia(EstadoResenia.PUBLICADA)));
        when(reporteRepository
            .existsByReseniaIdReseniaAndReportanteIdUsuario(7L, 99L))
            .thenReturn(false);
        when(usuarioRepository.findByIdUsuario(99L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> moderacionService.reportar(
                7L, 99L, request(MotivoReporteResenia.SPAM, null)))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void reportesDelUsuario_devuelveLosIdsReportados() {
        when(reporteRepository.findIdsReseniasReportadasPorUsuario(30L))
            .thenReturn(List.of(7L, 8L));

        assertThat(moderacionService.reportesDelUsuario(30L))
            .containsExactly(7L, 8L);
    }

    // -----------------------------------------------------------------
    // ocultar
    // -----------------------------------------------------------------

    @Test
    void ocultar_cambiaEstadoResuelveReportesYNotificaAlAutor() {
        Resenia resenia = resenia(EstadoResenia.PUBLICADA);
        Usuario admin = usuario(1L, "admin");
        ReporteResenia reporte = reporte(
            resenia, EstadoReporteResenia.PENDIENTE, usuario(30L, "reportante30"));
        when(reseniaRepository.findForUpdateById(7L))
            .thenReturn(Optional.of(resenia));
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(admin));
        when(reporteRepository
            .findByReseniaIdReseniaAndEstadoOrderByFechaCreacionAsc(
                7L, EstadoReporteResenia.PENDIENTE))
            .thenReturn(List.of(reporte));
        stubContenido(resenia, List.of(reporte));

        ReseniaReportadaDTO dto = moderacionService.ocultar(
            7L, 1L, "  Contenido ofensivo  ");

        assertThat(resenia.getEstado()).isEqualTo(EstadoResenia.OCULTA);
        assertThat(resenia.getModeradaEn()).isNotNull();
        assertThat(resenia.getMotivoModeracion()).isEqualTo("Contenido ofensivo");
        assertThat(reporte.getEstado()).isEqualTo(EstadoReporteResenia.RESUELTO);
        assertThat(reporte.getAccion()).isEqualTo(AccionReporteResenia.OCULTAR);
        assertThat(reporte.getResueltoPor()).isEqualTo(admin);
        assertThat(reporte.getFechaResolucion()).isNotNull();
        assertThat(dto.getEstado()).isEqualTo(EstadoResenia.OCULTA);
        assertThat(dto.getReportesPendientes()).isZero();
        assertThat(dto.getReportesTotales()).isEqualTo(1);
        assertThat(dto.getReportes().get(0).getResueltoPor().getIdUsuario())
            .isEqualTo(1L);
        verify(notificacionService).crearParaUsuario(
            eq(10L), eq("RESENIA_OCULTA"), anyString(), anyString(),
            eq(100L), eq("PEDIDO"));
    }

    @Test
    void ocultar_sinMotivo_guardaMotivoPorDefecto() {
        Resenia resenia = resenia(EstadoResenia.PUBLICADA);
        when(reseniaRepository.findForUpdateById(7L))
            .thenReturn(Optional.of(resenia));
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(usuario(1L, "admin")));
        when(reporteRepository
            .findByReseniaIdReseniaAndEstadoOrderByFechaCreacionAsc(
                7L, EstadoReporteResenia.PENDIENTE))
            .thenReturn(List.of());
        stubContenido(resenia, List.of());

        moderacionService.ocultar(7L, 1L, "   ");

        assertThat(resenia.getMotivoModeracion()).isEqualTo("No especificado");
    }

    @Test
    void ocultar_reseniaEliminada_lanzaError() {
        when(reseniaRepository.findForUpdateById(7L))
            .thenReturn(Optional.of(resenia(EstadoResenia.ELIMINADA)));

        assertThatThrownBy(() -> moderacionService.ocultar(7L, 1L, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("eliminada");
    }

    // -----------------------------------------------------------------
    // descartar reportes
    // -----------------------------------------------------------------

    @Test
    void descartarReportes_marcaPendientesComoDescartados() {
        Resenia resenia = resenia(EstadoResenia.PUBLICADA);
        Usuario admin = usuario(1L, "admin");
        ReporteResenia reporte = reporte(
            resenia, EstadoReporteResenia.PENDIENTE, usuario(30L, "reportante30"));
        when(reseniaRepository.findForUpdateById(7L))
            .thenReturn(Optional.of(resenia));
        when(reporteRepository
            .findByReseniaIdReseniaAndEstadoOrderByFechaCreacionAsc(
                7L, EstadoReporteResenia.PENDIENTE))
            .thenReturn(List.of(reporte));
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(admin));
        stubContenido(resenia, List.of(reporte));

        ReseniaReportadaDTO dto = moderacionService.descartarReportes(7L, 1L);

        assertThat(resenia.getEstado()).isEqualTo(EstadoResenia.PUBLICADA);
        assertThat(reporte.getEstado()).isEqualTo(EstadoReporteResenia.DESCARTADO);
        assertThat(reporte.getAccion()).isEqualTo(AccionReporteResenia.DESCARTAR);
        assertThat(reporte.getResueltoPor()).isEqualTo(admin);
        assertThat(dto.getReportesPendientes()).isZero();
        assertThat(dto.getReportes().get(0).getResueltoPor().getIdUsuario())
            .isEqualTo(1L);
        assertThat(dto.getReportes().get(0).getAccion())
            .isEqualTo(AccionReporteResenia.DESCARTAR);
        verify(notificacionService, never()).crearParaUsuario(
            any(), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void descartarReportes_sinPendientes_lanzaError() {
        when(reseniaRepository.findForUpdateById(7L))
            .thenReturn(Optional.of(resenia(EstadoResenia.PUBLICADA)));
        when(reporteRepository
            .findByReseniaIdReseniaAndEstadoOrderByFechaCreacionAsc(
                7L, EstadoReporteResenia.PENDIENTE))
            .thenReturn(List.of());

        assertThatThrownBy(() -> moderacionService.descartarReportes(7L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No hay reportes");
    }

    // -----------------------------------------------------------------
    // restaurar
    // -----------------------------------------------------------------

    @Test
    void restaurar_vuelveAPublicadaYLimpiaLaModeracion() {
        Resenia resenia = resenia(EstadoResenia.OCULTA);
        resenia.setModeradaEn(LocalDateTime.now().minusDays(1));
        resenia.setMotivoModeracion("Contenido ofensivo");
        when(reseniaRepository.findForUpdateById(7L))
            .thenReturn(Optional.of(resenia));
        when(usuarioRepository.findByIdUsuario(1L))
            .thenReturn(Optional.of(usuario(1L, "admin")));
        stubContenido(resenia, List.of());

        ReseniaReportadaDTO dto = moderacionService.restaurar(7L, 1L);

        assertThat(resenia.getEstado()).isEqualTo(EstadoResenia.PUBLICADA);
        assertThat(resenia.getModeradaEn()).isNull();
        assertThat(resenia.getMotivoModeracion()).isNull();
        assertThat(dto.getEstado()).isEqualTo(EstadoResenia.PUBLICADA);
        verify(notificacionService).crearParaUsuario(
            eq(10L), eq("RESENIA_RESTAURADA"), anyString(), anyString(),
            eq(100L), eq("PEDIDO"));
    }

    @Test
    void restaurar_reseniaPublicada_lanzaError() {
        when(reseniaRepository.findForUpdateById(7L))
            .thenReturn(Optional.of(resenia(EstadoResenia.PUBLICADA)));

        assertThatThrownBy(() -> moderacionService.restaurar(7L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no está oculta");
    }

    // -----------------------------------------------------------------
    // listados
    // -----------------------------------------------------------------

    @Test
    void listarReportadas_armaPaginaConReportesAgrupados() {
        Resenia resenia = resenia(EstadoResenia.PUBLICADA);
        ReporteResenia pendiente = reporte(
            resenia, EstadoReporteResenia.PENDIENTE, usuario(30L, "reportante30"));
        ReporteResenia resuelto = reporte(
            resenia, EstadoReporteResenia.DESCARTADO, usuario(31L, "reportante31"));
        PageRequest pageable = PageRequest.of(0, 10);
        when(reseniaRepository.findConReportesPendientes(
                EstadoReporteResenia.PENDIENTE, pageable))
            .thenReturn(new PageImpl<>(List.of(resenia), pageable, 1));
        stubContenido(resenia, List.of(pendiente, resuelto));

        PaginaDTO<ReseniaReportadaDTO> pagina =
            moderacionService.listarReportadas(0, 10);

        assertThat(pagina.getTotalElementos()).isEqualTo(1);
        assertThat(pagina.getContenido()).hasSize(1);
        ReseniaReportadaDTO dto = pagina.getContenido().get(0);
        assertThat(dto.getReportesTotales()).isEqualTo(2);
        assertThat(dto.getReportesPendientes()).isEqualTo(1);
        assertThat(dto.getReportes()).hasSize(2);
        assertThat(dto.getAutor().getIdUsuario()).isEqualTo(10L);
    }

    @Test
    void listarOcultas_consultaElEstadoOculto() {
        Resenia resenia = resenia(EstadoResenia.OCULTA);
        resenia.setModeradaEn(LocalDateTime.now());
        PageRequest pageable = PageRequest.of(0, 10);
        when(reseniaRepository.findPorEstado(EstadoResenia.OCULTA, pageable))
            .thenReturn(new PageImpl<>(List.of(resenia), pageable, 1));
        stubContenido(resenia, List.of());

        PaginaDTO<ReseniaReportadaDTO> pagina =
            moderacionService.listarOcultas(0, 10);

        assertThat(pagina.getContenido()).hasSize(1);
        assertThat(pagina.getContenido().get(0).getEstado())
            .isEqualTo(EstadoResenia.OCULTA);
    }

    @Test
    void listarHistorial_devuelveElAdminQueResolvioElReporte() {
        Resenia resenia = resenia(EstadoResenia.PUBLICADA);
        Usuario admin = usuario(1L, "admin");
        ReporteResenia reporte = reporte(
            resenia, EstadoReporteResenia.DESCARTADO, usuario(30L, "reportante30"));
        reporte.setAccion(AccionReporteResenia.DESCARTAR);
        reporte.setResueltoPor(admin);
        reporte.setFechaResolucion(LocalDateTime.now());
        PageRequest pageable = PageRequest.of(0, 10);
        when(reporteRepository.findHistorial(
                EstadoReporteResenia.DESCARTADO, pageable))
            .thenReturn(new PageImpl<>(List.of(reporte), pageable, 1));

        PaginaDTO<ReporteReseniaHistorialDTO> pagina =
            moderacionService.listarHistorial(
                EstadoReporteResenia.DESCARTADO, 0, 10);

        assertThat(pagina.getTotalElementos()).isEqualTo(1);
        ReporteReseniaHistorialDTO dto = pagina.getContenido().get(0);
        assertThat(dto.getEstado()).isEqualTo(EstadoReporteResenia.DESCARTADO);
        assertThat(dto.getAccion()).isEqualTo(AccionReporteResenia.DESCARTAR);
        assertThat(dto.getResueltoPor().getNombreUsuario()).isEqualTo("admin");
        assertThat(dto.getReportante().getNombreUsuario())
            .isEqualTo("reportante30");
        assertThat(dto.getNombreServicio()).isEqualTo("Retrato digital");
        assertThat(dto.getAutorResenia()).isEqualTo("cliente10");
        assertThat(dto.getEtiquetaMotivo()).isEqualTo("Spam o publicidad");
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private void stubContenido(
            Resenia resenia, List<ReporteResenia> reportes) {
        ReseniaVersion version = version(resenia);
        when(versionRepository.findActualesPorReseniaIds(
                List.of(resenia.getIdResenia())))
            .thenReturn(List.of(version));
        when(valorRepository.findByVersionIdVersionIn(List.of(50L)))
            .thenReturn(List.of());
        when(opcionRepository.findByVersionIdVersionIn(List.of(50L)))
            .thenReturn(List.of());
        when(reporteRepository.findByReseniaIdReseniaInOrderByFechaCreacionAsc(
                List.of(resenia.getIdResenia())))
            .thenReturn(reportes);
    }

    private ReporteReseniaRequest request(
            MotivoReporteResenia motivo, String comentario) {
        ReporteReseniaRequest request = new ReporteReseniaRequest();
        request.setMotivo(motivo);
        request.setComentario(comentario);
        return request;
    }

    private ReporteResenia reporte(
            Resenia resenia,
            EstadoReporteResenia estado,
            Usuario reportante) {
        ReporteResenia reporte = new ReporteResenia();
        reporte.setIdReporte(1L);
        reporte.setResenia(resenia);
        reporte.setReportante(reportante);
        reporte.setMotivo(MotivoReporteResenia.SPAM);
        reporte.setComentario("Comentario");
        reporte.setEstado(estado);
        reporte.setFechaCreacion(LocalDateTime.now().minusHours(1));
        return reporte;
    }

    private Resenia resenia(EstadoResenia estado) {
        Resenia resenia = new Resenia();
        resenia.setIdResenia(7L);
        resenia.setPedido(pedido());
        resenia.setAutor(usuario(10L, "cliente10"));
        resenia.setEstado(estado);
        resenia.setFechaCreacion(LocalDateTime.now().minusDays(1));
        return resenia;
    }

    private ReseniaVersion version(Resenia resenia) {
        ReseniaVersion version = new ReseniaVersion();
        version.setIdVersion(50L);
        version.setResenia(resenia);
        version.setNumero(1);
        version.setDescripcion("La entrega llegó en fecha.");
        version.setCalificacionGeneral(5);
        version.setFechaVersion(LocalDateTime.now().minusDays(1));
        return version;
    }

    private Pedido pedido() {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(100L);
        pedido.setEstado(EstadoPedido.FINALIZADO);
        pedido.setCliente(usuario(10L, "cliente10"));
        pedido.setArtista(usuario(20L, "artista20"));
        Servicio servicio = new Servicio();
        servicio.setNombre("Retrato digital");
        pedido.setServicio(servicio);
        return pedido;
    }

    private Usuario usuario(Long id, String nombreUsuario) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombreUsuario(nombreUsuario);
        return usuario;
    }
}
