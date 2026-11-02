package com.temipora.backend_springboot.servicios;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.repositorios.VisitaArtistaRepository;
import com.temipora.backend_springboot.repositorios.VisitaServicioRepository;

/**
 * Registro de visitas para el recomendador.
 *
 * Se escribe en una transacción aparte para no ensuciar la lectura pública:
 * si el registro falla, el detalle del servicio o el perfil se siguen
 * devolviendo normalmente.
 */
@Service
public class VisitaService {

    private static final Logger log = LoggerFactory.getLogger(VisitaService.class);

    private final VisitaArtistaRepository visitaArtistaRepository;
    private final VisitaServicioRepository visitaServicioRepository;

    public VisitaService(
            VisitaArtistaRepository visitaArtistaRepository,
            VisitaServicioRepository visitaServicioRepository) {
        this.visitaArtistaRepository = visitaArtistaRepository;
        this.visitaServicioRepository = visitaServicioRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarArtista(Long idUsuario, Long idArtista) {
        if (idUsuario == null || idArtista == null || idUsuario.equals(idArtista)) {
            return;
        }
        try {
            visitaArtistaRepository.registrar(idUsuario, idArtista);
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar la visita del usuario {} al artista {}",
                idUsuario, idArtista, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarServicio(Long idUsuario, Long idServicio) {
        if (idUsuario == null || idServicio == null) {
            return;
        }
        try {
            visitaServicioRepository.registrar(idUsuario, idServicio);
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar la visita del usuario {} al servicio {}",
                idUsuario, idServicio, e);
        }
    }
}
