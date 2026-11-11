package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.Carrito;
import com.temipora.backend_springboot.modelo.CarritoDTO;
import com.temipora.backend_springboot.modelo.CarritoItem;
import com.temipora.backend_springboot.modelo.CarritoItemRequest;
import com.temipora.backend_springboot.modelo.EstadoProducto;
import com.temipora.backend_springboot.modelo.EstadoReserva;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.TipoEntrega;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CarritoItemRepository;
import com.temipora.backend_springboot.repositorios.CarritoRepository;
import com.temipora.backend_springboot.repositorios.ProductoVarianteRepository;
import com.temipora.backend_springboot.repositorios.ReservaStockRepository;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoOAuthService;

@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    @Mock private CarritoRepository carritoRepository;
    @Mock private CarritoItemRepository carritoItemRepository;
    @Mock private ProductoVarianteRepository varianteRepository;
    @Mock private ReservaStockRepository reservaStockRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private MonedaService monedaService;
    @Mock private ConversionMonedaService conversionMonedaService;
    @Mock private MercadoPagoOAuthService oauthService;

    private CarritoService service;

    @BeforeEach
    void setUp() {
        service = new CarritoService(
            carritoRepository,
            carritoItemRepository,
            varianteRepository,
            reservaStockRepository,
            usuarioService,
            monedaService,
            conversionMonedaService,
            oauthService,
            true,
            "simulado");
    }

    @Test
    void obtener_sinCarrito_devuelveVacio() {
        Usuario cliente = usuario(1L, "cliente");
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(carritoRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.empty());
        stubsDeCalculo();

        CarritoDTO dto = service.obtener("cliente");

        assertThat(dto.getCantidadItems()).isZero();
        assertThat(dto.getTotal()).isEqualByComparingTo("0.00");
        assertThat(dto.isPuedeCheckout()).isFalse();
    }

    @Test
    void agregar_varianteUnica_creaItemYCalculaTotal() {
        Usuario cliente = usuario(1L, "cliente");
        Usuario artista = usuario(2L, "artista");
        ProductoVariante variante = variante(10L, artista, 1, "100.00");
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(varianteRepository.findByIdVariante(10L)).thenReturn(Optional.of(variante));
        when(reservaStockRepository.sumarCantidad(10L, EstadoReserva.ACTIVA))
            .thenReturn(0L);
        when(carritoRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.empty());
        when(carritoRepository.save(any(Carrito.class))).thenAnswer(inv -> {
            Carrito carrito = inv.getArgument(0);
            if (carrito.getIdCarrito() == null) carrito.setIdCarrito(1L);
            return carrito;
        });
        when(carritoItemRepository
            .findByCarritoIdCarritoAndVarianteIdVariante(1L, 10L))
            .thenReturn(Optional.empty());
        stubsDeCalculo();

        CarritoItemRequest request = new CarritoItemRequest();
        request.setIdVariante(10L);
        request.setCantidad(1);
        CarritoDTO dto = service.agregar("cliente", request);

        assertThat(dto.getCantidadItems()).isEqualTo(1);
        assertThat(dto.getGrupos()).hasSize(1);
        assertThat(dto.getTotal()).isEqualByComparingTo("100.00");
        assertThat(dto.getGrupos().get(0).getItems().get(0).getNombreVariante())
            .isEqualTo("Única");
    }

    @Test
    void agregar_productoPropio_lanzaError() {
        Usuario cliente = usuario(1L, "cliente");
        ProductoVariante variante = variante(10L, cliente, 1, "100.00");
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(varianteRepository.findByIdVariante(10L)).thenReturn(Optional.of(variante));

        CarritoItemRequest request = new CarritoItemRequest();
        request.setIdVariante(10L);

        assertThatThrownBy(() -> service.agregar("cliente", request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("propio");
    }

    @Test
    void agregar_otroArtistaConCarrito_lanzaConflicto() {
        Usuario cliente = usuario(1L, "cliente");
        Usuario artistaA = usuario(2L, "artistaA");
        Usuario artistaB = usuario(3L, "artistaB");
        ProductoVariante nueva = variante(11L, artistaB, 1, "100.00");
        Carrito carrito = new Carrito();
        carrito.setIdCarrito(1L);
        carrito.setUsuario(cliente);
        CarritoItem existente = new CarritoItem();
        existente.setVariante(variante(10L, artistaA, 1, "50.00"));
        carrito.agregarItem(existente);

        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(varianteRepository.findByIdVariante(11L)).thenReturn(Optional.of(nueva));
        when(reservaStockRepository.sumarCantidad(11L, EstadoReserva.ACTIVA))
            .thenReturn(0L);
        when(carritoRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(carrito));

        CarritoItemRequest request = new CarritoItemRequest();
        request.setIdVariante(11L);

        assertThatThrownBy(() -> service.agregar("cliente", request))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("un solo artista");
    }

    @Test
    void agregar_unicaConCantidadDos_lanzaError() {
        Usuario cliente = usuario(1L, "cliente");
        Usuario artista = usuario(2L, "artista");
        ProductoVariante variante = variante(10L, artista, 1, "100.00");
        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(varianteRepository.findByIdVariante(10L)).thenReturn(Optional.of(variante));

        CarritoItemRequest request = new CarritoItemRequest();
        request.setIdVariante(10L);
        request.setCantidad(2);

        assertThatThrownBy(() -> service.agregar("cliente", request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("una unidad");
    }

    @Test
    void actualizar_unicaConCantidadDos_lanzaError() {
        Usuario cliente = usuario(1L, "cliente");
        Carrito carrito = new Carrito();
        carrito.setIdCarrito(1L);
        carrito.setUsuario(cliente);
        CarritoItem item = new CarritoItem();
        item.setIdCarritoItem(5L);
        item.setVariante(variante(10L, usuario(2L, "artista"), 1, "100.00"));
        item.setCantidad(1);
        carrito.agregarItem(item);

        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(carritoRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(carrito));
        when(carritoItemRepository.findByIdCarritoItemAndCarritoIdCarrito(5L, 1L))
            .thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.actualizar("cliente", 5L, 2))
            .isInstanceOf(ConflictoException.class)
            .hasMessageContaining("una unidad");
    }

    @Test
    void actualizar_quitarYVaciar_modificanElCarrito() {
        Usuario cliente = usuario(1L, "cliente");
        Carrito carrito = new Carrito();
        carrito.setIdCarrito(1L);
        carrito.setUsuario(cliente);
        CarritoItem item = new CarritoItem();
        item.setIdCarritoItem(5L);
        item.setVariante(variante(10L, usuario(2L, "artista"), 5, "100.00"));
        item.setCantidad(1);
        carrito.agregarItem(item);

        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(carritoRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(carrito));
        when(carritoItemRepository.findByIdCarritoItemAndCarritoIdCarrito(5L, 1L))
            .thenReturn(Optional.of(item));
        when(reservaStockRepository.sumarCantidad(10L, EstadoReserva.ACTIVA))
            .thenReturn(0L);
        when(carritoRepository.save(any(Carrito.class))).thenAnswer(inv -> inv.getArgument(0));
        stubsDeCalculo();

        CarritoDTO actualizado = service.actualizar("cliente", 5L, 1);
        assertThat(actualizado.getCantidadItems()).isEqualTo(1);

        CarritoDTO quitado = service.quitar("cliente", 5L);
        assertThat(quitado.getCantidadItems()).isZero();

        CarritoDTO vaciado = service.vaciar("cliente");
        assertThat(vaciado.getCantidadItems()).isZero();
    }

    @Test
    void vaciarArtista_eliminaSoloLosItemsDeEseArtista() {
        Usuario cliente = usuario(1L, "cliente");
        Carrito carrito = new Carrito();
        carrito.setIdCarrito(1L);
        carrito.setUsuario(cliente);

        CarritoItem itemA = new CarritoItem();
        itemA.setIdCarritoItem(5L);
        itemA.setVariante(variante(10L, usuario(2L, "artistaA"), 1, "100.00"));
        itemA.setCantidad(1);
        carrito.agregarItem(itemA);

        CarritoItem itemB = new CarritoItem();
        itemB.setIdCarritoItem(6L);
        itemB.setVariante(variante(11L, usuario(3L, "artistaB"), 1, "50.00"));
        itemB.setCantidad(1);
        carrito.agregarItem(itemB);

        when(usuarioService.getByNombreUsuario("cliente")).thenReturn(cliente);
        when(carritoRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(carrito));
        when(carritoRepository.save(any(Carrito.class))).thenAnswer(inv -> inv.getArgument(0));
        stubsDeCalculo();

        CarritoDTO dto = service.vaciarArtista("cliente", 2L);

        assertThat(dto.getGrupos()).hasSize(1);
        assertThat(dto.getGrupos().get(0).getIdArtista()).isEqualTo(3L);
        assertThat(dto.getCantidadItems()).isEqualTo(1);
    }

    private void stubsDeCalculo() {
        lenient().when(monedaService.obtenerPorCodigo("ARS")).thenReturn(moneda());
        lenient().when(conversionMonedaService.factor(any(), any(), any()))
            .thenReturn(BigDecimal.ONE);
    }

    private ProductoVariante variante(
            Long id, Usuario artista, int stock, String precio) {
        Producto producto = new Producto();
        producto.setIdProducto(id + 100);
        producto.setTitulo("Pieza");
        producto.setEstado(EstadoProducto.DISPONIBLE);
        producto.setArtista(artista);
        producto.setMoneda(moneda());
        Medio medio = new Medio();
        medio.setTipoEntrega(TipoEntrega.DIGITAL);
        producto.setMedio(medio);

        ProductoVariante variante = new ProductoVariante();
        variante.setIdVariante(id);
        variante.setProducto(producto);
        variante.setNombre("Única");
        variante.setPrecio(new BigDecimal(precio));
        variante.setStockDisponible(stock);
        variante.setEstado(EstadoVariante.DISPONIBLE);
        variante.setImagenUrl("/uploads/x.webp");
        variante.setSha256("a".repeat(64));
        variante.setClaveArchivoOriginal("productos/x.jpg");
        return variante;
    }

    private Usuario usuario(Long id, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setNombreUsuario(nombre);
        return usuario;
    }

    private Moneda moneda() {
        Moneda moneda = new Moneda();
        moneda.setIdMoneda(1L);
        moneda.setCodigo("ARS");
        moneda.setNombre("Peso");
        return moneda;
    }
}
