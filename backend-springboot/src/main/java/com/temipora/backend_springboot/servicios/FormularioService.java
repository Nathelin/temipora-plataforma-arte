package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.Formulario;
import com.temipora.backend_springboot.modelo.FormularioDTO;
import com.temipora.backend_springboot.modelo.FormularioRequest;
import com.temipora.backend_springboot.modelo.OpcionPregunta;
import com.temipora.backend_springboot.modelo.Pregunta;
import com.temipora.backend_springboot.modelo.PreguntaRequest;
import com.temipora.backend_springboot.modelo.TipoCampo;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.FormularioRepository;
import com.temipora.backend_springboot.repositorios.TipoCampoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
@Validated
public class FormularioService {

    private static final String ENCABEZADO = "ENCABEZADO";
    private static final Set<String> TIPOS_CON_OPCIONES = Set.of("CASILLAS", "OPCIONES");

    private final FormularioRepository formularioRepository;
    private final TipoCampoRepository tipoCampoRepository;
    private final UsuarioRepository usuarioRepository;

    public FormularioService(
            FormularioRepository formularioRepository,
            TipoCampoRepository tipoCampoRepository,
            UsuarioRepository usuarioRepository) {
        this.formularioRepository = formularioRepository;
        this.tipoCampoRepository = tipoCampoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public FormularioDTO obtenerGlobal(Long idArtista) {
        return formularioRepository
            .findByArtistaIdUsuarioAndIdServicioIsNullAndEsSistemaFalse(idArtista)
            .map(FormularioDTO::desdeFormulario)
            .orElseGet(() -> FormularioDTO.desdeFormulario(formularioSistema()));
    }

    @Transactional
    public FormularioDTO guardarGlobal(Long idArtista, FormularioRequest request) {
        List<Pregunta> preguntas = construirPreguntas(request.getPreguntas());
        validarAlMenosUnaEntrada(preguntas);

        Formulario formulario = formularioRepository
            .findByArtistaIdUsuarioAndIdServicioIsNullAndEsSistemaFalse(idArtista)
            .orElseGet(() -> {
                Usuario artista = usuarioRepository.findByIdUsuario(idArtista)
                    .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + idArtista));
                Formulario nuevo = new Formulario();
                nuevo.setArtista(artista);
                nuevo.setEsSistema(false);
                return nuevo;
            });

        formulario.reemplazarPreguntas(preguntas);
        formulario.setFechaActualizacion(LocalDateTime.now());
        return FormularioDTO.desdeFormulario(formularioRepository.save(formulario));
    }

    private List<Pregunta> construirPreguntas(List<PreguntaRequest> requests) {
        List<Pregunta> preguntas = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            PreguntaRequest pr = requests.get(i);
            TipoCampo tipo = tipoCampoRepository.findByIdTipoCampo(pr.getIdTipoCampo())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de campo no encontrado: " + pr.getIdTipoCampo()));

            Pregunta pregunta = new Pregunta();
            pregunta.setTipoCampo(tipo);
            pregunta.setEnunciado(pr.getEnunciado().trim());
            pregunta.setAyuda(pr.getAyuda() != null && !pr.getAyuda().isBlank() ? pr.getAyuda().trim() : null);
            pregunta.setObligatoria(pr.isObligatoria());
            pregunta.setOrden(i);

            if (TIPOS_CON_OPCIONES.contains(tipo.getCodigo())) {
                List<String> opciones = limpiarOpciones(pr.getOpciones());
                if (opciones.isEmpty()) {
                    throw new IllegalArgumentException(
                        "La pregunta '" + pregunta.getEnunciado() + "' necesita al menos una opción.");
                }
                int orden = 0;
                for (String texto : opciones) {
                    OpcionPregunta opcion = new OpcionPregunta();
                    opcion.setPregunta(pregunta);
                    opcion.setTexto(texto);
                    opcion.setOrden(orden++);
                    pregunta.getOpciones().add(opcion);
                }
            }

            preguntas.add(pregunta);
        }
        return preguntas;
    }

    private List<String> limpiarOpciones(List<String> opciones) {
        if (opciones == null) {
            return List.of();
        }
        return opciones.stream()
            .filter(o -> o != null && !o.isBlank())
            .map(String::trim)
            .toList();
    }

    private void validarAlMenosUnaEntrada(List<Pregunta> preguntas) {
        boolean hayEntrada = preguntas.stream()
            .anyMatch(p -> !ENCABEZADO.equals(p.getTipoCampo().getCodigo()));
        if (!hayEntrada) {
            throw new IllegalArgumentException(
                "El formulario debe tener al menos una pregunta que el cliente pueda completar.");
        }
    }

    private Formulario formularioSistema() {
        return formularioRepository.findFirstByEsSistemaTrue()
            .orElseThrow(() -> new IllegalStateException("No existe el formulario del sistema."));
    }
}
