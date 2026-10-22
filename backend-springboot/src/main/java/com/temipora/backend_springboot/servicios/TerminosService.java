package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.TerminoDerecho;
import com.temipora.backend_springboot.modelo.TerminoDerechoRequest;
import com.temipora.backend_springboot.modelo.Terminos;
import com.temipora.backend_springboot.modelo.TerminosDTO;
import com.temipora.backend_springboot.modelo.TerminosRequest;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.DerechoTerminoRepository;
import com.temipora.backend_springboot.repositorios.TerminosRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
@Validated
public class TerminosService {

    private static final String DERECHO_PERSONAL = "PERSONAL";

    private final TerminosRepository terminosRepository;
    private final DerechoTerminoRepository derechoTerminoRepository;
    private final UsuarioRepository usuarioRepository;

    public TerminosService(
            TerminosRepository terminosRepository,
            DerechoTerminoRepository derechoTerminoRepository,
            UsuarioRepository usuarioRepository) {
        this.terminosRepository = terminosRepository;
        this.derechoTerminoRepository = derechoTerminoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public TerminosDTO obtenerGlobal(Long idArtista) {
        return terminosRepository
            .findByArtistaIdUsuarioAndIdProductoIsNullAndIdServicioIsNull(idArtista)
            .map(TerminosDTO::desdeTerminos)
            .orElseGet(TerminosDTO::vacio);
    }

    @Transactional
    public TerminosDTO guardarGlobal(Long idArtista, TerminosRequest request) {
        validarContenido(request);
        Terminos terminos = terminosRepository
            .findByArtistaIdUsuarioAndIdProductoIsNullAndIdServicioIsNull(idArtista)
            .orElseGet(() -> nuevoTerminos(idArtista, null));
        return guardar(terminos, request);
    }

    @Transactional(readOnly = true)
    public TerminosDTO obtenerServicio(Long idServicio, Long idArtista) {
        return terminosRepository.findByArtistaIdUsuarioAndIdServicio(idArtista, idServicio)
            .map(terminos -> TerminosDTO.desdeTerminos(terminos, true))
            .orElseGet(() -> obtenerGlobal(idArtista));
    }

    @Transactional
    public TerminosDTO guardarServicio(Long idServicio, Long idArtista, TerminosRequest request) {
        validarContenido(request);
        Terminos terminos = terminosRepository
            .findByArtistaIdUsuarioAndIdServicio(idArtista, idServicio)
            .orElseGet(() -> nuevoTerminos(idArtista, idServicio));
        return guardar(terminos, request);
    }

    @Transactional
    public void eliminarServicio(Long idServicio, Long idArtista) {
        if (!tieneGlobalValido(idArtista)) {
            throw new IllegalArgumentException(
                "Antes de quitar los términos propios, completá tus términos globales.");
        }
        terminosRepository.findByArtistaIdUsuarioAndIdServicio(idArtista, idServicio)
            .ifPresent(terminosRepository::delete);
    }

    @Transactional(readOnly = true)
    public boolean tieneGlobalValido(Long idArtista) {
        return terminosRepository
            .findByArtistaIdUsuarioAndIdProductoIsNullAndIdServicioIsNull(idArtista)
            .map(this::tieneContenido)
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean tieneTerminosValidos(Long idServicio, Long idArtista) {
        return terminosRepository.findByArtistaIdUsuarioAndIdServicio(idArtista, idServicio)
            .map(this::tieneContenido)
            .orElseGet(() -> tieneGlobalValido(idArtista));
    }

    private TerminosDTO guardar(Terminos terminos, TerminosRequest request) {
        if (terminos.getIdTerminos() != null) {
            terminos.setVersion(terminos.getVersion() + 1);
            // Borra los hijos existentes ANTES de insertar los nuevos: si no, Hibernate
            // inserta primero y choca con el UNIQUE (id_terminos, id_derecho_termino).
            terminos.getTerminoDerechos().clear();
            terminosRepository.flush();
        }

        terminos.setInformacionGeneral(limpiar(request.getInformacionGeneral()));
        terminos.setPoliticaRevisiones(limpiar(request.getPoliticaRevisiones()));
        terminos.setPoliticaCancelaciones(limpiar(request.getPoliticaCancelaciones()));
        terminos.setUsoArtista(limpiar(request.getUsoArtista()));
        terminos.setUsoCliente(limpiar(request.getUsoCliente()));
        terminos.setPagoExtra(limpiar(request.getPagoExtra()));
        terminos.setQueDibuja(limpiar(request.getQueDibuja()));
        terminos.setQueNoDibuja(limpiar(request.getQueNoDibuja()));
        terminos.reemplazarDerechos(construirDerechos(request.getDerechos()));
        terminos.setFechaActualizacion(LocalDateTime.now());

        Terminos guardados = terminosRepository.save(terminos);
        return TerminosDTO.desdeTerminos(guardados, guardados.getIdServicio() != null);
    }

    private Terminos nuevoTerminos(Long idArtista, Long idServicio) {
        Usuario artista = usuarioRepository.findByIdUsuario(idArtista)
            .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idArtista));
        Terminos nuevo = new Terminos();
        nuevo.setArtista(artista);
        nuevo.setIdServicio(idServicio);
        nuevo.setVersion(1);
        return nuevo;
    }

    private List<TerminoDerecho> construirDerechos(List<TerminoDerechoRequest> requests) {
        List<TerminoDerecho> lista = new ArrayList<>();
        if (requests == null) {
            return lista;
        }
        Set<Long> vistos = new LinkedHashSet<>();
        for (TerminoDerechoRequest r : requests) {
            if (!vistos.add(r.getIdDerechoTermino())) {
                continue;
            }
            DerechoTermino derecho = derechoTerminoRepository.findByIdDerechoTermino(r.getIdDerechoTermino())
                .orElseThrow(() -> new IllegalArgumentException("Derecho no encontrado: " + r.getIdDerechoTermino()));

            boolean esPersonal = DERECHO_PERSONAL.equals(derecho.getCodigo());

            TerminoDerecho terminoDerecho = new TerminoDerecho();
            terminoDerecho.setDerecho(derecho);
            terminoDerecho.setDisponible(esPersonal || r.isDisponible());
            terminoDerecho.setPorcentajeExtra(esPersonal ? null : r.getPorcentajeExtra());
            lista.add(terminoDerecho);
        }
        return lista;
    }

    private void validarContenido(TerminosRequest r) {
        boolean hayTexto = Stream.of(
                r.getInformacionGeneral(),
                r.getPoliticaRevisiones(),
                r.getPoliticaCancelaciones(),
                r.getUsoArtista(),
                r.getUsoCliente(),
                r.getQueDibuja(),
                r.getQueNoDibuja())
            .anyMatch(s -> s != null && !s.isBlank());

        if (!hayTexto) {
            throw new IllegalArgumentException("Completá al menos una sección de tus términos.");
        }
    }

    private boolean tieneContenido(Terminos terminos) {
        return Stream.of(
                terminos.getInformacionGeneral(),
                terminos.getPoliticaRevisiones(),
                terminos.getPoliticaCancelaciones(),
                terminos.getUsoArtista(),
                terminos.getUsoCliente(),
                terminos.getQueDibuja(),
                terminos.getQueNoDibuja())
            .anyMatch(this::hayTexto);
    }

    private boolean hayTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String limpiar(String valor) {
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }
}
