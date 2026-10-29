package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.temipora.backend_springboot.modelo.ConfiguracionResenia;
import com.temipora.backend_springboot.modelo.CriterioPromedioDTO;
import com.temipora.backend_springboot.modelo.DistribucionEstrellasDTO;
import com.temipora.backend_springboot.modelo.EstadoPedido;
import com.temipora.backend_springboot.modelo.EstadoResenia;
import com.temipora.backend_springboot.modelo.EtiquetaReputacion;
import com.temipora.backend_springboot.modelo.FiltroReseniaServicioDTO;
import com.temipora.backend_springboot.modelo.OpcionDestacadaDTO;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.PaginaReseniasDTO;
import com.temipora.backend_springboot.modelo.PolaridadOpcion;
import com.temipora.backend_springboot.modelo.PromedioReseniasDTO;
import com.temipora.backend_springboot.modelo.ReputacionClienteDTO;
import com.temipora.backend_springboot.modelo.Resenia;
import com.temipora.backend_springboot.modelo.ReseniaOpcion;
import com.temipora.backend_springboot.modelo.ReseniaOpcionDTO;
import com.temipora.backend_springboot.modelo.ReseniaPublicaDTO;
import com.temipora.backend_springboot.modelo.ReseniaPublicaResumenDTO;
import com.temipora.backend_springboot.modelo.ReseniaValor;
import com.temipora.backend_springboot.modelo.ReseniaValorDTO;
import com.temipora.backend_springboot.modelo.ReseniaVersion;
import com.temipora.backend_springboot.modelo.ResumenReseniasDTO;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.UsuarioResumenDTO;
import com.temipora.backend_springboot.repositorios.ReseniaOpcionRepository;
import com.temipora.backend_springboot.repositorios.ReseniaRepository;
import com.temipora.backend_springboot.repositorios.ReseniaValorRepository;
import com.temipora.backend_springboot.repositorios.ReseniaVersionRepository;

@Service
@Validated
public class ReseniaPublicaService {

    private static final int MINIMO_DESGLOSE = 3;
    private static final int TAMANIO_MAXIMO = 50;
    private static final String ORDEN_RECIENTES = "RECIENTES";
    private static final String ORDEN_MEJOR = "MEJOR";
    private static final String ORDEN_PEOR = "PEOR";
    private static final String AUTOR_ANONIMO = "Cliente verificado";

    private final ReseniaRepository reseniaRepository;
    private final ReseniaVersionRepository versionRepository;
    private final ReseniaValorRepository valorRepository;
    private final ReseniaOpcionRepository opcionRepository;
    private final ConfiguracionReseniaService configuracionService;

    public ReseniaPublicaService(
            ReseniaRepository reseniaRepository,
            ReseniaVersionRepository versionRepository,
            ReseniaValorRepository valorRepository,
            ReseniaOpcionRepository opcionRepository,
            ConfiguracionReseniaService configuracionService) {
        this.reseniaRepository = reseniaRepository;
        this.versionRepository = versionRepository;
        this.valorRepository = valorRepository;
        this.opcionRepository = opcionRepository;
        this.configuracionService = configuracionService;
    }

    @Transactional(readOnly = true)
    public Map<Long, ReseniaPublicaResumenDTO> resumenPorPedidos(
            Collection<Long> idsPedido) {
        if (idsPedido == null || idsPedido.isEmpty()) {
            return Map.of();
        }
        List<Resenia> resenias = reseniaRepository.findPublicasPorPedidos(
            idsPedido, EstadoResenia.PUBLICADA, EstadoPedido.FINALIZADO);
        if (resenias.isEmpty()) {
            return Map.of();
        }
        Map<Long, ReseniaVersion> versiones = versionesActuales(resenias);
        Map<Long, ReseniaPublicaResumenDTO> mapa = new HashMap<>();
        for (Resenia resenia : resenias) {
            mapa.put(
                resenia.getPedido().getIdPedido(),
                aResumen(resenia, versiones.get(resenia.getIdResenia())));
        }
        return mapa;
    }

    @Transactional(readOnly = true)
    public ReseniaPublicaResumenDTO resumenPorPedido(Long idPedido) {
        return resumenPorPedidos(List.of(idPedido)).get(idPedido);
    }

    @Transactional(readOnly = true)
    public ResumenReseniasDTO resumenServicio(Long idServicio) {
        return resumen(idServicio, null);
    }

    @Transactional(readOnly = true)
    public ResumenReseniasDTO resumenArtista(Long idArtista) {
        return resumen(null, idArtista);
    }

    @Transactional(readOnly = true)
    public PaginaReseniasDTO listarServicio(
            Long idServicio,
            Integer calificacion,
            String orden,
            int pagina,
            int tamanio) {
        validarCalificacion(calificacion);
        Pageable pageable = pagina(pagina, tamanio);
        Page<Resenia> page = reseniaRepository.findPublicasPorServicio(
            idServicio,
            calificacion,
            normalizarOrden(orden),
            EstadoResenia.PUBLICADA,
            EstadoPedido.FINALIZADO,
            pageable);

        return PaginaReseniasDTO.builder()
            .resumen(resumenServicio(idServicio))
            .pagina(aPagina(page))
            .servicios(List.of())
            .build();
    }

    @Transactional(readOnly = true)
    public PaginaReseniasDTO listarArtista(
            Long idArtista,
            Long idServicio,
            Integer calificacion,
            String orden,
            int pagina,
            int tamanio) {
        validarCalificacion(calificacion);
        Pageable pageable = pagina(pagina, tamanio);
        Page<Resenia> page = reseniaRepository.findPublicasPorArtista(
            idArtista,
            idServicio,
            calificacion,
            normalizarOrden(orden),
            EstadoResenia.PUBLICADA,
            EstadoPedido.FINALIZADO,
            pageable);

        return PaginaReseniasDTO.builder()
            .resumen(resumenArtista(idArtista))
            .pagina(aPagina(page))
            .servicios(serviciosConResenias(idArtista))
            .build();
    }

    @Transactional(readOnly = true)
    public List<FiltroReseniaServicioDTO> serviciosConResenias(Long idArtista) {
        return reseniaRepository
            .serviciosConResenias(idArtista, EstadoResenia.PUBLICADA).stream()
            .map(fila -> FiltroReseniaServicioDTO.builder()
                .idServicio(((Number) fila[0]).longValue())
                .nombre((String) fila[1])
                .build())
            .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, PromedioReseniasDTO> promediosPorServicios(
            Collection<Long> idsServicio) {
        if (idsServicio == null || idsServicio.isEmpty()) {
            return Map.of();
        }
        Map<Long, PromedioReseniasDTO> mapa = new HashMap<>();
        for (Object[] fila : reseniaRepository.promediosPorServicios(
                idsServicio, EstadoResenia.PUBLICADA, EstadoPedido.FINALIZADO)) {
            Long id = ((Number) fila[0]).longValue();
            long cantidad = ((Number) fila[1]).longValue();
            Double promedioCrudo = fila[2] != null
                ? ((Number) fila[2]).doubleValue()
                : null;
            mapa.put(id, PromedioReseniasDTO.builder()
                .cantidad(cantidad)
                .promedio(decimal(promedioCrudo))
                .build());
        }
        return mapa;
    }

    @Transactional
    public Map<Long, ReputacionClienteDTO> reputacionPorClientes(
            Collection<Long> idsCliente) {
        if (idsCliente == null || idsCliente.isEmpty()) {
            return Map.of();
        }
        ConfiguracionResenia config = configuracionService.obtener();
        Map<Long, ReputacionClienteDTO> mapa = new HashMap<>();
        for (Object[] fila : reseniaRepository.reputacionPorClientes(
                idsCliente, EstadoResenia.PUBLICADA)) {
            Long id = ((Number) fila[0]).longValue();
            long cantidad = ((Number) fila[1]).longValue();
            Double promedioCrudo = fila[2] != null
                ? ((Number) fila[2]).doubleValue()
                : null;
            BigDecimal promedio = decimal(promedioCrudo);
            EtiquetaReputacion etiqueta =
                ReputacionCalculator.calcular(cantidad, promedio, config);
            mapa.put(id, ReputacionClienteDTO.builder()
                .idCliente(id)
                .cantidadResenias(cantidad)
                .promedio(etiqueta == EtiquetaReputacion.SIN_DATOS ? null : promedio)
                .etiqueta(etiqueta)
                .hayDatos(etiqueta != EtiquetaReputacion.SIN_DATOS)
                .minimoResenias(config.getMinimoReseniasEtiqueta())
                .build());
        }
        return mapa;
    }

    // -----------------------------------------------------------------
    // armado del resumen
    // -----------------------------------------------------------------

    private ResumenReseniasDTO resumen(Long idServicio, Long idArtista) {
        List<Object[]> filas = reseniaRepository.resumenConteoPromedio(
            idServicio, idArtista, EstadoResenia.PUBLICADA, EstadoPedido.FINALIZADO);
        Object[] fila = filas == null || filas.isEmpty() ? null : filas.get(0);

        long cantidad = fila != null && fila.length > 0 && fila[0] != null
            ? ((Number) fila[0]).longValue()
            : 0L;
        BigDecimal promedio = fila != null && fila.length > 1 && fila[1] != null
            ? decimal(((Number) fila[1]).doubleValue())
            : null;

        if (cantidad == 0 || promedio == null) {
            return ResumenReseniasDTO.vacio();
        }

        return ResumenReseniasDTO.builder()
            .promedio(promedio)
            .cantidad(cantidad)
            .hayDatos(true)
            .distribucion(distribucion(idServicio, idArtista, cantidad))
            .criterios(cantidad >= MINIMO_DESGLOSE
                ? criterios(idServicio, idArtista)
                : List.of())
            .destacados(cantidad >= MINIMO_DESGLOSE
                ? destacados(idServicio, idArtista, cantidad)
                : List.of())
            .build();
    }

    private List<DistribucionEstrellasDTO> distribucion(
            Long idServicio, Long idArtista, long total) {
        Map<Integer, Long> conteos = new HashMap<>();
        for (Object[] fila : reseniaRepository.distribucionPublica(
                idServicio, idArtista, EstadoResenia.PUBLICADA,
                EstadoPedido.FINALIZADO)) {
            conteos.put(
                ((Number) fila[0]).intValue(),
                ((Number) fila[1]).longValue());
        }

        java.util.List<DistribucionEstrellasDTO> distribucion =
            new java.util.ArrayList<>();
        for (int estrellas = 5; estrellas >= 1; estrellas--) {
            long cantidad = conteos.getOrDefault(estrellas, 0L);
            distribucion.add(DistribucionEstrellasDTO.builder()
                .estrellas(estrellas)
                .cantidad(cantidad)
                .porcentaje(porcentaje(cantidad, total))
                .build());
        }
        return distribucion;
    }

    private List<CriterioPromedioDTO> criterios(
            Long idServicio, Long idArtista) {
        return reseniaRepository.criteriosPublicos(
            idServicio, idArtista, EstadoResenia.PUBLICADA,
            EstadoPedido.FINALIZADO).stream()
            .map(fila -> CriterioPromedioDTO.builder()
                .idCriterio(((Number) fila[0]).longValue())
                .nombre((String) fila[1])
                .promedio(decimal(((Number) fila[3]).doubleValue()))
                .cantidad(((Number) fila[4]).longValue())
                .build())
            .toList();
    }

    private List<OpcionDestacadaDTO> destacados(
            Long idServicio, Long idArtista, long total) {
        return reseniaRepository.opcionesDestacadasPublicas(
            idServicio, idArtista, EstadoResenia.PUBLICADA,
            EstadoPedido.FINALIZADO).stream()
            .map(fila -> OpcionDestacadaDTO.builder()
                .idOpcion(((Number) fila[0]).longValue())
                .etiqueta((String) fila[1])
                .polaridad((PolaridadOpcion) fila[2])
                .cantidad(((Number) fila[3]).longValue())
                .porcentaje(porcentaje(((Number) fila[3]).longValue(), total))
                .build())
            .toList();
    }

    // -----------------------------------------------------------------
    // armado de la página
    // -----------------------------------------------------------------

    private PaginaDTO<ReseniaPublicaDTO> aPagina(Page<Resenia> page) {
        List<Resenia> contenido = page.getContent();
        List<ReseniaPublicaDTO> dtos = List.of();

        if (!contenido.isEmpty()) {
            Map<Long, ReseniaVersion> versiones = versionesActuales(contenido);
            Map<Long, List<ReseniaValor>> valores = valoresPorVersion(
                versiones.values());
            Map<Long, List<ReseniaOpcion>> opciones = opcionesPorVersion(
                versiones.values());

            dtos = contenido.stream()
                .map(resenia -> aPublica(
                    resenia,
                    versiones.get(resenia.getIdResenia()),
                    valores,
                    opciones))
                .toList();
        }

        return PaginaDTO.<ReseniaPublicaDTO>builder()
            .contenido(dtos)
            .pagina(page.getNumber())
            .tamanio(page.getSize())
            .totalElementos(page.getTotalElements())
            .totalPaginas(page.getTotalPages())
            .hayMas(page.hasNext())
            .build();
    }

    private Map<Long, ReseniaVersion> versionesActuales(
            Collection<Resenia> resenias) {
        if (resenias.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = resenias.stream().map(Resenia::getIdResenia).toList();
        return versionRepository.findActualesPorReseniaIds(ids).stream()
            .collect(Collectors.toMap(
                version -> version.getResenia().getIdResenia(),
                version -> version));
    }

    private Map<Long, List<ReseniaValor>> valoresPorVersion(
            Collection<ReseniaVersion> versiones) {
        if (versiones.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = versiones.stream()
            .map(ReseniaVersion::getIdVersion)
            .toList();
        return valorRepository.findByVersionIdVersionIn(ids).stream()
            .collect(Collectors.groupingBy(
                valor -> valor.getVersion().getIdVersion()));
    }

    private Map<Long, List<ReseniaOpcion>> opcionesPorVersion(
            Collection<ReseniaVersion> versiones) {
        if (versiones.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = versiones.stream()
            .map(ReseniaVersion::getIdVersion)
            .toList();
        return opcionRepository.findByVersionIdVersionIn(ids).stream()
            .collect(Collectors.groupingBy(
                opcion -> opcion.getVersion().getIdVersion()));
    }

    private ReseniaPublicaDTO aPublica(
            Resenia resenia,
            ReseniaVersion version,
            Map<Long, List<ReseniaValor>> valores,
            Map<Long, List<ReseniaOpcion>> opciones) {
        boolean anonima = esAnonima(resenia);
        Long idVersion = version != null ? version.getIdVersion() : null;

        ReseniaPublicaDTO.ReseniaPublicaDTOBuilder builder =
            ReseniaPublicaDTO.builder()
                .idResenia(resenia.getIdResenia())
                .nombreServicio(resenia.getPedido().getServicio().getNombre())
                .calificacionGeneral(version != null
                    ? version.getCalificacionGeneral()
                    : 0)
                .descripcion(version != null ? version.getDescripcion() : null)
                .fechaVersion(version != null ? version.getFechaVersion() : null)
                .editada(resenia.estaEditada())
                .autorMostrado(anonima
                    ? AUTOR_ANONIMO
                    : nombreMostrado(resenia.getAutor()))
                .anonima(anonima)
                .valores(idVersion != null
                    ? valores.getOrDefault(idVersion, List.of()).stream()
                        .map(ReseniaValorDTO::desdeValor).toList()
                    : List.of())
                .opciones(idVersion != null
                    ? opciones.getOrDefault(idVersion, List.of()).stream()
                        .map(ReseniaOpcionDTO::desdeOpcion).toList()
                    : List.of());

        if (!anonima) {
            builder.autor(UsuarioResumenDTO.desdeUsuario(resenia.getAutor()));
        }

        return builder.build();
    }

    private ReseniaPublicaResumenDTO aResumen(
            Resenia resenia, ReseniaVersion version) {
        boolean anonima = esAnonima(resenia);
        return ReseniaPublicaResumenDTO.builder()
            .idResenia(resenia.getIdResenia())
            .calificacionGeneral(version != null
                ? version.getCalificacionGeneral()
                : 0)
            .descripcion(version != null ? version.getDescripcion() : null)
            .fechaVersion(version != null ? version.getFechaVersion() : null)
            .autorMostrado(anonima
                ? AUTOR_ANONIMO
                : nombreMostrado(resenia.getAutor()))
            .anonima(anonima)
            .build();
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private boolean esAnonima(Resenia resenia) {
        if (!resenia.isMostrarNombre()) {
            return true;
        }
        return resenia.getPedido().getServicio() != null
            && resenia.getPedido().getServicio().isContenidoSensible();
    }

    private String nombreMostrado(Usuario usuario) {
        if (usuario == null) {
            return AUTOR_ANONIMO;
        }
        return usuario.getNombreDisplay() != null
            && !usuario.getNombreDisplay().isBlank()
            ? usuario.getNombreDisplay()
            : usuario.getNombreUsuario();
    }

    private Pageable pagina(int pagina, int tamanio) {
        return PageRequest.of(
            Math.max(0, pagina),
            Math.min(Math.max(1, tamanio), TAMANIO_MAXIMO));
    }

    private String normalizarOrden(String orden) {
        if (orden == null || orden.isBlank()) {
            return ORDEN_RECIENTES;
        }
        String limpio = orden.trim().toUpperCase();
        if (ORDEN_MEJOR.equals(limpio) || ORDEN_PEOR.equals(limpio)) {
            return limpio;
        }
        if (ORDEN_RECIENTES.equals(limpio)) {
            return ORDEN_RECIENTES;
        }
        throw new IllegalArgumentException("Orden de reseñas inválido: " + orden);
    }

    private void validarCalificacion(Integer calificacion) {
        if (calificacion != null && (calificacion < 1 || calificacion > 5)) {
            throw new IllegalArgumentException(
                "La calificación debe ser de 1 a 5 estrellas.");
        }
    }

    private BigDecimal decimal(Double valor) {
        return valor == null
            ? null
            : BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal porcentaje(long cantidad, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(cantidad)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
}
