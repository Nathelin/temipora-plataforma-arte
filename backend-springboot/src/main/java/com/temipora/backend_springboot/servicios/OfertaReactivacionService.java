package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.CampanaReactivacion;
import com.temipora.backend_springboot.modelo.EstadoCampanaReactivacion;
import com.temipora.backend_springboot.modelo.EstadoOfertaReactivacion;
import com.temipora.backend_springboot.modelo.OfertaReactivacion;
import com.temipora.backend_springboot.repositorios.CampanaReactivacionRepository;
import com.temipora.backend_springboot.repositorios.OfertaReactivacionRepository;

@Service
public class OfertaReactivacionService {

    private static final List<EstadoOfertaReactivacion> ESTADOS_VIGENTES =
        List.of(EstadoOfertaReactivacion.OFRECIDA,
            EstadoOfertaReactivacion.RESERVADA);

    private final OfertaReactivacionRepository ofertaRepository;
    private final CampanaReactivacionRepository campanaRepository;

    public OfertaReactivacionService(
            OfertaReactivacionRepository ofertaRepository,
            CampanaReactivacionRepository campanaRepository) {
        this.ofertaRepository = ofertaRepository;
        this.campanaRepository = campanaRepository;
    }

    @Transactional(readOnly = true)
    public List<OfertaReactivacion> activasDeUsuario(Long idUsuario) {
        LocalDateTime ahora = LocalDateTime.now();
        return ofertaRepository.findActivasPorUsuario(
                idUsuario, ESTADOS_VIGENTES, ahora)
            .stream()
            .filter(oferta -> oferta.getCampana() != null
                && oferta.getCampana().getEstado() == EstadoCampanaReactivacion.ACTIVA
                && oferta.getCampana().getFechaVencimiento() != null
                && oferta.getCampana().getFechaVencimiento().isAfter(ahora))
            .toList();
    }

    @Transactional(readOnly = true)
    public OfertaReactivacion ofertaDeUsuario(Long idOferta, Long idUsuario) {
        OfertaReactivacion oferta = ofertaRepository
            .findByIdConDetalle(idOferta)
            .orElseThrow(() -> new NoSuchElementException(
                "La oferta no existe."));
        if (oferta.getCampana() == null
                || oferta.getCampana().getUsuario() == null
                || !oferta.getCampana().getUsuario().getIdUsuario()
                    .equals(idUsuario)) {
            throw new AccesoDenegadoException(
                "La oferta no pertenece a tu cuenta.");
        }
        return oferta;
    }

    @Transactional(readOnly = true)
    public CampanaReactivacion campanaPorToken(String token) {
        return campanaRepository.findByTokenConOfertas(token)
            .orElseThrow(() -> new NoSuchElementException(
                "Campaña no encontrada."));
    }

    @Transactional
    public OfertaReactivacion validarParaSolicitud(
            Long idOferta,
            Long idCliente,
            Long idServicio) {
        OfertaReactivacion oferta = ofertaRepository.findByIdOferta(idOferta)
            .orElseThrow(() -> new NoSuchElementException(
                "La oferta no existe."));
        if (oferta.getCampana() == null
                || oferta.getCampana().getUsuario() == null
                || !oferta.getCampana().getUsuario().getIdUsuario().equals(idCliente)) {
            throw new IllegalArgumentException(
                "La oferta no pertenece a tu cuenta.");
        }
        if (oferta.getServicio() == null
                || !oferta.getServicio().getIdServicio().equals(idServicio)) {
            throw new IllegalArgumentException(
                "La oferta no corresponde a este servicio.");
        }
        EstadoOfertaReactivacion estado = oferta.getEstado();
        if (estado != EstadoOfertaReactivacion.OFRECIDA
                && estado != EstadoOfertaReactivacion.RESERVADA) {
            throw new IllegalArgumentException(
                "La oferta ya fue utilizada o venció.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (oferta.getFechaVencimiento() == null
                || !oferta.getFechaVencimiento().isAfter(ahora)
                || oferta.getCampana().getEstado() != EstadoCampanaReactivacion.ACTIVA
                || oferta.getCampana().getFechaVencimiento() == null
                || !oferta.getCampana().getFechaVencimiento().isAfter(ahora)) {
            oferta.setEstado(EstadoOfertaReactivacion.VENCIDA);
            throw new IllegalArgumentException(
                "La oferta venció. Podés solicitar el servicio sin descuento.");
        }
        oferta.setEstado(EstadoOfertaReactivacion.RESERVADA);
        return oferta;
    }

    @Transactional
    public void marcarUtilizada(OfertaReactivacion oferta) {
        if (oferta == null) {
            return;
        }
        oferta.setEstado(EstadoOfertaReactivacion.UTILIZADA);
        ofertaRepository.save(oferta);
    }
}
