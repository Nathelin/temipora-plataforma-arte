package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

import com.temipora.backend_springboot.modelo.ConfiguracionRecomendacionProducto;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoEtiqueta;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.TipoProducto;
import com.temipora.backend_springboot.repositorios.MonedaRepository;
import com.temipora.backend_springboot.repositorios.RecomendacionProductoRepository;
import com.temipora.backend_springboot.repositorios.VisitaProductoRepository;

/**
 * Ranking explicable de productos para un usuario.
 *
 * Sigue el mismo enfoque que el recomendador de servicios: no hay machine
 * learning, se combinan señales reales (favoritos, compras, visitas y
 * etiquetas) y cada sugerencia informa el motivo por el que aparece. Los pesos
 * viven en {@code configuracion_recomendacion_producto} para calibrarlos sin
 * recompilar.
 */
@Service
public class RecomendacionProductoService {

    private static final double PESO_SIMILITUD_ETIQUETAS = 0.7;
    private static final double PESO_SIMILITUD_CATEGORIAS = 0.3;

    private final RecomendacionProductoRepository recomendacionRepository;
    private final VisitaProductoRepository visitaProductoRepository;
    private final InteresUsuarioService interesService;
    private final ConversionMonedaService conversionMonedaService;
    private final MonedaRepository monedaRepository;
    private final ConfiguracionRecomendacionProductoService configuracionService;

    public RecomendacionProductoService(
            RecomendacionProductoRepository recomendacionRepository,
            VisitaProductoRepository visitaProductoRepository,
            InteresUsuarioService interesService,
            ConversionMonedaService conversionMonedaService,
            MonedaRepository monedaRepository,
            ConfiguracionRecomendacionProductoService configuracionService) {
        this.recomendacionRepository = recomendacionRepository;
        this.visitaProductoRepository = visitaProductoRepository;
        this.interesService = interesService;
        this.conversionMonedaService = conversionMonedaService;
        this.monedaRepository = monedaRepository;
        this.configuracionService = configuracionService;
    }

    public record ProductoPuntuado(Producto producto, double score, String motivo) {
    }

    public record ResultadoRankingProducto(
            List<ProductoPuntuado> items,
            boolean personalizado) {
    }

    /**
     * Pesos de la similitud producto→producto. "Calidad" reemplaza al rating:
     * hoy el catálogo no tiene reseñas, así que se usa disponibilidad real.
     */
    public record PesosSimilitudProducto(
            double etiquetas,
            double categorias,
            double tipo,
            double medio,
            double precio,
            double calidad,
            double afinidad) {

        public static final PesosSimilitudProducto POR_DEFECTO =
            new PesosSimilitudProducto(
                0.30, 0.08, 0.30, 0.05, 0.20, 0.04, 0.03);
    }

    public record OpcionesSimilitudProducto(
            PesosSimilitudProducto pesos,
            int limite,
            int maxPorArtista) {

        public static OpcionesSimilitudProducto porDefecto(
                int limite, int maxPorArtista) {
            return new OpcionesSimilitudProducto(
                PesosSimilitudProducto.POR_DEFECTO, limite, maxPorArtista);
        }
    }

    private record Senales(
            Set<Long> productosFavoritos,
            Map<Long, Double> coCompra,
            Map<Long, Double> coVisita,
            BigDecimal precioPromedio) {

        boolean vacias() {
            return productosFavoritos.isEmpty()
                && coCompra.isEmpty()
                && coVisita.isEmpty()
                && precioPromedio == null;
        }
    }

    @Transactional(readOnly = true)
    public ResultadoRankingProducto rankear(
            List<Producto> candidatos,
            Long idUsuario,
            Moneda monedaDestino) {
        if (candidatos == null || candidatos.isEmpty()) {
            return new ResultadoRankingProducto(List.of(), false);
        }

        List<Producto> ordenBase = new ArrayList<>(candidatos);
        ordenBase.sort(Comparator.comparing(
            Producto::getTitulo, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        if (idUsuario == null) {
            return new ResultadoRankingProducto(sinPuntaje(ordenBase), false);
        }

        ConfiguracionRecomendacionProducto config = configuracionService.obtener();
        InteresUsuarioService.InteresContenido interes =
            interesService.cargar(idUsuario, config.getDiasVisitas());
        Senales senales = cargarSenales(idUsuario, config, monedaDestino);
        if (interes.vacio() && senales.vacias()) {
            return new ResultadoRankingProducto(sinPuntaje(ordenBase), false);
        }

        List<ProductoPuntuado> puntuados = new ArrayList<>();
        for (Producto producto : ordenBase) {
            Long idArtista = idArtista(producto);
            Set<Long> tags = tagsDe(producto);
            Set<Long> categorias = categoriasDe(producto);

            double favoritoProducto =
                senales.productosFavoritos().contains(producto.getIdProducto()) ? 1 : 0;
            double favoritoArtista = idArtista != null
                && interes.artistasFavoritos().contains(idArtista) ? 1 : 0;
            double historial = idArtista != null
                && interes.artistasComprados().contains(idArtista) ? 1 : 0;
            double coCompra = senales.coCompra()
                .getOrDefault(producto.getIdProducto(), 0.0);
            double coVisita = senales.coVisita()
                .getOrDefault(producto.getIdProducto(), 0.0);
            double etiquetas = PESO_SIMILITUD_ETIQUETAS
                * RankingSoporte.jaccard(tags, interes.etiquetas())
                + PESO_SIMILITUD_CATEGORIAS
                * RankingSoporte.jaccard(categorias, interes.categorias());
            boolean conStock = tieneStock(producto);
            double calidad = conStock ? 1 : 0;
            double precio = RankingSoporte.precioSim(
                senales.precioPromedio(),
                precioDesde(producto, monedaDestino),
                config.getRangoPrecioPct());
            double recencia = RankingSoporte.recencia(
                ultimaInteraccion(producto, idArtista, interes),
                config.getDiasDecaimiento());

            double score = peso(config.getPesoFavoritoProducto()) * favoritoProducto
                + peso(config.getPesoFavoritoArtista()) * favoritoArtista
                + peso(config.getPesoHistorial()) * historial
                + peso(config.getPesoCoCompra()) * coCompra
                + peso(config.getPesoCoVisita()) * coVisita
                + peso(config.getPesoEtiquetas()) * etiquetas
                + peso(config.getPesoCalidad()) * calidad
                + peso(config.getPesoRecencia()) * recencia
                + peso(config.getPesoPrecio()) * precio;

            puntuados.add(new ProductoPuntuado(
                producto,
                score,
                motivo(favoritoProducto, favoritoArtista, historial,
                    coCompra, coVisita, etiquetas, precio, recencia, conStock)));
        }

        puntuados.sort(Comparator.comparingDouble(ProductoPuntuado::score).reversed());
        return new ResultadoRankingProducto(
            aplicarDiversidad(puntuados, config, monedaDestino), true);
    }

    @Transactional(readOnly = true)
    public List<ProductoPuntuado> similares(
            Producto origen,
            List<Producto> candidatos,
            Long idUsuario,
            Moneda monedaDestino) {
        ConfiguracionRecomendacionProducto config = configuracionService.obtener();
        return similares(origen, candidatos, idUsuario, monedaDestino,
            OpcionesSimilitudProducto.porDefecto(
                config.getMaxSugerencias(), config.getMaxPorArtista()));
    }

    @Transactional(readOnly = true)
    public List<ProductoPuntuado> similares(
            Producto origen,
            List<Producto> candidatos,
            Long idUsuario,
            Moneda monedaDestino,
            int limite) {
        ConfiguracionRecomendacionProducto config = configuracionService.obtener();
        return similares(origen, candidatos, idUsuario, monedaDestino,
            OpcionesSimilitudProducto.porDefecto(
                Math.max(1, limite), config.getMaxPorArtista()));
    }

    @Transactional(readOnly = true)
    public List<ProductoPuntuado> similares(
            Producto origen,
            List<Producto> candidatos,
            Long idUsuario,
            Moneda monedaDestino,
            OpcionesSimilitudProducto opciones) {
        if (origen == null || candidatos == null || candidatos.isEmpty()) {
            return List.of();
        }

        ConfiguracionRecomendacionProducto config = configuracionService.obtener();
        OpcionesSimilitudProducto efectivas = opciones != null
            ? opciones
            : OpcionesSimilitudProducto.porDefecto(
                config.getMaxSugerencias(), config.getMaxPorArtista());
        PesosSimilitudProducto pesos = efectivas.pesos() != null
            ? efectivas.pesos()
            : PesosSimilitudProducto.POR_DEFECTO;

        Set<Long> tagsOrigen = tagsDe(origen);
        Set<Long> categoriasOrigen = categoriasDe(origen);
        BigDecimal precioOrigen = precioDesde(origen, monedaDestino);

        Set<Long> favoritos = Set.of();
        Set<Long> conocidos = Set.of();
        if (idUsuario != null) {
            InteresUsuarioService.InteresContenido interes =
                interesService.cargar(idUsuario, config.getDiasVisitas());
            favoritos = interes.artistasFavoritos();
            Set<Long> union = new HashSet<>(interes.artistasComprados());
            union.addAll(interes.artistasVisitados());
            conocidos = union;
        }

        List<ProductoPuntuado> puntuados = new ArrayList<>();
        for (Producto producto : candidatos) {
            if (producto == null
                    || Objects.equals(
                        producto.getIdProducto(), origen.getIdProducto())) {
                continue;
            }
            Long idArtista = idArtista(producto);
            boolean esFavorito = idArtista != null && favoritos.contains(idArtista);
            boolean conocido = idArtista != null && conocidos.contains(idArtista);

            double tagSim = RankingSoporte.jaccard(
                tagsDe(producto), tagsOrigen);
            double categoriaSim = RankingSoporte.jaccard(
                categoriasDe(producto), categoriasOrigen);
            double tipoSim = mismoTipo(
                producto.getTipoProducto(), origen.getTipoProducto()) ? 1 : 0;
            double medioSim = mismoMedio(
                producto.getMedio(), origen.getMedio()) ? 1 : 0;
            double precioSim = RankingSoporte.precioSim(
                precioOrigen,
                precioDesde(producto, monedaDestino),
                config.getRangoPrecioPct());
            boolean conStock = tieneStock(producto);
            double calidad = conStock ? 1 : 0;
            double afinidad = (esFavorito ? 0.5 : 0) + (conocido ? 0.5 : 0);

            double score = pesos.etiquetas() * tagSim
                + pesos.categorias() * categoriaSim
                + pesos.tipo() * tipoSim
                + pesos.medio() * medioSim
                + pesos.precio() * precioSim
                + pesos.calidad() * calidad
                + pesos.afinidad() * afinidad;

            puntuados.add(new ProductoPuntuado(
                producto,
                score,
                motivoSimilar(tagSim, precioSim, tipoSim, esFavorito, conStock)));
        }

        puntuados.sort(Comparator.comparingDouble(ProductoPuntuado::score).reversed());

        List<ProductoPuntuado> resultado = new ArrayList<>();
        Map<Long, Integer> porArtista = new HashMap<>();
        int limite = Math.max(1, efectivas.limite());
        int maxPorArtista = Math.max(1, efectivas.maxPorArtista());
        for (ProductoPuntuado item : puntuados) {
            Long idArtista = idArtista(item.producto());
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

    private Senales cargarSenales(
            Long idUsuario,
            ConfiguracionRecomendacionProducto config,
            Moneda monedaDestino) {
        LocalDate desde = LocalDate.now()
            .minusDays(Math.max(1, config.getDiasVisitas()));

        Set<Long> favoritos = new HashSet<>(
            safe(recomendacionRepository.productosFavoritos(idUsuario)));

        Map<Long, Long> coCompraCrudo = aMapa(
            recomendacionRepository.coCompra(idUsuario));
        Map<Long, Long> popularidadCompra = new HashMap<>();
        if (!coCompraCrudo.isEmpty()) {
            for (Object[] fila : safe(recomendacionRepository.popularidadCompra(
                    coCompraCrudo.keySet()))) {
                popularidadCompra.put(numero(fila[0]), ((Number) fila[1]).longValue());
            }
        }

        Map<Long, Long> coVisitaCrudo = aMapa(
            visitaProductoRepository.coVisitas(idUsuario, desde));
        Map<Long, Long> popularidadVisita = new HashMap<>();
        if (!coVisitaCrudo.isEmpty()) {
            for (Object[] fila : safe(visitaProductoRepository.popularidad(
                    coVisitaCrudo.keySet()))) {
                popularidadVisita.put(numero(fila[0]), ((Number) fila[1]).longValue());
            }
        }

        return new Senales(
            favoritos,
            RankingSoporte.normalizar(coCompraCrudo, popularidadCompra),
            RankingSoporte.normalizar(coVisitaCrudo, popularidadVisita),
            precioPromedioComprado(idUsuario, monedaDestino));
    }

    private List<ProductoPuntuado> aplicarDiversidad(
            List<ProductoPuntuado> puntuados,
            ConfiguracionRecomendacionProducto config,
            Moneda monedaDestino) {
        if (puntuados.size() <= 1) {
            return puntuados;
        }
        int limite = Math.min(
            Math.max(1, config.getMaxSugerencias()), puntuados.size());
        int maxPorArtista = Math.max(1, config.getMaxPorArtista());

        List<ProductoPuntuado> seleccionados = new ArrayList<>();
        List<ProductoPuntuado> resto = new ArrayList<>();
        Map<Long, Integer> porArtista = new HashMap<>();

        for (ProductoPuntuado item : puntuados) {
            Long idArtista = idArtista(item.producto());
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

        List<ProductoPuntuado> resultado = RankingSoporte.diversificarPorPrecio(
            seleccionados,
            item -> precioDesde(item.producto(), monedaDestino),
            config.getMinFranjasPrecio());
        resultado.addAll(resto);
        return resultado;
    }

    private BigDecimal precioPromedioComprado(
            Long idUsuario,
            Moneda monedaDestino) {
        List<Object[]> filas = safe(
            recomendacionRepository.promedioPrecioComprado(idUsuario));
        if (filas.isEmpty()) {
            return null;
        }
        LocalDateTime ahora = LocalDateTime.now();
        BigDecimal suma = BigDecimal.ZERO;
        int cantidad = 0;
        for (Object[] fila : filas) {
            Long idMoneda = numero(fila[0]);
            BigDecimal promedio = aBigDecimal(fila[1]);
            if (promedio == null) {
                continue;
            }
            Moneda origen = idMoneda != null
                ? monedaRepository.findByIdMoneda(idMoneda).orElse(null)
                : null;
            BigDecimal factor = origen != null && monedaDestino != null
                ? conversionMonedaService.factor(origen, monedaDestino, ahora)
                : null;
            suma = suma.add(factor != null
                ? promedio.multiply(factor)
                : promedio);
            cantidad++;
        }
        return cantidad == 0
            ? null
            : suma.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
    }

    private LocalDateTime ultimaInteraccion(
            Producto producto,
            Long idArtista,
            InteresUsuarioService.InteresContenido interes) {
        LocalDateTime ultima = idArtista != null
            ? interes.ultimaInteraccion().get(idArtista)
            : null;
        if (ultima == null) {
            ultima = producto.getFechaCreacion();
        }
        return ultima;
    }

    private Set<Long> tagsDe(Producto producto) {
        if (producto.getEtiquetas() == null || producto.getEtiquetas().isEmpty()) {
            return Set.of();
        }
        Set<Long> tags = new HashSet<>();
        for (ProductoEtiqueta asignacion : producto.getEtiquetas()) {
            Etiqueta etiqueta = asignacion != null ? asignacion.getEtiqueta() : null;
            if (etiqueta != null && etiqueta.getIdEtiqueta() != null) {
                tags.add(etiqueta.getIdEtiqueta());
            }
        }
        return tags;
    }

    private Set<Long> categoriasDe(Producto producto) {
        if (producto.getEtiquetas() == null || producto.getEtiquetas().isEmpty()) {
            return Set.of();
        }
        Set<Long> categorias = new HashSet<>();
        for (ProductoEtiqueta asignacion : producto.getEtiquetas()) {
            Etiqueta etiqueta = asignacion != null ? asignacion.getEtiqueta() : null;
            if (etiqueta != null && etiqueta.getCategoria() != null
                    && etiqueta.getCategoria().getIdCategoriaEtiqueta() != null) {
                categorias.add(etiqueta.getCategoria().getIdCategoriaEtiqueta());
            }
        }
        return categorias;
    }

    private Long idArtista(Producto producto) {
        return producto != null && producto.getArtista() != null
            ? producto.getArtista().getIdUsuario()
            : null;
    }

    private boolean mismoTipo(TipoProducto a, TipoProducto b) {
        return a != null && b != null
            && a.getIdTipoProducto() != null
            && a.getIdTipoProducto().equals(b.getIdTipoProducto());
    }

    private boolean mismoMedio(Medio a, Medio b) {
        return a != null && b != null
            && a.getIdMedio() != null
            && a.getIdMedio().equals(b.getIdMedio());
    }

    private boolean tieneStock(Producto producto) {
        if (producto.getVariantes() == null) {
            return false;
        }
        return producto.getVariantes().stream()
            .anyMatch(ProductoVariante::estaDisponible);
    }

    private BigDecimal precioDesde(Producto producto, Moneda monedaDestino) {
        if (producto.getVariantes() == null) {
            return null;
        }
        BigDecimal minimo = producto.getVariantes().stream()
            .filter(variante -> variante.getEstado() != EstadoVariante.ARCHIVADO)
            .map(ProductoVariante::getPrecio)
            .filter(Objects::nonNull)
            .min(BigDecimal::compareTo)
            .orElse(null);
        if (minimo == null) {
            return null;
        }
        if (monedaDestino == null || producto.getMoneda() == null
                || Objects.equals(producto.getMoneda().getIdMoneda(),
                    monedaDestino.getIdMoneda())) {
            return minimo.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = conversionMonedaService.factor(
            producto.getMoneda(), monedaDestino, LocalDateTime.now());
        if (factor == null) {
            return minimo.setScale(2, RoundingMode.HALF_UP);
        }
        return minimo.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private List<ProductoPuntuado> sinPuntaje(List<Producto> productos) {
        return productos.stream()
            .map(producto -> new ProductoPuntuado(producto, 0, null))
            .toList();
    }

    private String motivo(
            double favoritoProducto,
            double favoritoArtista,
            double historial,
            double coCompra,
            double coVisita,
            double etiquetas,
            double precio,
            double recencia,
            boolean conStock) {
        if (favoritoProducto > 0) {
            return "Lo tenías en favoritos";
        }
        if (favoritoArtista > 0) {
            return "Artista que seguís";
        }
        if (historial > 0) {
            return "Ya compraste de este artista";
        }
        if (coCompra >= 0.15) {
            return "Quienes compraron lo que te llevaste también compraron esto";
        }
        if (coVisita >= 0.15) {
            return "Popular entre quienes vieron lo mismo";
        }
        if (etiquetas >= 0.25) {
            return "Estilo parecido a lo que te gusta";
        }
        if (precio >= 0.75) {
            return "Precio acorde a lo que comprás";
        }
        if (recencia >= 0.75) {
            return "Recién publicado";
        }
        if (conStock) {
            return "Stock disponible";
        }
        return "Recomendado para vos";
    }

    private String motivoSimilar(
            double tagSim,
            double precioSim,
            double tipoSim,
            boolean esFavorito,
            boolean conStock) {
        if (esFavorito) {
            return "Artista que seguís";
        }
        if (tipoSim > 0) {
            return "Mismo tipo de producto";
        }
        if (tagSim >= 0.30) {
            return "Mismo estilo";
        }
        if (precioSim >= 0.60) {
            return "Precio similar";
        }
        if (conStock) {
            return "Stock disponible";
        }
        return "Recomendado para vos";
    }

    private Map<Long, Long> aMapa(List<Object[]> filas) {
        Map<Long, Long> mapa = new HashMap<>();
        for (Object[] fila : safe(filas)) {
            if (fila == null || fila[0] == null || fila[1] == null) {
                continue;
            }
            mapa.put(numero(fila[0]), ((Number) fila[1]).longValue());
        }
        return mapa;
    }

    private BigDecimal aBigDecimal(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }
        if (valor instanceof Number numero) {
            return new BigDecimal(numero.toString());
        }
        return null;
    }

    private double peso(BigDecimal valor) {
        return valor != null ? valor.doubleValue() : 0;
    }

    private Long numero(Object valor) {
        return valor == null ? null : ((Number) valor).longValue();
    }

    private <T> List<T> safe(List<T> valores) {
        return valores != null ? valores : List.of();
    }
}
