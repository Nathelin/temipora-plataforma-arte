package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;

@ExtendWith(MockitoExtension.class)
class PedidoCierreServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private PedidoCierreService pedidoCierreService;

    @Test
    void finalizar_marcaPedidoYSolicitudYNotificaAAmbasPartes() {
        Pedido pedido = pedido();

        pedidoCierreService.finalizar(pedido, LocalDateTime.now());

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.FINALIZADO);
        assertThat(pedido.getFinalizadoEn()).isNotNull();
        assertThat(pedido.getSolicitud().getEstado())
            .isEqualTo(EstadoSolicitud.FINALIZADA);
        verify(pedidoRepository).save(pedido);
        verify(solicitudRepository).save(pedido.getSolicitud());
        verify(notificacionService, times(2)).crearParaUsuario(
            anyLong(),
            eq("PEDIDO_FINALIZADO"),
            eq("Pedido finalizado"),
            anyString(),
            eq(1L),
            eq("PEDIDO"));
    }

    @Test
    void finalizarPedidoYaFinalizado_noHaceNada() {
        Pedido pedido = pedido();
        pedido.setEstado(EstadoPedido.FINALIZADO);

        pedidoCierreService.finalizar(pedido, LocalDateTime.now());

        verifyNoInteractions(pedidoRepository, solicitudRepository,
            notificacionService);
    }

    private Pedido pedido() {
        Usuario cliente = new Usuario();
        cliente.setIdUsuario(1L);
        Usuario artista = new Usuario();
        artista.setIdUsuario(2L);
        Servicio servicio = new Servicio();
        servicio.setNombre("Ilustración");

        Solicitud solicitud = new Solicitud();
        solicitud.setEstado(EstadoSolicitud.PAGADA);

        Pedido pedido = new Pedido();
        pedido.setIdPedido(1L);
        pedido.setCliente(cliente);
        pedido.setArtista(artista);
        pedido.setServicio(servicio);
        pedido.setSolicitud(solicitud);
        pedido.setEstado(EstadoPedido.EN_CURSO);
        return pedido;
    }
}
