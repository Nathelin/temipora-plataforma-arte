package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.ListaEspera;
import com.temipora.backend_springboot.modelo.ListaEsperaDTO;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ListaEsperaRepository;

@ExtendWith(MockitoExtension.class)
class ListaEsperaServiceTest {

    @Mock
    private ListaEsperaRepository listaEsperaRepository;

    @InjectMocks
    private ListaEsperaService listaEsperaService;

    @Test
    void crear_conListaExistente_asignaLaSiguientePosicion() {
        Servicio servicio = servicio(7L);
        Solicitud solicitud = solicitud(11L);
        Usuario cliente = cliente(1L, "cliente");
        when(listaEsperaRepository.maxPosicionActiva(7L)).thenReturn(3);
        when(listaEsperaRepository.save(any(ListaEspera.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ListaEspera lista = listaEsperaService.crear(
            solicitud, servicio, cliente, 2);

        assertThat(lista.getServicio()).isSameAs(servicio);
        assertThat(lista.getSolicitud()).isSameAs(solicitud);
        assertThat(lista.getCliente()).isSameAs(cliente);
        assertThat(lista.getCantidadDibujos()).isEqualTo(2);
        assertThat(lista.getPosicion()).isEqualTo(4);
        assertThat(lista.getEstado()).isEqualTo("ESPERANDO");
        assertThat(lista.getFechaActualizacion()).isNotNull();
        assertThat(solicitud.getListaEspera()).isSameAs(lista);
        verify(listaEsperaRepository).save(lista);
    }

    @Test
    void crear_sinListaExistente_usaLaPrimerPosicion() {
        when(listaEsperaRepository.maxPosicionActiva(7L)).thenReturn(null);
        when(listaEsperaRepository.save(any(ListaEspera.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ListaEspera lista = listaEsperaService.crear(
            solicitud(11L), servicio(7L), cliente(1L, "cliente"), 1);

        assertThat(lista.getPosicion()).isEqualTo(1);
    }

    @Test
    void listarPorServicio_mapeaLasEntradasEsperando() {
        ListaEspera lista = listaEspera(5L, 11L, 7L, cliente(1L, "cliente"), 2, "ESPERANDO");
        when(listaEsperaRepository
                .findByServicioIdServicioAndEstadoOrderByPosicionAsc(7L, "ESPERANDO"))
            .thenReturn(List.of(lista));

        List<ListaEsperaDTO> resultado = listaEsperaService.listarPorServicio(7L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getIdListaEspera()).isEqualTo(5L);
        assertThat(resultado.get(0).getIdSolicitud()).isEqualTo(11L);
        assertThat(resultado.get(0).getIdServicio()).isEqualTo(7L);
        assertThat(resultado.get(0).getIdCliente()).isEqualTo(1L);
        assertThat(resultado.get(0).getNombreCliente()).isEqualTo("cliente");
        assertThat(resultado.get(0).getCantidadDibujos()).isEqualTo(2);
        assertThat(resultado.get(0).getPosicion()).isEqualTo(2);
        assertThat(resultado.get(0).getEstado()).isEqualTo("ESPERANDO");
    }

    @Test
    void marcarRechazada_actualizaElEstado() {
        ListaEspera lista = listaEspera(5L, 11L, 7L, cliente(1L, "cliente"), 1, "ESPERANDO");
        when(listaEsperaRepository.findBySolicitudIdSolicitud(11L))
            .thenReturn(Optional.of(lista));

        listaEsperaService.marcarRechazada(solicitud(11L));

        assertThat(lista.getEstado()).isEqualTo("RECHAZADA");
        assertThat(lista.getFechaActualizacion()).isNotNull();
    }

    @Test
    void marcarCancelada_actualizaElEstado() {
        ListaEspera lista = listaEspera(5L, 11L, 7L, cliente(1L, "cliente"), 1, "ESPERANDO");
        when(listaEsperaRepository.findBySolicitudIdSolicitud(11L))
            .thenReturn(Optional.of(lista));

        listaEsperaService.marcarCancelada(solicitud(11L));

        assertThat(lista.getEstado()).isEqualTo("CANCELADA");
    }

    @Test
    void marcarConvertida_actualizaElEstado() {
        ListaEspera lista = listaEspera(5L, 11L, 7L, cliente(1L, "cliente"), 1, "ESPERANDO");
        when(listaEsperaRepository.findBySolicitudIdSolicitud(11L))
            .thenReturn(Optional.of(lista));

        listaEsperaService.marcarConvertida(solicitud(11L));

        assertThat(lista.getEstado()).isEqualTo("CONVERTIDA");
    }

    @Test
    void marcarEstados_sinLista_noHaceNada() {
        when(listaEsperaRepository.findBySolicitudIdSolicitud(11L))
            .thenReturn(Optional.empty());

        listaEsperaService.marcarRechazada(solicitud(11L));

        assertThat(listaEsperaRepository.findBySolicitudIdSolicitud(11L)).isEmpty();
    }

    private Servicio servicio(Long id) {
        Servicio servicio = new Servicio();
        servicio.setIdServicio(id);
        return servicio;
    }

    private Solicitud solicitud(Long id) {
        Solicitud solicitud = new Solicitud();
        solicitud.setIdSolicitud(id);
        return solicitud;
    }

    private Usuario cliente(Long id, String nombreUsuario) {
        Usuario cliente = new Usuario();
        cliente.setIdUsuario(id);
        cliente.setNombreUsuario(nombreUsuario);
        return cliente;
    }

    private ListaEspera listaEspera(
            Long id, Long idSolicitud, Long idServicio, Usuario cliente,
            int cantidadDibujos, String estado) {
        ListaEspera lista = new ListaEspera();
        lista.setIdListaEspera(id);
        lista.setSolicitud(solicitud(idSolicitud));
        lista.setServicio(servicio(idServicio));
        lista.setCliente(cliente);
        lista.setCantidadDibujos(cantidadDibujos);
        lista.setPosicion(2);
        lista.setEstado(estado);
        return lista;
    }
}
