package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.EstadoSolicitud;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.modelo.ServicioResumenEstadoDTO;
import com.temipora.backend_springboot.repositorios.ListaEsperaRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;
import com.temipora.backend_springboot.repositorios.SolicitudRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class ServicioBajaService {

    private final ServicioRepository servicioRepository;
    private final SolicitudRepository solicitudRepository;
    private final ListaEsperaRepository listaEsperaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioService servicioService;
    private final SolicitudServicioService solicitudServicioService;

    public ServicioBajaService(
            ServicioRepository servicioRepository,
            SolicitudRepository solicitudRepository,
            ListaEsperaRepository listaEsperaRepository,
            UsuarioRepository usuarioRepository,
            ServicioService servicioService,
            SolicitudServicioService solicitudServicioService) {
        this.servicioRepository = servicioRepository;
        this.solicitudRepository = solicitudRepository;
        this.listaEsperaRepository = listaEsperaRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicioService = servicioService;
        this.solicitudServicioService = solicitudServicioService;
    }

    @Transactional(readOnly = true)
    public ServicioResumenEstadoDTO resumenEstado(Long idServicio, Long idArtista) {
        servicioService.verificarPropietario(idServicio, idArtista);

        long pendientes = 0;
        long enEspera = 0;
        long presupuestadas = 0;
        long pagadas = 0;
        for (Object[] fila : solicitudRepository.contarPorServicioAgrupadoPorEstado(idServicio)) {
            EstadoSolicitud estado = (EstadoSolicitud) fila[0];
            long total = ((Number) fila[1]).longValue();
            switch (estado) {
                case PENDIENTE -> pendientes = total;
                case EN_LISTA_ESPERA -> enEspera = total;
                case PRESUPUESTADA -> presupuestadas = total;
                case PAGADA -> pagadas = total;
                default -> {
                    // Estados cerrados: no forman parte del impacto de la baja.
                }
            }
        }

        long listaEspera = listaEsperaRepository
            .findByServicioIdServicioAndEstadoOrderByPosicionAsc(idServicio, "ESPERANDO")
            .size();

        return ServicioResumenEstadoDTO.builder()
            .idServicio(idServicio)
            .pendientes(pendientes)
            .enEspera(enEspera)
            .presupuestadas(presupuestadas)
            .pagadas(pagadas)
            .listaEspera(listaEspera)
            .build();
    }

    @Transactional
    public ServicioDTO archivar(Long idServicio, Long idArtista) {
        Servicio servicio = servicioRepository.findByIdServicioForUpdate(idServicio)
            .orElseThrow(() -> new NoSuchElementException(
                "Servicio no encontrado: " + idServicio));
        servicioService.verificarDuenio(servicio, idArtista);

        if (servicio.getEstado() == EstadoServicio.ARCHIVADO) {
            throw new ConflictoException("El servicio ya está archivado.");
        }

        if (tieneCompromisosAbiertos(idServicio)) {
            long presupuestadas = contar(idServicio, EstadoSolicitud.PRESUPUESTADA);
            if (presupuestadas > 0) {
                throw new ConflictoException(
                    "No podés dar de baja definitiva el servicio: tiene " + presupuestadas
                        + " presupuesto(s) vigente(s). Retirá el presupuesto antes de continuar.");
            }
            long pagadas = contar(idServicio, EstadoSolicitud.PAGADA);
            throw new ConflictoException(
                "No podés dar de baja definitiva el servicio: tiene " + pagadas
                    + " pedido(s) en curso. Esperá a que se entreguen.");
        }

        solicitudServicioService.cerrarPorBajaServicio(idServicio);

        servicio.setEstado(EstadoServicio.ARCHIVADO);
        servicio.setArchivadoEn(LocalDateTime.now());
        servicio.setArchivadoPor(usuarioRepository.findByIdUsuario(idArtista)
            .orElseThrow(() -> new NoSuchElementException(
                "Usuario no encontrado: " + idArtista)));
        servicioRepository.save(servicio);

        return ServicioDTO.desdeServicio(servicio);
    }

    /**
     * Compromisos que impiden archivar el servicio.
     *
     * <p>Hoy no existe la entidad Pedido: una solicitud PAGADA siempre representa un
     * compromiso en curso. Cuando se implementen los pedidos, la segunda condición debe
     * pasar a consultar el estado del pedido asociado (bloquear solo si no está entregado),
     * sin cambiar a los llamadores.</p>
     */
    boolean tieneCompromisosAbiertos(Long idServicio) {
        if (solicitudRepository.existsByServicioIdServicioAndEstado(
                idServicio, EstadoSolicitud.PRESUPUESTADA)) {
            return true;
        }
        return solicitudRepository.existsByServicioIdServicioAndEstado(
            idServicio, EstadoSolicitud.PAGADA);
    }

    private long contar(Long idServicio, EstadoSolicitud estado) {
        return solicitudRepository.countByServicioIdServicioAndEstadoIn(
            idServicio, List.of(estado));
    }
}
