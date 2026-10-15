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
import com.temipora.backend_springboot.modelo.TemaTermino;
import com.temipora.backend_springboot.modelo.TerminoDerecho;
import com.temipora.backend_springboot.modelo.TerminoDerechoRequest;
import com.temipora.backend_springboot.modelo.TerminoTema;
import com.temipora.backend_springboot.modelo.TerminoTemaRequest;
import com.temipora.backend_springboot.modelo.Terminos;
import com.temipora.backend_springboot.modelo.TerminosDTO;
import com.temipora.backend_springboot.modelo.TerminosRequest;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.DerechoTerminoRepository;
import com.temipora.backend_springboot.repositorios.TemaTerminoRepository;
import com.temipora.backend_springboot.repositorios.TerminosRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
@Validated
public class TerminosService {

    private static final String DERECHO_PERSONAL = "PERSONAL";

    private final TerminosRepository terminosRepository;
    private final TemaTerminoRepository temaTerminoRepository;
    private final DerechoTerminoRepository derechoTerminoRepository;
    private final UsuarioRepository usuarioRepository;

    public TerminosService(
            TerminosRepository terminosRepository,
            TemaTerminoRepository temaTerminoRepository,
            DerechoTerminoRepository derechoTerminoRepository,
            UsuarioRepository usuarioRepository) {
        this.terminosRepository = terminosRepository;
        this.temaTerminoRepository = temaTerminoRepository;
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
            .orElseGet(() -> {
                Usuario artista = usuarioRepository.findByIdUsuario(idArtista)
                    .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idArtista));
                Terminos nuevo = new Terminos();
                nuevo.setArtista(artista);
                nuevo.setVersion(1);
                return nuevo;
            });

        if (terminos.getIdTerminos() != null) {
            terminos.setVersion(terminos.getVersion() + 1);
            // Borra los hijos existentes ANTES de insertar los nuevos: si no, Hibernate
            // inserta primero y choca con el UNIQUE (id_terminos, id_tema_termino/id_derecho_termino).
            terminos.getTerminoTemas().clear();
            terminos.getTerminoDerechos().clear();
            terminosRepository.flush();
        }

        terminos.setInformacionGeneral(limpiar(request.getInformacionGeneral()));
        terminos.setPoliticaRevisiones(limpiar(request.getPoliticaRevisiones()));
        terminos.setPoliticaCancelaciones(limpiar(request.getPoliticaCancelaciones()));
        terminos.setUsoArtista(limpiar(request.getUsoArtista()));
        terminos.setUsoCliente(limpiar(request.getUsoCliente()));
        terminos.setPagoExtra(limpiar(request.getPagoExtra()));
        terminos.setTiempoEntrega(limpiar(request.getTiempoEntrega()));
        terminos.reemplazarTemas(construirTemas(request.getTemas()));
        terminos.reemplazarDerechos(construirDerechos(request.getDerechos()));
        terminos.setFechaActualizacion(LocalDateTime.now());

        return TerminosDTO.desdeTerminos(terminosRepository.save(terminos));
    }

    private List<TerminoTema> construirTemas(List<TerminoTemaRequest> requests) {
        List<TerminoTema> lista = new ArrayList<>();
        if (requests == null) {
            return lista;
        }
        Set<String> vistos = new LinkedHashSet<>();
        for (TerminoTemaRequest r : requests) {
            TerminoTema terminoTema = new TerminoTema();
            terminoTema.setEstado(r.getEstado());

            if (r.getIdTemaTermino() != null) {
                if (!vistos.add("t" + r.getIdTemaTermino())) {
                    continue;
                }
                TemaTermino tema = temaTerminoRepository.findByIdTemaTermino(r.getIdTemaTermino())
                    .orElseThrow(() -> new IllegalArgumentException("Tema no encontrado: " + r.getIdTemaTermino()));
                terminoTema.setTema(tema);
            } else {
                String nombre = r.getNombre() == null ? "" : r.getNombre().trim();
                if (nombre.isEmpty()) {
                    throw new IllegalArgumentException("El tema personalizado necesita un nombre.");
                }
                if (!vistos.add("c" + nombre.toLowerCase())) {
                    continue;
                }
                terminoTema.setNombrePersonalizado(nombre);
            }
            lista.add(terminoTema);
        }
        return lista;
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
                r.getTiempoEntrega())
            .anyMatch(s -> s != null && !s.isBlank());

        boolean hayEstructura = (r.getTemas() != null && !r.getTemas().isEmpty())
            || (r.getDerechos() != null && !r.getDerechos().isEmpty());

        if (!hayTexto && !hayEstructura) {
            throw new IllegalArgumentException("Completá al menos una sección de tus términos.");
        }
    }

    private String limpiar(String valor) {
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }
}
