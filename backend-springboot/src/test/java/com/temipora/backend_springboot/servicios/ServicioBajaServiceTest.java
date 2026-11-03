package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.ListaEspera;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.ServicioResumenEstadoDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ListaEsperaRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ServicioBajaServiceTest {

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private ListaEsperaRepository listaEsperaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ServicioService servicioService;

    @Mock
    private SolicitudServicioService solicitudServicioService;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ServicioBajaService servicioBajaService;

    @Test
    void archivar_conPresupuestoVigente_lanzaConflictoYNoCierraSolicitudes() {
        when(servicioRepository.findByIdServicioForUpdate(1L))
            .thenReturn(Optional.of(servicio()));
        when(solicitudRepository.existsByServicioIdServicioAndEstado(
            1L, EstadoSolicitud.PRESUPUESTADA)).thenReturn(true);
        when(solicitudRepository.countByServicioIdServicioAndEstadoIn(
            eq(1L), eq(List.of(EstadoSolicitud.PRESUPUESTADA)))).thenReturn(1L);

        assertThatThrownBy(() -> servicioBajaService.archivar(1L, 5L))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("presupuesto");

        verify(solicitudServicioService, never()).cerrarPorBajaServicio(any());
        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void archivar_conPedidoPagadoEnCurso_lanzaConflicto() {
        when(servicioRepository.findByIdServicioForUpdate(1L))
            .thenReturn(Optional.of(servicio()));
        when(solicitudRepository.existsByServicioIdServicioAndEstado(
            1L, EstadoSolicitud.PRESUPUESTADA)).thenReturn(false);
        when(solicitudRepository.existsByServicioIdServicioAndEstado(
            1L, EstadoSolicitud.PAGADA)).thenReturn(true);
        when(solicitudRepository.countByServicioIdServicioAndEstadoIn(
            eq(1L), eq(List.of(EstadoSolicitud.PRESUPUESTADA)))).thenReturn(0L);
        when(solicitudRepository.countByServicioIdServicioAndEstadoIn(
            eq(1L), eq(List.of(EstadoSolicitud.PAGADA)))).thenReturn(2L);

        assertThatThrownBy(() -> servicioBajaService.archivar(1L, 5L))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("pedido");

        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void archivar_sinCompromisos_cierraSolicitudesYArchiva() {
        Servicio servicio = servicio();
        when(servicioRepository.findByIdServicioForUpdate(1L)).thenReturn(Optional.of(servicio));
        when(solicitudRepository.existsByServicioIdServicioAndEstado(any(), any()))
            .thenReturn(false);
        when(solicitudServicioService.cerrarPorBajaServicio(1L)).thenReturn(2);
        when(usuarioRepository.findByIdUsuario(5L))
            .thenReturn(Optional.of(Usuario.builder().idUsuario(5L).build()));
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));

        ServicioDTO dto = servicioBajaService.archivar(1L, 5L);

        assertThat(dto.getEstado()).isEqualTo(EstadoServicio.ARCHIVADO);
        verify(solicitudServicioService).cerrarPorBajaServicio(1L);
    }

    @Test
    void archivar_yaArchivado_lanzaConflicto() {
        Servicio servicio = servicio();
        servicio.setEstado(EstadoServicio.ARCHIVADO);
        when(servicioRepository.findByIdServicioForUpdate(1L)).thenReturn(Optional.of(servicio));

        assertThatThrownBy(() -> servicioBajaService.archivar(1L, 5L))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("archivado");
    }

    @Test
    void resumenEstado_cuentaSolicitudesYListaDeEspera() {
        when(solicitudRepository.contarPorServicioAgrupadoPorEstado(1L)).thenReturn(List.of(
            new Object[] { EstadoSolicitud.PENDIENTE, 4L },
            new Object[] { EstadoSolicitud.EN_LISTA_ESPERA, 1L },
            new Object[] { EstadoSolicitud.PRESUPUESTADA, 2L },
            new Object[] { EstadoSolicitud.RECHAZADA, 7L }));
        when(listaEsperaRepository.findByServicioIdServicioAndEstadoOrderByPosicionAsc(
            1L, "ESPERANDO")).thenReturn(List.of(
                new ListaEspera(), new ListaEspera(), new ListaEspera()));

        ServicioResumenEstadoDTO resumen = servicioBajaService.resumenEstado(1L, 5L);

        assertThat(resumen.getPendientes()).isEqualTo(4);
        assertThat(resumen.getEnEspera()).isEqualTo(1);
        assertThat(resumen.getPresupuestadas()).isEqualTo(2);
        assertThat(resumen.getPagadas()).isZero();
        assertThat(resumen.getListaEspera()).isEqualTo(3);
    }

    private Servicio servicio() {
        Servicio servicio = new Servicio();
        servicio.setIdServicio(1L);
        servicio.setArtista(Usuario.builder().idUsuario(5L).nombreUsuario("artista").build());
        servicio.setImagenes(new ArrayList<>());
        servicio.setPrecioBase(new BigDecimal("100.00"));
        return servicio;
    }
}
