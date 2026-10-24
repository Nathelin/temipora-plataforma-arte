package com.temipora.backend_springboot.servicios;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EstadoServicio;
import com.temipora.backend_springboot.modelo.Imagen;
import com.temipora.backend_springboot.modelo.ImagenEtiqueta;
import com.temipora.backend_springboot.modelo.PerfilArtisticoDTO;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioEtiqueta;
import com.temipora.backend_springboot.repositorios.ImagenEtiquetaRepository;
import com.temipora.backend_springboot.repositorios.ImagenRepository;
import com.temipora.backend_springboot.repositorios.ServicioRepository;

@Service
public class PerfilArtisticoService {

    private static final int MAXIMO_ETIQUETAS = 5;
    private static final int MAXIMO_TIPOS_SERVICIO = 3;

    private final ImagenRepository imagenRepository;
    private final ImagenEtiquetaRepository imagenEtiquetaRepository;
    private final ServicioRepository servicioRepository;

    public PerfilArtisticoService(
            ImagenRepository imagenRepository,
            ImagenEtiquetaRepository imagenEtiquetaRepository,
            ServicioRepository servicioRepository) {
        this.imagenRepository = imagenRepository;
        this.imagenEtiquetaRepository = imagenEtiquetaRepository;
        this.servicioRepository = servicioRepository;
    }

    @Transactional(readOnly = true)
    public PerfilArtisticoDTO calcular(Long idArtista) {
        if (idArtista == null) {
            return new PerfilArtisticoDTO();
        }
        return calcularParaArtistas(List.of(idArtista))
            .getOrDefault(idArtista, new PerfilArtisticoDTO());
    }

    @Transactional(readOnly = true)
    public Map<Long, PerfilArtisticoDTO> calcularParaArtistas(Collection<Long> idsArtistas) {
        if (idsArtistas == null || idsArtistas.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = idsArtistas.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        List<Imagen> imagenes = imagenRepository.findByUsuarioIdUsuarioInAndEstadoTrue(ids);
        List<Servicio> servicios = servicioRepository.findByArtistaIdUsuarioInAndEstado(
            ids, EstadoServicio.PUBLICADO);

        List<Long> idsImagenes = imagenes.stream()
            .map(Imagen::getIdImagen)
            .filter(Objects::nonNull)
            .toList();
        Map<Long, List<ImagenEtiqueta>> etiquetasPorImagen = idsImagenes.isEmpty()
            ? Map.of()
            : imagenEtiquetaRepository.findByImagenIdImagenIn(idsImagenes).stream()
                .collect(Collectors.groupingBy(etiqueta -> etiqueta.getImagen().getIdImagen()));

        Map<Long, List<Imagen>> imagenesPorArtista = imagenes.stream()
            .collect(Collectors.groupingBy(imagen -> imagen.getUsuario().getIdUsuario()));
        Map<Long, List<Servicio>> serviciosPorArtista = servicios.stream()
            .collect(Collectors.groupingBy(servicio -> servicio.getArtista().getIdUsuario()));

        Map<Long, PerfilArtisticoDTO> resultado = new HashMap<>();
        for (Long idArtista : ids) {
            resultado.put(idArtista, calcularUno(
                imagenesPorArtista.getOrDefault(idArtista, List.of()),
                serviciosPorArtista.getOrDefault(idArtista, List.of()),
                etiquetasPorImagen));
        }
        return resultado;
    }

    private PerfilArtisticoDTO calcularUno(
            List<Imagen> imagenes,
            List<Servicio> servicios,
            Map<Long, List<ImagenEtiqueta>> etiquetasPorImagen) {

        Map<String, Integer> tiposArte = new HashMap<>();
        Map<String, Integer> medios = new HashMap<>();
        Map<String, Integer> etiquetas = new HashMap<>();
        Map<String, Integer> tiposServicio = new HashMap<>();

        for (Imagen imagen : imagenes) {
            if (imagen.getTipoArte() != null) {
                contar(tiposArte, imagen.getTipoArte().getNombre());
                if (imagen.getTipoArte().getMedio() != null) {
                    contar(medios, imagen.getTipoArte().getMedio().getNombre());
                }
            }
            for (ImagenEtiqueta etiqueta : etiquetasPorImagen.getOrDefault(imagen.getIdImagen(), List.of())) {
                if (etiqueta.getEtiqueta() != null) {
                    contar(etiquetas, etiqueta.getEtiqueta().getNombre());
                }
            }
        }

        for (Servicio servicio : servicios) {
            if (servicio.getTipoServicio() != null) {
                contar(tiposServicio, servicio.getTipoServicio().getNombre());
            }
            for (ServicioEtiqueta asignacion : servicio.getEtiquetas()) {
                if (asignacion.getEtiqueta() != null) {
                    contar(etiquetas, asignacion.getEtiqueta().getNombre());
                }
            }
        }

        return PerfilArtisticoDTO.builder()
            .tipoArtePrincipal(primero(tiposArte))
            .medioPrincipal(primero(medios))
            .etiquetasDestacadas(top(etiquetas, MAXIMO_ETIQUETAS))
            .tiposServicioDestacados(top(tiposServicio, MAXIMO_TIPOS_SERVICIO))
            .cantidadImagenes(imagenes.size())
            .cantidadServicios(servicios.size())
            .build();
    }

    private void contar(Map<String, Integer> contador, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        contador.merge(valor, 1, Integer::sum);
    }

    private String primero(Map<String, Integer> contador) {
        List<String> top = top(contador, 1);
        return top.isEmpty() ? null : top.get(0);
    }

    private List<String> top(Map<String, Integer> contador, int limite) {
        return contador.entrySet().stream()
            .sorted((a, b) -> {
                int porCantidad = Integer.compare(b.getValue(), a.getValue());
                return porCantidad != 0
                    ? porCantidad
                    : a.getKey().compareToIgnoreCase(b.getKey());
            })
            .limit(limite)
            .map(Map.Entry::getKey)
            .toList();
    }
}
