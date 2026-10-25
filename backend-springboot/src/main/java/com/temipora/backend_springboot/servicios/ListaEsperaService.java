package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ListaEspera;
import com.temipora.backend_springboot.modelo.ListaEsperaDTO;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.Solicitud;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.ListaEsperaRepository;

@Service
public class ListaEsperaService {

    private final ListaEsperaRepository listaEsperaRepository;

    public ListaEsperaService(ListaEsperaRepository listaEsperaRepository) {
        this.listaEsperaRepository = listaEsperaRepository;
    }

    @Transactional
    public ListaEspera crear(Solicitud solicitud, Servicio servicio, Usuario cliente, int cantidadDibujos) {
        Integer max = listaEsperaRepository.maxPosicionActiva(servicio.getIdServicio());
        int posicion = (max == null ? 0 : max) + 1;

        ListaEspera lista = new ListaEspera();
        lista.setServicio(servicio);
        lista.setSolicitud(solicitud);
        lista.setCliente(cliente);
        lista.setCantidadDibujos(cantidadDibujos);
        lista.setPosicion(posicion);
        lista.setEstado("ESPERANDO");
        lista.setFechaActualizacion(LocalDateTime.now());

        ListaEspera guardada = listaEsperaRepository.save(lista);
        solicitud.setListaEspera(guardada);
        return guardada;
    }

    @Transactional(readOnly = true)
    public List<ListaEsperaDTO> listarPorServicio(Long idServicio) {
        return listaEsperaRepository
            .findByServicioIdServicioAndEstadoOrderByPosicionAsc(idServicio, "ESPERANDO")
            .stream()
            .map(ListaEsperaDTO::desdeListaEspera)
            .toList();
    }

    @Transactional
    public void marcarRechazada(Solicitud solicitud) {
        listaEsperaRepository.findBySolicitudIdSolicitud(solicitud.getIdSolicitud())
            .ifPresent(lista -> {
                lista.setEstado("RECHAZADA");
                lista.setFechaActualizacion(LocalDateTime.now());
            });
    }

    @Transactional
    public void marcarConvertida(Solicitud solicitud) {
        listaEsperaRepository.findBySolicitudIdSolicitud(solicitud.getIdSolicitud())
            .ifPresent(lista -> {
                lista.setEstado("CONVERTIDA");
                lista.setFechaActualizacion(LocalDateTime.now());
            });
    }
}
