package com.temipora.backend_springboot.servicios;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.repositorios.RecomendacionProductoRepository;
import com.temipora.backend_springboot.repositorios.RecomendacionRepository;
import com.temipora.backend_springboot.repositorios.VisitaArtistaRepository;
import com.temipora.backend_springboot.repositorios.VisitaProductoRepository;
import com.temipora.backend_springboot.repositorios.VisitaServicioRepository;

/**
 * Intereses de contenido del usuario, unificados entre servicios y productos.
 *
 * Las etiquetas y categorías son un vocabulario global compartido, y la tabla
 * {@code artista_favorito} también lo es. Por eso un interés mostrado en el
 * módulo de servicios (comprar, guardar o visitar) enriquece las
 * recomendaciones de productos sin duplicar datos ni lógica.
 */
@Service
public class InteresUsuarioService {

    private final RecomendacionRepository recomendacionRepository;
    private final RecomendacionProductoRepository recomendacionProductoRepository;
    private final VisitaServicioRepository visitaServicioRepository;
    private final VisitaProductoRepository visitaProductoRepository;
    private final VisitaArtistaRepository visitaArtistaRepository;

    public InteresUsuarioService(
            RecomendacionRepository recomendacionRepository,
            RecomendacionProductoRepository recomendacionProductoRepository,
            VisitaServicioRepository visitaServicioRepository,
            VisitaProductoRepository visitaProductoRepository,
            VisitaArtistaRepository visitaArtistaRepository) {
        this.recomendacionRepository = recomendacionRepository;
        this.recomendacionProductoRepository = recomendacionProductoRepository;
        this.visitaServicioRepository = visitaServicioRepository;
        this.visitaProductoRepository = visitaProductoRepository;
        this.visitaArtistaRepository = visitaArtistaRepository;
    }

    public record InteresContenido(
            Set<Long> artistasFavoritos,
            Set<Long> artistasComprados,
            Set<Long> artistasVisitados,
            Set<Long> etiquetas,
            Set<Long> categorias,
            Map<Long, LocalDateTime> ultimaInteraccion) {

        public boolean vacio() {
            return artistasFavoritos.isEmpty()
                && artistasComprados.isEmpty()
                && artistasVisitados.isEmpty()
                && etiquetas.isEmpty();
        }
    }

    @Transactional(readOnly = true)
    public InteresContenido cargar(Long idUsuario, int diasVisitas) {
        LocalDate desde = LocalDate.now()
            .minusDays(Math.max(1, diasVisitas));

        Set<Long> artistasFavoritos = new HashSet<>(
            safe(recomendacionRepository.artistasFavoritos(idUsuario)));

        Set<Long> artistasComprados = new HashSet<>();
        artistasComprados.addAll(safe(
            recomendacionRepository.artistasComprados(idUsuario)));
        artistasComprados.addAll(safe(
            recomendacionProductoRepository.artistasComprados(idUsuario)));

        Set<Long> etiquetas = new HashSet<>();
        etiquetas.addAll(safe(recomendacionRepository.etiquetasCompradas(idUsuario)));
        etiquetas.addAll(safe(recomendacionRepository.etiquetasFavoritas(idUsuario)));
        etiquetas.addAll(safe(
            visitaServicioRepository.etiquetasVisitadas(idUsuario, desde)));
        etiquetas.addAll(safe(
            recomendacionProductoRepository.etiquetasFavoritas(idUsuario)));
        etiquetas.addAll(safe(
            recomendacionProductoRepository.etiquetasCompradas(idUsuario)));
        etiquetas.addAll(safe(
            visitaProductoRepository.etiquetasVisitadas(idUsuario, desde)));

        Set<Long> categorias = etiquetas.isEmpty()
            ? new HashSet<>()
            : new HashSet<>(safe(
                recomendacionRepository.categoriasDeEtiquetas(etiquetas)));

        Set<Long> artistasVisitados = new HashSet<>();
        Map<Long, LocalDateTime> ultimaInteraccion = new HashMap<>();
        for (Object[] fila : safe(
                visitaArtistaRepository.resumenPorUsuario(idUsuario, desde))) {
            Long idArtista = numero(fila[0]);
            if (idArtista != null) {
                artistasVisitados.add(idArtista);
            }
            actualizarUltima(ultimaInteraccion, idArtista, aFecha(fila[2]));
        }
        for (Object[] fila : safe(
                recomendacionRepository.ultimaCompraPorArtista(idUsuario))) {
            actualizarUltima(ultimaInteraccion, numero(fila[0]), aFecha(fila[1]));
        }
        for (Object[] fila : safe(
                recomendacionRepository.ultimoFavoritoArtista(idUsuario))) {
            actualizarUltima(ultimaInteraccion, numero(fila[0]), aFecha(fila[1]));
        }
        for (Object[] fila : safe(
                recomendacionProductoRepository.ultimaCompraPorArtista(idUsuario))) {
            actualizarUltima(ultimaInteraccion, numero(fila[0]), aFecha(fila[1]));
        }

        return new InteresContenido(
            artistasFavoritos,
            artistasComprados,
            artistasVisitados,
            etiquetas,
            categorias,
            ultimaInteraccion);
    }

    private void actualizarUltima(
            Map<Long, LocalDateTime> mapa,
            Long id,
            LocalDateTime fecha) {
        if (id == null || fecha == null) {
            return;
        }
        mapa.merge(id, fecha, (anterior, nueva) ->
            anterior.isAfter(nueva) ? anterior : nueva);
    }

    private Long numero(Object valor) {
        return valor == null ? null : ((Number) valor).longValue();
    }

    private LocalDateTime aFecha(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof LocalDateTime fecha) {
            return fecha;
        }
        if (valor instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (valor instanceof java.sql.Date fechaSql) {
            return fechaSql.toLocalDate().atStartOfDay();
        }
        return null;
    }

    private <T> List<T> safe(List<T> valores) {
        return valores != null ? valores : List.of();
    }
}
