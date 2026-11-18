package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.temipora.backend_springboot.modelo.CategoriaEtiqueta;
import com.temipora.backend_springboot.modelo.ConfiguracionRecomendacionProducto;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoEtiqueta;
import com.temipora.backend_springboot.modelo.ProductoEtiquetaId;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.TipoProducto;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.MonedaRepository;
import com.temipora.backend_springboot.repositorios.RecomendacionProductoRepository;
import com.temipora.backend_springboot.repositorios.VisitaProductoRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecomendacionProductoServiceTest {

    @Mock
    private RecomendacionProductoRepository recomendacionRepository;

    @Mock
    private VisitaProductoRepository visitaProductoRepository;

    @Mock
    private InteresUsuarioService interesService;

    @Mock
    private ConversionMonedaService conversionMonedaService;

    @Mock
    private MonedaRepository monedaRepository;

    @Mock
    private ConfiguracionRecomendacionProductoService configuracionService;

    @InjectMocks
    private RecomendacionProductoService recomendacionService;

    private Moneda ars;

    @BeforeEach
    void setUp() {
        ars = Moneda.builder().idMoneda(1L).codigo("ARS").nombre("Peso").build();

        ConfiguracionRecomendacionProducto config =
            new ConfiguracionRecomendacionProducto();
        config.setMinFranjasPrecio(0);
        when(configuracionService.obtener()).thenReturn(config);
        when(interesService.cargar(eq(1L), anyInt()))
            .thenReturn(interesVacio());
    }

    @Test
    void rankear_sinUsuarioMantieneOrdenAlfabeticoYSinPersonalizar() {
        Producto b = producto(2L, "Boceto", 20L, "200");
        Producto a = producto(1L, "Acuarela", 10L, "100");

        RecomendacionProductoService.ResultadoRankingProducto resultado =
            recomendacionService.rankear(List.of(b, a), null, ars);

        assertThat(resultado.personalizado()).isFalse();
        assertThat(resultado.items())
            .extracting(item -> item.producto().getTitulo())
            .containsExactly("Acuarela", "Boceto");
    }

    @Test
    void rankear_sinSenalesNoPersonaliza() {
        Producto a = producto(1L, "Acuarela", 10L, "100");

        RecomendacionProductoService.ResultadoRankingProducto resultado =
            recomendacionService.rankear(List.of(a), 1L, ars);

        assertThat(resultado.personalizado()).isFalse();
        assertThat(resultado.items()).hasSize(1);
        assertThat(resultado.items().get(0).motivo()).isNull();
    }

    @Test
    void rankear_priorizaElProductoFavorito() {
        when(recomendacionRepository.productosFavoritos(1L)).thenReturn(List.of(1L));

        Producto noFavorito = producto(2L, "Boceto", 20L, "200");
        Producto favorito = producto(1L, "Acuarela", 10L, "100");

        RecomendacionProductoService.ResultadoRankingProducto resultado =
            recomendacionService.rankear(List.of(noFavorito, favorito), 1L, ars);

        assertThat(resultado.personalizado()).isTrue();
        assertThat(resultado.items().get(0).producto().getIdProducto()).isEqualTo(1L);
        assertThat(resultado.items().get(0).motivo())
            .isEqualTo("Lo tenías en favoritos");
    }

    @Test
    void rankear_usaEtiquetasCruzadasConServicios() {
        Etiqueta etiqueta = etiqueta(5L, 1L);
        when(interesService.cargar(eq(1L), anyInt())).thenReturn(
            new InteresUsuarioService.InteresContenido(
                Set.of(), Set.of(), Set.of(),
                Set.of(5L), Set.of(1L), Map.of()));

        Producto conEstilo = producto(1L, "Con estilo", 10L, "100");
        conEstilo.setEtiquetas(etiquetas(etiqueta));
        Producto sinEstilo = producto(2L, "Sin estilo", 20L, "100");

        RecomendacionProductoService.ResultadoRankingProducto resultado =
            recomendacionService.rankear(List.of(sinEstilo, conEstilo), 1L, ars);

        assertThat(resultado.personalizado()).isTrue();
        assertThat(resultado.items().get(0).producto().getIdProducto()).isEqualTo(1L);
        assertThat(resultado.items().get(0).motivo())
            .isEqualTo("Estilo parecido a lo que te gusta");
    }

    @Test
    void rankear_priorizaAlArtistaComprado() {
        when(interesService.cargar(eq(1L), anyInt())).thenReturn(
            new InteresUsuarioService.InteresContenido(
                Set.of(), Set.of(20L), Set.of(),
                Set.of(), Set.of(), Map.of()));

        Producto deOtro = producto(1L, "Otro", 10L, "100");
        Producto delArtista = producto(2L, "Historial", 20L, "200");

        RecomendacionProductoService.ResultadoRankingProducto resultado =
            recomendacionService.rankear(List.of(deOtro, delArtista), 1L, ars);

        assertThat(resultado.items().get(0).producto().getIdProducto()).isEqualTo(2L);
        assertThat(resultado.items().get(0).motivo())
            .isEqualTo("Ya compraste de este artista");
    }

    @Test
    void similares_excluyeElPropioYPrefiereEtiquetasIguales() {
        Etiqueta etiqueta = etiqueta(5L, 1L);

        Producto origen = producto(1L, "Origen", 99L, "100");
        origen.setEtiquetas(etiquetas(etiqueta));
        origen.setTipoProducto(TipoProducto.builder()
            .idTipoProducto(99L)
            .nombre("Otro tipo")
            .descripcion("Otro")
            .build());

        Producto similar = producto(3L, "Similar", 20L, "100");
        similar.setEtiquetas(etiquetas(etiqueta));

        Producto distinto = producto(4L, "Distinto", 30L, "100");

        List<RecomendacionProductoService.ProductoPuntuado> resultado =
            recomendacionService.similares(
                origen, List.of(origen, distinto, similar), null, ars);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).producto().getIdProducto()).isEqualTo(3L);
        assertThat(resultado.get(0).motivo()).isEqualTo("Mismo estilo");
        assertThat(resultado)
            .extracting(item -> item.producto().getIdProducto())
            .doesNotContain(1L);
    }

    @Test
    void similares_respetaElLimitePorArtista() {
        Producto origen = producto(1L, "Origen", 99L, "100");
        Producto mismaA = producto(2L, "Misma A", 20L, "110");
        Producto mismaB = producto(3L, "Misma B", 20L, "110");

        List<RecomendacionProductoService.ProductoPuntuado> resultado =
            recomendacionService.similares(
                origen,
                List.of(mismaA, mismaB),
                null,
                ars,
                new RecomendacionProductoService.OpcionesSimilitudProducto(
                    RecomendacionProductoService.PesosSimilitudProducto.POR_DEFECTO,
                    10, 1));

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).producto().getArtista().getIdUsuario())
            .isEqualTo(20L);
    }

    private InteresUsuarioService.InteresContenido interesVacio() {
        return new InteresUsuarioService.InteresContenido(
            Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Map.of());
    }

    private Producto producto(Long id, String titulo, Long idArtista, String precio) {
        Producto producto = new Producto();
        producto.setIdProducto(id);
        producto.setTitulo(titulo);
        producto.setMoneda(ars);
        producto.setFechaCreacion(LocalDateTime.now());
        producto.setArtista(Usuario.builder()
            .idUsuario(idArtista)
            .nombreUsuario("artista" + idArtista)
            .build());
        producto.setTipoProducto(TipoProducto.builder()
            .idTipoProducto(1L)
            .nombre("Print digital")
            .descripcion("Print")
            .build());
        producto.setMedio(Medio.builder()
            .idMedio(1L)
            .nombre("Digital")
            .build());

        ProductoVariante variante = new ProductoVariante();
        variante.setIdVariante(id * 10);
        variante.setNombre("Única");
        variante.setPrecio(new BigDecimal(precio));
        variante.setStockDisponible(null);
        variante.setEstado(EstadoVariante.DISPONIBLE);
        variante.setProducto(producto);
        producto.getVariantes().add(variante);
        producto.setEtiquetas(new LinkedHashSet<>());
        return producto;
    }

    private Etiqueta etiqueta(Long idEtiqueta, Long idCategoria) {
        return Etiqueta.builder()
            .idEtiqueta(idEtiqueta)
            .nombre("Etiqueta " + idEtiqueta)
            .categoria(CategoriaEtiqueta.builder()
                .idCategoriaEtiqueta(idCategoria)
                .nombre("Categoría " + idCategoria)
                .build())
            .build();
    }

    private Set<ProductoEtiqueta> etiquetas(Etiqueta etiqueta) {
        ProductoEtiqueta asignacion = new ProductoEtiqueta();
        asignacion.setId(new ProductoEtiquetaId());
        asignacion.setEtiqueta(etiqueta);
        Set<ProductoEtiqueta> etiquetas = new LinkedHashSet<>();
        etiquetas.add(asignacion);
        return etiquetas;
    }
}
