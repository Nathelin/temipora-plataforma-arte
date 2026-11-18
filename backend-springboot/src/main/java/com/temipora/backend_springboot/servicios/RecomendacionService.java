package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.ConfiguracionRecomendacion;
import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.PromedioReseniasDTO;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.ServicioEtiqueta;
import com.temipora.backend_springboot.repositorios.RecomendacionRepository;
import com.temipora.backend_springboot.repositorios.VisitaArtistaRepository;
import com.temipora.backend_springboot.repositorios.VisitaServicioRepository;

/**
 * Ranking explicable de servicios para un usuario.
 *
 * No hay machine learning: se combinan señales que ya existen en la plataforma
 * (favoritos, pedidos, visitas y etiquetas) y cada sugerencia devuelve el
 * motivo por el que aparece. Los pesos viven en
 * {@code configuracion_recomendacion} para poder calibrarlos sin recompilar.
 */
@Service
public class RecomendacionService {

    private static final double PESO_SIMILITUD_ETIQUETAS = 0.7;
    private static final double PESO_SIMILITUD_CATEGORIAS = 0.3;
    private static final double PRIOR_RATING = 4.0;
    private static final double PESO_PRIOR_RATING = 5.0;

    private final RecomendacionRepository recomendacionRepository;
    private final VisitaArtistaRepository visitaArtistaRepository;
    private final VisitaServicioRepository visitaServicioRepository;
    private final ReseniaPublicaService reseniaPublicaService;
    private final ConversionMonedaService conversionMonedaService;
    private final ConfiguracionRecomendacionService configuracionService;

    public RecomendacionService(
            RecomendacionRepository recomendacionRepository,
            VisitaArtistaRepository visitaArtistaRepository,
            VisitaServicioRepository visitaServicioRepository,
            ReseniaPublicaService reseniaPublicaService,
            ConversionMonedaService conversionMonedaService,
            ConfiguracionRecomendacionService configuracionService) {
        this.recomendacionRepository = recomendacionRepository;
        this.visitaArtistaRepository = visitaArtistaRepository;
        this.visitaServicioRepository = visitaServicioRepository;
        this.reseniaPublicaService = reseniaPublicaService;
        this.conversionMonedaService = conversionMonedaService;
        this.configuracionService = configuracionService;
    }

    public record ServicioPuntuado(Servicio servicio, double score, String motivo) {
    }

    public record ResultadoRanking(List<ServicioPuntuado> items, boolean personalizado) {
    }

    /**
     * Pesos de la similitud entre un servicio y otro. Se parametrizan para
     * que el proceso de sustituciones pueda priorizar a los artistas que el
     * cliente sigue sin duplicar la fórmula.
     */
    public record PesosSimilitud(
            double etiquetas,
            double categorias,
            double tipo,
            double medio,
            double precio,
            double rating,
            double afinidad,
            double bonusFavorito) {

        public static final PesosSimilitud POR_DEFECTO = new PesosSimilitud(
            0.40, 0.15, 0.15, 0.10, 0.10, 0.05, 0.05, 0);

        /**
         * Pesos del proceso de sustituciones: lo que más importa es que el
         * servicio sea del mismo tipo, con un precio parecido y etiquetas
         * similares. El resto del puntaje queda como desempate.
         */
        public static final PesosSimilitud SUSTITUCION = new PesosSimilitud(
            0.35, 0.05, 0.30, 0.03, 0.25, 0.02, 0, 0);

        public static PesosSimilitud conPesoFavorito(double peso) {
            return new PesosSimilitud(
                SUSTITUCION.etiquetas,
                SUSTITUCION.categorias,
                SUSTITUCION.tipo,
                SUSTITUCION.medio,
                SUSTITUCION.precio,
                SUSTITUCION.rating,
                SUSTITUCION.afinidad,
                peso);
        }
    }

    public record OpcionesSimilitud(
            PesosSimilitud pesos,
            int limite,
            int maxPorArtista,
            double factorTiempoMax) {

        public static OpcionesSimilitud porDefecto(int limite, int maxPorArtista) {
            return new OpcionesSimilitud(
                PesosSimilitud.POR_DEFECTO, limite, maxPorArtista, 0);
        }
    }

    private record Senales(
            Set<Long> artistasFavoritos,
            Set<Long> serviciosFavoritos,
            Set<Long> artistasComprados,
            Set<Long> etiquetasInteres,
            Set<Long> categoriasInteres,
            Map<Long, Double> coCompra,
            Map<Long, Double> coVisita,
            Map<Long, LocalDateTime> ultimaInteraccion) {

        boolean vacias() {
            return artistasFavoritos.isEmpty()
                && serviciosFavoritos.isEmpty()
                && artistasComprados.isEmpty()
                && etiquetasInteres.isEmpty()
                && coCompra.isEmpty()
                && coVisita.isEmpty();
        }
    }

    @Transactional(readOnly = true)
    public ResultadoRanking rankear(
            List<Servicio> candidatos,
            Long idUsuario,
            Moneda monedaDestino) {
        if (candidatos == null || candidatos.isEmpty()) {
            return new ResultadoRanking(List.of(), false);
        }

        List<Servicio> ordenBase = new ArrayList<>(candidatos);
        ordenBase.sort(Comparator.comparing(
            Servicio::getNombre, String.CASE_INSENSITIVE_ORDER));

        if (idUsuario == null) {
            return new ResultadoRanking(sinPuntaje(ordenBase), false);
        }

        ConfiguracionRecomendacion config = configuracionService.obtener();
        Senales senales = cargarSenales(idUsuario, config);
        if (senales.vacias()) {
            return new ResultadoRanking(sinPuntaje(ordenBase), false);
        }

        Map<Long, PromedioReseniasDTO> promedios =
            reseniaPublicaService.promediosPorServicios(
                ordenBase.stream().map(Servicio::getIdServicio).toList());

        List<ServicioPuntuado> puntuados = new ArrayList<>();
        for (Servicio servicio : ordenBase) {
            Long idArtista = servicio.getArtista() != null
                ? servicio.getArtista().getIdUsuario()
                : null;

            Set<Long> tags = tagsDe(servicio);
            Set<Long> categorias = categoriasDe(servicio);

            double favoritoArtista = senales.artistasFavoritos().contains(idArtista) ? 1 : 0;
            double favoritoServicio =
                senales.serviciosFavoritos().contains(servicio.getIdServicio()) ? 1 : 0;
            double historial = senales.artistasComprados().contains(idArtista) ? 1 : 0;
            double coCompra = idArtista != null
                ? senales.coCompra().getOrDefault(idArtista, 0.0)
                : 0;
            double coVisita = idArtista != null
                ? senales.coVisita().getOrDefault(idArtista, 0.0)
                : 0;
            double etiquetas = PESO_SIMILITUD_ETIQUETAS
                * jaccard(tags, senales.etiquetasInteres())
                + PESO_SIMILITUD_CATEGORIAS
                * jaccard(categorias, senales.categoriasInteres());
            double rating = ratingScore(promedios.get(servicio.getIdServicio()));
            double recencia = recencia(
                idArtista != null ? senales.ultimaInteraccion().get(idArtista) : null,
                config.getDiasDecaimiento());
            double precio = 0.5;

            double score = peso(config.getPesoFavoritoArtista()) * favoritoArtista
                + peso(config.getPesoFavoritoServicio()) * favoritoServicio
                + peso(config.getPesoHistorial()) * historial
                + peso(config.getPesoCoCompra()) * coCompra
                + peso(config.getPesoCoVisita()) * coVisita
                + peso(config.getPesoEtiquetas()) * etiquetas
                + peso(config.getPesoRating()) * rating
                + peso(config.getPesoRecencia()) * recencia
                + peso(config.getPesoPrecio()) * precio;

            puntuados.add(new ServicioPuntuado(
                servicio,
                score,
                motivo(favoritoArtista, favoritoServicio, historial,
                    coCompra, coVisita, etiquetas, rating)));
        }

        puntuados.sort(Comparator.comparingDouble(ServicioPuntuado::score).reversed());
        return new ResultadoRanking(
            aplicarDiversidad(puntuados, config, monedaDestino), true);
    }

    /**
     * Fallback para usuarios sin señales (sin favoritos, compras ni visitas):
     * ordena por calidad percibida y aplica la misma diversidad de precio.
     */
    @Transactional(readOnly = true)
    public List<ServicioPuntuado> rankearPorCalidad(
            List<Servicio> candidatos,
            Moneda monedaDestino) {
        if (candidatos == null || candidatos.isEmpty()) {
            return List.of();
        }
        ConfiguracionRecomendacion config = configuracionService.obtener();
        Map<Long, PromedioReseniasDTO> promedios =
            reseniaPublicaService.promediosPorServicios(
                candidatos.stream().map(Servicio::getIdServicio).toList());

        List<ServicioPuntuado> puntuados = new ArrayList<>();
        for (Servicio servicio : candidatos) {
            puntuados.add(new ServicioPuntuado(
                servicio,
                ratingScore(promedios.get(servicio.getIdServicio())),
                "Propuesta para retomar"));
        }
        puntuados.sort(Comparator.comparingDouble(ServicioPuntuado::score).reversed());
        return aplicarDiversidad(puntuados, config, monedaDestino);
    }

    @Transactional(readOnly = true)
    public List<ServicioPuntuado> similares(
            Servicio origen,
            List<Servicio> candidatos,
            Long idUsuario,
            Moneda monedaDestino) {
        ConfiguracionRecomendacion config = configuracionService.obtener();
        return similares(origen, candidatos, idUsuario, monedaDestino,
            OpcionesSimilitud.porDefecto(
                config.getMaxSugerencias(), config.getMaxPorArtista()));
    }

    @Transactional(readOnly = true)
    public List<ServicioPuntuado> similares(
            Servicio origen,
            List<Servicio> candidatos,
            Long idUsuario,
            Moneda monedaDestino,
            OpcionesSimilitud opciones) {
        if (origen == null || candidatos == null || candidatos.isEmpty()) {
            return List.of();
        }

        ConfiguracionRecomendacion config = configuracionService.obtener();
        OpcionesSimilitud efectivas = opciones != null
            ? opciones
            : OpcionesSimilitud.porDefecto(
                config.getMaxSugerencias(), config.getMaxPorArtista());
        PesosSimilitud pesos = efectivas.pesos() != null
            ? efectivas.pesos()
            : PesosSimilitud.POR_DEFECTO;
        Set<Long> tagsOrigen = tagsDe(origen);
        Set<Long> categoriasOrigen = categoriasDe(origen);
        BigDecimal precioOrigen = precioConvertido(origen, monedaDestino);
        Long idArtistaOrigen = origen.getArtista() != null
            ? origen.getArtista().getIdUsuario()
            : null;

        Set<Long> favoritos = Set.of();
        Set<Long> visitados = Set.of();
        if (idUsuario != null) {
            favoritos = new HashSet<>(safe(recomendacionRepository.artistasFavoritos(idUsuario)));
            LocalDate desde = LocalDate.now()
                .minusDays(Math.max(1, config.getDiasVisitas()));
            visitados = new HashSet<>();
            for (Object[] fila : visitaArtistaRepository.resumenPorUsuario(idUsuario, desde)) {
                visitados.add(numero(fila[0]));
            }
        }

        Map<Long, PromedioReseniasDTO> promedios =
            reseniaPublicaService.promediosPorServicios(
                candidatos.stream().map(Servicio::getIdServicio).toList());

        List<ServicioPuntuado> puntuados = new ArrayList<>();
        for (Servicio servicio : candidatos) {
            if (servicio == null
                    || Objects.equals(servicio.getIdServicio(), origen.getIdServicio())) {
                continue;
            }
            Long idArtista = servicio.getArtista() != null
                ? servicio.getArtista().getIdUsuario()
                : null;
            if (idArtista != null && idArtista.equals(idArtistaOrigen)) {
                continue;
            }
            if (!respetaTiempoEntrega(origen, servicio, efectivas.factorTiempoMax())) {
                continue;
            }

            boolean esFavorito = idArtista != null && favoritos.contains(idArtista);
            boolean visitado = idArtista != null && visitados.contains(idArtista);

            double tagSim = jaccard(tagsDe(servicio), tagsOrigen);
            double categoriaSim = jaccard(categoriasDe(servicio), categoriasOrigen);
            double tipoSim = mismo(servicio.getTipoServicio(), origen.getTipoServicio()) ? 1 : 0;
            double medioSim = mismo(servicio.getMedio(), origen.getMedio()) ? 1 : 0;
            double precioSim = precioSim(
                precioOrigen,
                precioConvertido(servicio, monedaDestino),
                config.getRangoPrecioPct());
            double rating = ratingScore(promedios.get(servicio.getIdServicio()));
            double usuario = (esFavorito ? 0.5 : 0) + (visitado ? 0.5 : 0);

            double base = pesos.etiquetas() * tagSim
                + pesos.categorias() * categoriaSim
                + pesos.tipo() * tipoSim
                + pesos.medio() * medioSim
                + pesos.precio() * precioSim
                + pesos.rating() * rating
                + pesos.afinidad() * usuario;
            // El peso de favoritos es una porción del puntaje total: el resto
            // se reparte entre las señales de parecido, así nunca lo supera.
            double pesoFavorito = Math.max(0,
                Math.min(1, pesos.bonusFavorito()));
            double score = (1 - pesoFavorito) * base
                + pesoFavorito * (esFavorito ? 1 : 0);

            puntuados.add(new ServicioPuntuado(
                servicio, score, motivoSimilar(tagSim, precioSim, tipoSim, esFavorito)));
        }

        puntuados.sort(Comparator.comparingDouble(ServicioPuntuado::score).reversed());

        List<ServicioPuntuado> resultado = new ArrayList<>();
        Map<Long, Integer> porArtista = new HashMap<>();
        int limite = Math.max(1, efectivas.limite());
        int maxPorArtista = Math.max(1, efectivas.maxPorArtista());
        for (ServicioPuntuado item : puntuados) {
            Long idArtista = item.servicio().getArtista() != null
                ? item.servicio().getArtista().getIdUsuario()
                : null;
            int usados = idArtista != null
                ? porArtista.getOrDefault(idArtista, 0)
                : 0;
            if (idArtista != null && usados >= maxPorArtista) {
                continue;
            }
            resultado.add(item);
            if (idArtista != null) {
                porArtista.put(idArtista, usados + 1);
            }
            if (resultado.size() >= limite) {
                break;
            }
        }
        return resultado;
    }

    private Senales cargarSenales(Long idUsuario, ConfiguracionRecomendacion config) {
        LocalDate desde = LocalDate.now()
            .minusDays(Math.max(1, config.getDiasVisitas()));

        Set<Long> favoritosArtista = new HashSet<>(
            safe(recomendacionRepository.artistasFavoritos(idUsuario)));
        Set<Long> favoritosServicio = new HashSet<>(
            safe(recomendacionRepository.serviciosFavoritos(idUsuario)));
        Set<Long> comprados = new HashSet<>(
            safe(recomendacionRepository.artistasComprados(idUsuario)));

        Set<Long> etiquetas = new HashSet<>();
        etiquetas.addAll(safe(recomendacionRepository.etiquetasCompradas(idUsuario)));
        etiquetas.addAll(safe(recomendacionRepository.etiquetasFavoritas(idUsuario)));
        etiquetas.addAll(safe(visitaServicioRepository.etiquetasVisitadas(idUsuario, desde)));

        Set<Long> categorias = etiquetas.isEmpty()
            ? Set.of()
            : new HashSet<>(safe(
                recomendacionRepository.categoriasDeEtiquetas(etiquetas)));

        Map<Long, Long> coCompraCrudo = new HashMap<>();
        for (Object[] fila : recomendacionRepository.coCompra(idUsuario)) {
            coCompraCrudo.put(numero(fila[0]), ((Number) fila[1]).longValue());
        }
        Map<Long, Long> popularidadCompra = new HashMap<>();
        if (!coCompraCrudo.isEmpty()) {
            for (Object[] fila : recomendacionRepository.popularidadCompra(
                    coCompraCrudo.keySet())) {
                popularidadCompra.put(numero(fila[0]), ((Number) fila[1]).longValue());
            }
        }

        Map<Long, Long> coVisitaCrudo = new HashMap<>();
        for (Object[] fila : visitaArtistaRepository.coVisitas(idUsuario, desde)) {
            coVisitaCrudo.put(numero(fila[0]), ((Number) fila[1]).longValue());
        }
        Map<Long, Long> popularidadVisita = new HashMap<>();
        if (!coVisitaCrudo.isEmpty()) {
            for (Object[] fila : visitaArtistaRepository.popularidad(
                    coVisitaCrudo.keySet())) {
                popularidadVisita.put(numero(fila[0]), ((Number) fila[1]).longValue());
            }
        }

        Map<Long, LocalDateTime> ultimaInteraccion = new HashMap<>();
        for (Object[] fila : visitaArtistaRepository.resumenPorUsuario(idUsuario, desde)) {
            actualizarUltima(ultimaInteraccion, numero(fila[0]), aFecha(fila[2]));
        }
        for (Object[] fila : recomendacionRepository.ultimaCompraPorArtista(idUsuario)) {
            actualizarUltima(ultimaInteraccion, numero(fila[0]), aFecha(fila[1]));
        }
        for (Object[] fila : recomendacionRepository.ultimoFavoritoArtista(idUsuario)) {
            actualizarUltima(ultimaInteraccion, numero(fila[0]), aFecha(fila[1]));
        }

        return new Senales(
            favoritosArtista,
            favoritosServicio,
            comprados,
            etiquetas,
            categorias,
            normalizar(coCompraCrudo, popularidadCompra),
            normalizar(coVisitaCrudo, popularidadVisita),
            ultimaInteraccion);
    }

    private List<ServicioPuntuado> aplicarDiversidad(
            List<ServicioPuntuado> puntuados,
            ConfiguracionRecomendacion config,
            Moneda monedaDestino) {
        if (puntuados.size() <= 1) {
            return puntuados;
        }
        int limite = Math.min(
            Math.max(1, config.getMaxSugerencias()), puntuados.size());
        int maxPorArtista = Math.max(1, config.getMaxPorArtista());

        List<ServicioPuntuado> seleccionados = new ArrayList<>();
        List<ServicioPuntuado> resto = new ArrayList<>();
        Map<Long, Integer> porArtista = new HashMap<>();

        for (ServicioPuntuado item : puntuados) {
            Long idArtista = item.servicio().getArtista() != null
                ? item.servicio().getArtista().getIdUsuario()
                : null;
            int usados = idArtista != null
                ? porArtista.getOrDefault(idArtista, 0)
                : 0;
            boolean entra = seleccionados.size() < limite
                && (idArtista == null || usados < maxPorArtista);
            if (entra) {
                seleccionados.add(item);
                if (idArtista != null) {
                    porArtista.put(idArtista, usados + 1);
                }
            } else {
                resto.add(item);
            }
        }

        List<ServicioPuntuado> resultado = diversificarPorPrecio(
            seleccionados, config, monedaDestino);
        resultado.addAll(resto);
        return resultado;
    }

    private List<ServicioPuntuado> diversificarPorPrecio(
            List<ServicioPuntuado> seleccionados,
            ConfiguracionRecomendacion config,
            Moneda monedaDestino) {
        if (seleccionados.size() <= 1) {
            return new ArrayList<>(seleccionados);
        }
        int minimoFranjas = Math.min(3, Math.max(0, config.getMinFranjasPrecio()));
        if (minimoFranjas <= 1) {
            return new ArrayList<>(seleccionados);
        }

        Map<Long, BigDecimal> precios = new HashMap<>();
        for (ServicioPuntuado item : seleccionados) {
            precios.put(
                item.servicio().getIdServicio(),
                precioConvertido(item.servicio(), monedaDestino));
        }
        List<BigDecimal> valores = precios.values().stream()
            .filter(Objects::nonNull)
            .sorted()
            .toList();
        if (valores.size() < minimoFranjas) {
            return new ArrayList<>(seleccionados);
        }

        BigDecimal corte33 = valores.get((int) Math.floor(valores.size() * 0.33));
        BigDecimal corte66 = valores.get((int) Math.floor(valores.size() * 0.66));

        List<ServicioPuntuado> resultado = new ArrayList<>();
        Set<Long> incluidos = new HashSet<>();
        Set<Integer> franjas = new HashSet<>();

        int iniciales = Math.min(2, seleccionados.size());
        for (int i = 0; i < iniciales; i++) {
            ServicioPuntuado item = seleccionados.get(i);
            resultado.add(item);
            incluidos.add(item.servicio().getIdServicio());
            franjas.add(franja(item, precios, corte33, corte66));
        }

        for (int numeroFranja = 0;
                numeroFranja < 3 && franjas.size() < minimoFranjas;
                numeroFranja++) {
            if (franjas.contains(numeroFranja)) {
                continue;
            }
            for (ServicioPuntuado item : seleccionados) {
                if (incluidos.contains(item.servicio().getIdServicio())) {
                    continue;
                }
                if (franja(item, precios, corte33, corte66) != numeroFranja) {
                    continue;
                }
                resultado.add(item);
                incluidos.add(item.servicio().getIdServicio());
                franjas.add(numeroFranja);
                break;
            }
        }

        for (ServicioPuntuado item : seleccionados) {
            if (incluidos.add(item.servicio().getIdServicio())) {
                resultado.add(item);
            }
        }
        return resultado;
    }

    private int franja(
            ServicioPuntuado item,
            Map<Long, BigDecimal> precios,
            BigDecimal corte33,
            BigDecimal corte66) {
        BigDecimal precio = precios.get(item.servicio().getIdServicio());
        if (precio == null) {
            return -1;
        }
        if (precio.compareTo(corte33) <= 0) {
            return 0;
        }
        if (precio.compareTo(corte66) <= 0) {
            return 1;
        }
        return 2;
    }

    private List<ServicioPuntuado> sinPuntaje(List<Servicio> servicios) {
        return servicios.stream()
            .map(s -> new ServicioPuntuado(s, 0, null))
            .toList();
    }

    private Set<Long> tagsDe(Servicio servicio) {
        if (servicio.getEtiquetas() == null || servicio.getEtiquetas().isEmpty()) {
            return Set.of();
        }
        Set<Long> tags = new HashSet<>();
        for (ServicioEtiqueta asignacion : servicio.getEtiquetas()) {
            Etiqueta etiqueta = asignacion != null ? asignacion.getEtiqueta() : null;
            if (etiqueta != null && etiqueta.getIdEtiqueta() != null) {
                tags.add(etiqueta.getIdEtiqueta());
            }
        }
        return tags;
    }

    private Set<Long> categoriasDe(Servicio servicio) {
        if (servicio.getEtiquetas() == null || servicio.getEtiquetas().isEmpty()) {
            return Set.of();
        }
        Set<Long> categorias = new HashSet<>();
        for (ServicioEtiqueta asignacion : servicio.getEtiquetas()) {
            Etiqueta etiqueta = asignacion != null ? asignacion.getEtiqueta() : null;
            if (etiqueta != null && etiqueta.getCategoria() != null
                    && etiqueta.getCategoria().getIdCategoriaEtiqueta() != null) {
                categorias.add(etiqueta.getCategoria().getIdCategoriaEtiqueta());
            }
        }
        return categorias;
    }

    private double jaccard(Set<Long> a, Set<Long> b) {
        return RankingSoporte.jaccard(a, b);
    }

    private double ratingScore(PromedioReseniasDTO promedio) {
        if (promedio == null || promedio.getCantidad() <= 0
                || promedio.getPromedio() == null) {
            return 0.5;
        }
        double cantidad = promedio.getCantidad();
        double bayesiano = (promedio.getPromedio().doubleValue() * cantidad
            + PRIOR_RATING * PESO_PRIOR_RATING)
            / (cantidad + PESO_PRIOR_RATING);
        return Math.max(0, Math.min(1, bayesiano / 5.0));
    }

    private double recencia(LocalDateTime ultima, int diasDecaimiento) {
        return RankingSoporte.recencia(ultima, diasDecaimiento);
    }

    private double precioSim(
            BigDecimal origen,
            BigDecimal candidato,
            BigDecimal rangoPct) {
        return RankingSoporte.precioSim(origen, candidato, rangoPct);
    }

    private BigDecimal precioConvertido(Servicio servicio, Moneda monedaDestino) {
        if (servicio == null || servicio.getPrecioBase() == null) {
            return null;
        }
        BigDecimal factor = conversionMonedaService.factor(
            servicio.getMoneda(), monedaDestino, LocalDateTime.now());
        if (factor == null) {
            return servicio.getPrecioBase();
        }
        return servicio.getPrecioBase().multiply(factor);
    }

    private boolean mismo(Object a, Object b) {
        return a != null && b != null && (a == b || Objects.equals(a, b));
    }

    private String motivo(
            double favoritoArtista,
            double favoritoServicio,
            double historial,
            double coCompra,
            double coVisita,
            double etiquetas,
            double rating) {
        if (favoritoArtista > 0) {
            return "Artista que seguís";
        }
        if (favoritoServicio > 0) {
            return "Servicio que guardaste";
        }
        if (historial > 0) {
            return "Ya trabajaste con este artista";
        }
        if (coCompra >= 0.15) {
            return "Elegido por clientes como vos";
        }
        if (coVisita >= 0.15) {
            return "Popular entre quienes vieron lo mismo";
        }
        if (etiquetas >= 0.25) {
            return "Estilo parecido a lo que te gusta";
        }
        if (rating >= 0.8) {
            return "Buenas reseñas";
        }
        return "Tiene cupos disponibles";
    }

    private boolean respetaTiempoEntrega(
            Servicio origen,
            Servicio candidato,
            double factorTiempoMax) {
        if (factorTiempoMax <= 1) {
            return true;
        }
        Integer diasOrigen = origen.getTiempoEntregaDias();
        Integer diasCandidato = candidato.getTiempoEntregaDias();
        if (diasOrigen == null || diasCandidato == null || diasOrigen <= 0) {
            return true;
        }
        return diasCandidato <= Math.ceil(diasOrigen * factorTiempoMax);
    }

    private String motivoSimilar(
            double tagSim,
            double precioSim,
            double tipoSim,
            boolean esFavorito) {
        if (esFavorito) {
            return "Artista que seguís";
        }
        if (tipoSim > 0) {
            return "Mismo tipo de servicio";
        }
        if (tagSim >= 0.30) {
            return "Mismo estilo";
        }
        if (precioSim >= 0.60) {
            return "Precio similar";
        }
        return "Tiene cupos disponibles";
    }

    private Map<Long, Double> normalizar(
            Map<Long, Long> crudo,
            Map<Long, Long> popularidad) {
        return RankingSoporte.normalizar(crudo, popularidad);
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

    private double peso(BigDecimal valor) {
        return valor != null ? valor.doubleValue() : 0;
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
