package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.CategoriaEtiqueta;
import com.temipora.backend_springboot.modelo.ContenidoSensible;
import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.Etiqueta;
import com.temipora.backend_springboot.modelo.EtiquetaEstado;
import com.temipora.backend_springboot.modelo.FacetasProductoDTO;
import com.temipora.backend_springboot.modelo.FiltroProductoRequest;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoEtiqueta;
import com.temipora.backend_springboot.modelo.ProductoEtiquetaId;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.TipoProducto;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.PublicacionReventaRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

/**
 * Conteos por faceta del catálogo de productos.
 *
 * Lo que se protege acá es la regla central: cada faceta se cuenta con todos
 * los filtros activos salvo el de su propia dimensión.
 */
@ExtendWith(MockitoExtension.class)
class ProductoCatalogoFacetasTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EventoLedgerRepository eventoLedgerRepository;
    @Mock
    private PublicacionReventaRepository publicacionReventaRepository;
    @Mock
    private MonedaService monedaService;
    @Mock
    private ConversionMonedaService conversionMonedaService;
    @Mock
    private LedgerService ledgerService;
    @Mock
    private RecomendacionProductoService recomendacionProductoService;
    @Mock
    private ReseniaPublicaService reseniaPublicaService;
    @Mock
    private AnclajeService anclajeService;

    @InjectMocks
    private ProductoCatalogoService catalogoService;

    private Moneda ars;
    private TipoProducto ilustracion;
    private Medio digital;

    @BeforeEach
    void setUp() {
        ars = Moneda.builder().idMoneda(1L).codigo("ARS").nombre("Peso").build();
        ilustracion = TipoProducto.builder()
            .idTipoProducto(10L).nombre("Ilustración").descripcion("Ilustración").build();
        digital = Medio.builder().idMedio(100L).nombre("Digital").tipoEntrega(
            com.temipora.backend_springboot.modelo.TipoEntrega.DIGITAL).build();

        Usuario viewer = Usuario.builder()
            .idUsuario(1L)
            .nombreUsuario("viewer")
            .preferenciaSensible(EPreferenciaContenidoSensible.ADVERTIR)
            .monedaPreferida(ars)
            .build();
        lenient().when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(viewer));
        lenient().when(reseniaPublicaService.promediosPorProductos(anyList()))
            .thenReturn(Map.of());
        lenient().when(publicacionReventaRepository
            .findByEstadoOrderByFechaPublicacionDesc(
                com.temipora.backend_springboot.modelo.EstadoPublicacionReventa.PUBLICADA))
            .thenReturn(List.of());
    }

    @Test
    void facetas_cuentaTiposYMedios() {
        Producto a = producto(1L, ilustracion, digital);
        Producto b = producto(2L, ilustracion, digital);
        when(productoRepository.findByEstadoOrderByFechaCreacionDesc(
            EstadoProducto.DISPONIBLE)).thenReturn(List.of(a, b));

        FacetasProductoDTO facetas = catalogoService.facetas(
            new FiltroProductoRequest(), null);

        assertThat(facetas.getTipos()).containsEntry("10", 2L);
        assertThat(facetas.getMedios()).containsEntry("100", 2L);
    }

    @Test
    void facetas_alFiltrarPorEtiqueta_otrasEtiquetasSiguenMostrandSuConteo() {
        Etiqueta gotico = etiqueta(1000L, "Gótico");
        Etiqueta acuarela = etiqueta(1001L, "Acuarela");

        Producto conGotico = producto(1L, ilustracion, digital);
        conGotico.setEtiquetas(new LinkedHashSet<>(List.of(asignacion(conGotico, gotico))));
        Producto conAcuarela = producto(2L, ilustracion, digital);
        conAcuarela.setEtiquetas(new LinkedHashSet<>(List.of(asignacion(conAcuarela, acuarela))));

        when(productoRepository.findByEstadoOrderByFechaCreacionDesc(
            EstadoProducto.DISPONIBLE)).thenReturn(List.of(conGotico, conAcuarela));

        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setIdsEtiquetas(List.of(1000L));

        FacetasProductoDTO facetas = catalogoService.facetas(filtro, null);

        // La dimensión filtrada (etiquetas) se cuenta sin su propio filtro.
        assertThat(facetas.getEtiquetas())
            .containsEntry("1000", 1L)
            .containsEntry("1001", 1L);
    }

    @Test
    void facetas_conExclusionDeEtiqueta_esaEtiquetaSigueMostrandoSuConteo() {
        Etiqueta gotico = etiqueta(1000L, "Gótico");
        Producto p = producto(1L, ilustracion, digital);
        p.setEtiquetas(new LinkedHashSet<>(List.of(asignacion(p, gotico))));

        when(productoRepository.findByEstadoOrderByFechaCreacionDesc(
            EstadoProducto.DISPONIBLE)).thenReturn(List.of(p));

        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setIdsEtiquetasExcluidas(List.of(1000L));

        FacetasProductoDTO facetas = catalogoService.facetas(filtro, null);

        // Al contar la dimensión de etiquetas se omiten también las
        // exclusiones, para que el usuario vea qué ganaría al dejar de excluir.
        assertThat(facetas.getEtiquetas()).containsEntry("1000", 1L);
    }

    @Test
    void facetas_laExclusionDeEtiquetaSiReduceOtrasDimensiones() {
        Etiqueta gotico = etiqueta(1000L, "Gótico");
        Producto conGotico = producto(1L, ilustracion, digital);
        conGotico.setEtiquetas(new LinkedHashSet<>(List.of(asignacion(conGotico, gotico))));
        Producto sinGotico = producto(2L, ilustracion, digital);

        when(productoRepository.findByEstadoOrderByFechaCreacionDesc(
            EstadoProducto.DISPONIBLE)).thenReturn(List.of(conGotico, sinGotico));

        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setIdsEtiquetasExcluidas(List.of(1000L));

        FacetasProductoDTO facetas = catalogoService.facetas(filtro, null);

        // La exclusión sí aplica al conteo de tipos: queda un solo producto.
        assertThat(facetas.getTipos()).containsEntry("10", 1L);
    }

    @Test
    void facetas_ignoraEtiquetasNoValidas() {
        Etiqueta valida = etiqueta(1000L, "Gótico");
        Etiqueta pendiente = etiqueta(1001L, "Pendiente");
        pendiente.setEstado(EtiquetaEstado.PENDIENTE);

        Producto p = producto(1L, ilustracion, digital);
        p.setEtiquetas(new LinkedHashSet<>(List.of(
            asignacion(p, valida), asignacion(p, pendiente))));

        when(productoRepository.findByEstadoOrderByFechaCreacionDesc(
            EstadoProducto.DISPONIBLE)).thenReturn(List.of(p));

        FacetasProductoDTO facetas = catalogoService.facetas(
            new FiltroProductoRequest(), null);

        assertThat(facetas.getEtiquetas()).containsEntry("1000", 1L);
        assertThat(facetas.getEtiquetas()).doesNotContainKey("1001");
    }

    @Test
    void facetas_cuentaContenidosSensibles() {
        Producto p = producto(1L, ilustracion, digital);
        p.setContenidosSensibles(new HashSet<>(
            Set.of(ContenidoSensible.VIOLENCIA, ContenidoSensible.DESNUDOS)));

        when(productoRepository.findByEstadoOrderByFechaCreacionDesc(
            EstadoProducto.DISPONIBLE)).thenReturn(List.of(p));

        FacetasProductoDTO facetas = catalogoService.facetas(
            new FiltroProductoRequest(), null);

        assertThat(facetas.getContenidosSensibles())
            .containsEntry("VIOLENCIA", 1L)
            .containsEntry("DESNUDOS", 1L);
    }

    @Test
    void facetas_conIncluirReventasApagado_noDevuelveLaFacetaOrigen() {
        Producto p = producto(1L, ilustracion, digital);
        when(productoRepository.findByEstadoOrderByFechaCreacionDesc(
            EstadoProducto.DISPONIBLE)).thenReturn(List.of(p));

        FiltroProductoRequest filtro = new FiltroProductoRequest();
        filtro.setIncluirReventas(false);

        FacetasProductoDTO facetas = catalogoService.facetas(filtro, null);

        assertThat(facetas.getOrigen()).isEmpty();
    }

    @Test
    void facetas_conIncluirReventas_devuelveElDesgloseDeOrigen() {
        Producto p = producto(1L, ilustracion, digital);
        when(productoRepository.findByEstadoOrderByFechaCreacionDesc(
            EstadoProducto.DISPONIBLE)).thenReturn(List.of(p));

        FacetasProductoDTO facetas = catalogoService.facetas(
            new FiltroProductoRequest(), null);

        assertThat(facetas.getOrigen())
            .containsEntry("producto", 1L)
            .containsEntry("reventa", 0L);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Producto producto(Long id, TipoProducto tipo, Medio medio) {
        Producto producto = new Producto();
        producto.setIdProducto(id);
        producto.setTitulo("Producto " + id);
        producto.setDescripcion("Descripción");
        producto.setTipoProducto(tipo);
        producto.setMedio(medio);
        producto.setMoneda(ars);
        producto.setEstado(EstadoProducto.DISPONIBLE);
        producto.setFechaCreacion(LocalDateTime.now());
        producto.setContenidosSensibles(new HashSet<>());
        producto.setEtiquetas(new LinkedHashSet<>());

        ProductoVariante variante = new ProductoVariante();
        variante.setIdVariante(id);
        variante.setProducto(producto);
        variante.setNombre("Variante");
        variante.setPrecio(new BigDecimal("1000"));
        variante.setStockDisponible(3);
        variante.setOrden(0);
        variante.setEstado(EstadoVariante.DISPONIBLE);
        producto.agregarVariante(variante);
        return producto;
    }

    private Etiqueta etiqueta(Long id, String nombre) {
        return Etiqueta.builder()
            .idEtiqueta(id)
            .nombre(nombre)
            .descripcion(nombre)
            .categoria(CategoriaEtiqueta.builder()
                .idCategoriaEtiqueta(5L).nombre("Estilo").descripcion("Estilo").build())
            .estado(EtiquetaEstado.VALIDA)
            .build();
    }

    private ProductoEtiqueta asignacion(Producto producto, Etiqueta etiqueta) {
        ProductoEtiqueta asignacion = new ProductoEtiqueta();
        asignacion.setId(new ProductoEtiquetaId());
        asignacion.setProducto(producto);
        asignacion.setEtiqueta(etiqueta);
        return asignacion;
    }
}
