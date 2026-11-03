package com.temipora.backend_springboot.servicios;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioDTO;
import com.temipora.backend_springboot.repositorios.ServicioRepository;

@Service
public class ServicioAdminService {

    private static final String ENTIDAD = "Servicio";
    private static final List<CampoAuditado> CAMPOS = List.of(
        CampoAuditado.de("estado", "Estado"));

    private final ServicioRepository servicioRepository;
    private final AuditoriaService auditoria;

    public ServicioAdminService(
            ServicioRepository servicioRepository,
            AuditoriaService auditoria) {
        this.servicioRepository = servicioRepository;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<ServicioDTO> listarArchivados() {
        return servicioRepository.findByEstadoOrderByArchivadoEnDesc(EstadoServicio.ARCHIVADO)
            .stream()
            .map(ServicioDTO::desdeServicio)
            .toList();
    }

    @Transactional
    public ServicioDTO restaurar(Long idServicio) {
        Servicio servicio = servicioRepository.findByIdServicioForUpdate(idServicio)
            .orElseThrow(() -> new NoSuchElementException(
                "Servicio no encontrado: " + idServicio));
        if (servicio.getEstado() != EstadoServicio.ARCHIVADO) {
            throw new ConflictoException("El servicio no está archivado.");
        }
        Snapshot antes = Snapshot.de(servicio, CAMPOS);
        servicio.setEstado(EstadoServicio.PAUSADO);
        servicio.setPausadoEn(null);
        servicioRepository.save(servicio);
        auditoria.registrar(
            AccionesAdmin.SERVICIO_RESTAURAR, ENTIDAD, servicio.getIdServicio(),
            "Restauró el servicio “" + servicio.getNombre() + "”", antes, servicio, CAMPOS);
        return ServicioDTO.desdeServicio(servicio);
    }
}
