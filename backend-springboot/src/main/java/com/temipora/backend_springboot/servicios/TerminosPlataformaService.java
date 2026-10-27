package com.temipora.backend_springboot.servicios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ComisionConfigDTO;
import com.temipora.backend_springboot.modelo.TerminosPlataforma;
import com.temipora.backend_springboot.modelo.TerminosPlataformaDTO;
import com.temipora.backend_springboot.modelo.TerminosPlataformaRequest;
import com.temipora.backend_springboot.repositorios.TerminosPlataformaRepository;

@Service
public class TerminosPlataformaService {

    private final TerminosPlataformaRepository terminosRepository;
    private final ComisionService comisionService;

    public TerminosPlataformaService(
            TerminosPlataformaRepository terminosRepository,
            ComisionService comisionService) {
        this.terminosRepository = terminosRepository;
        this.comisionService = comisionService;
    }

    @Transactional(readOnly = true)
    public TerminosPlataformaDTO obtenerVigente() {
        return terminosRepository.findFirstByVigenteTrueOrderByVersionDesc()
            .map(TerminosPlataformaDTO::desdeTerminos)
            .orElseThrow(() -> new NoSuchElementException(
                "La plataforma todavía no publicó sus términos y condiciones."));
    }

    @Transactional(readOnly = true)
    public List<TerminosPlataformaDTO> listarVersiones() {
        return terminosRepository.findAllByOrderByVersionDesc().stream()
            .map(TerminosPlataformaDTO::desdeTerminos)
            .toList();
    }

    /**
     * Publica una versión nueva e inmutable. La comisión declarada se congela
     * con los valores vigentes al momento de publicar, para que el documento
     * no cambie si después se modifica la configuración.
     */
    @Transactional
    public TerminosPlataformaDTO publicar(TerminosPlataformaRequest request) {
        String titulo = limpiar(request.getTitulo(), "El título es obligatorio.", 120);
        String contenido = limpiar(
            request.getContenido(), "El contenido es obligatorio.", Integer.MAX_VALUE);

        ComisionConfigDTO comision = comisionService.obtenerEstado();

        int version = terminosRepository.findFirstByOrderByVersionDesc()
            .map(actual -> actual.getVersion() + 1)
            .orElse(1);

        for (TerminosPlataforma anterior : terminosRepository.findByVigenteTrue()) {
            anterior.setVigente(false);
            terminosRepository.save(anterior);
        }

        TerminosPlataforma terminos = new TerminosPlataforma();
        terminos.setVersion(version);
        terminos.setTitulo(titulo);
        terminos.setContenido(contenido);
        terminos.setComisionServicio(comision.getPorcentajeServicio());
        terminos.setComisionProducto(comision.getPorcentajeProducto());
        terminos.setFechaPublicacion(LocalDateTime.now());
        terminos.setVigente(true);
        return TerminosPlataformaDTO.desdeTerminos(terminosRepository.save(terminos));
    }

    private String limpiar(String valor, String mensaje, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        String limpio = valor.trim();
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException(
                "El texto no puede superar los " + maximo + " caracteres.");
        }
        return limpio;
    }
}
