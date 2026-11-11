package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.EPreferenciaContenidoSensible;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.FiltroProductoRequest;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.OrdenProducto;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoCatalogoDTO;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.TipoProducto;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.ProductoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ProductoCatalogoServiceRankingTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EventoLedgerRepository eventoLedgerRepository;

    @Mock
    private MonedaService monedaService;

    @Mock
    private ConversionMonedaService conversionMonedaService;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private RecomendacionProductoService recomendacionProductoService;

    @InjectMocks
    private ProductoCatalogoService catalogoService;

    private Moneda ars;

    @BeforeEach
    void setUp() {
        ars = Moneda.builder().idMoneda(1L).codigo("ARS").nombre("Peso").build();
        Usuario viewer = Usuario.builder()
            .idUsuario(1L)
            .nombreUsuario("viewer")
            .preferenciaSensible(EPreferenciaContenidoSensible.ADVERTIR)
            .monedaPreferida(ars)
            .build();
        when(usuarioRepository.findByIdUsuario(1L)).thenReturn(Optional.of(viewer));
    }

    @Test
    void listar_relevanciaUsaElRankingPersonalizado() {
        Producto viejo = producto(1L, "A", 10L, LocalDateTime.now().minusDays(3));
        Producto nuevo = producto(2L, "B", 20L, LocalDateTime.now());
        when(productoRepository.findByEstadoOrderByFechaCreacionDesc(
            EstadoProducto.DISPONIBLE)).thenReturn(List.of(viejo, nuevo));
        when(recomendacionProductoService.rankear(anyList(), eq(1L), eq(ars)))
            .thenReturn(new RecomendacionProductoService.ResultadoRankingProducto(
                List.of(
                    new RecomendacionProductoService.ProductoPuntuado(
                        nuevo, 0.9, "Lo tenías en favoritos"),
                    new RecomendacionProductoService.ProductoPuntuado(
                        viejo, 0.1, "Recomendado para vos")),
                true));

        PaginaDTO<ProductoCatalogoDTO> pagina = catalogoService.listar(
            new FiltroProductoRequest(), 1L, 0, 10, OrdenProducto.RELEVANCIA);

        assertThat(pagina.getPersonalizado()).isTrue();
        assertThat(pagina.getContenido())
            .extracting(ProductoCatalogoDTO::getIdProducto)
            .containsExactly(2L, 1L);
        assertThat(pagina.getContenido().get(0).getMotivo())
            .isEqualTo("Lo tenías en favoritos");
        assertThat(pagina.getContenido().get(0).getScore()).isEqualTo(0.9);
    }

    @Test
    void listar_sinSenalesCaeEnRecenciaYSinPersonalizar() {
        Producto viejo = producto(1L, "A", 10L, LocalDateTime.now().minusDays(3));
        Producto nuevo = producto(2L, "B", 20L, LocalDateTime.now());
        when(productoRepository.findByEstadoOrderByFechaCreacionDesc(
            EstadoProducto.DISPONIBLE)).thenReturn(List.of(viejo, nuevo));
        when(recomendacionProductoService.rankear(anyList(), eq(1L), eq(ars)))
            .thenReturn(new RecomendacionProductoService.ResultadoRankingProducto(
                List.of(new RecomendacionProductoService.ProductoPuntuado(
                    viejo, 0, null)),
                false));

        PaginaDTO<ProductoCatalogoDTO> pagina = catalogoService.listar(
            new FiltroProductoRequest(), 1L, 0, 10, OrdenProducto.RELEVANCIA);

        assertThat(pagina.getPersonalizado()).isFalse();
        assertThat(pagina.getContenido())
            .extracting(ProductoCatalogoDTO::getIdProducto)
            .containsExactly(2L, 1L);
        assertThat(pagina.getContenido().get(0).getScore()).isNull();
        assertThat(pagina.getContenido().get(0).getMotivo()).isNull();
    }

    private Producto producto(
            Long id, String titulo, Long idArtista, LocalDateTime fecha) {
        Producto producto = new Producto();
        producto.setIdProducto(id);
        producto.setTitulo(titulo);
        producto.setMoneda(ars);
        producto.setFechaCreacion(fecha);
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
        variante.setPrecio(new BigDecimal("100.00"));
        variante.setStockDisponible(null);
        variante.setEstado(EstadoVariante.DISPONIBLE);
        variante.setProducto(producto);
        producto.getVariantes().add(variante);
        return producto;
    }
}
