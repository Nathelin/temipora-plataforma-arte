package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.CarritoItemRequest;
import com.temipora.backend_springboot.modelo.CheckoutRequest;
import com.temipora.backend_springboot.modelo.CompraDTO;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.CompraItemDTO;
import com.temipora.backend_springboot.modelo.DerechoTermino;
import com.temipora.backend_springboot.modelo.ERol;
import com.temipora.backend_springboot.modelo.EstadoCompra;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoEntregaItem;
import com.temipora.backend_springboot.modelo.EstadoPublicacionReventa;
import com.temipora.backend_springboot.modelo.EstadoReserva;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.FiltroProductoRequest;
import com.temipora.backend_springboot.modelo.LicenciaProductoRequest;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.OrdenProducto;
import com.temipora.backend_springboot.modelo.OrigenCompraItem;
import com.temipora.backend_springboot.modelo.PagoCompraDTO;
import com.temipora.backend_springboot.modelo.ProductoDTO;
import com.temipora.backend_springboot.modelo.ProductoImagenRequest;
import com.temipora.backend_springboot.modelo.ProductoRequest;
import com.temipora.backend_springboot.modelo.ProductoVarianteRequest;
import com.temipora.backend_springboot.modelo.PublicacionReventaDTO;
import com.temipora.backend_springboot.modelo.ReventaRequest;
import com.temipora.backend_springboot.modelo.Rol;
import com.temipora.backend_springboot.modelo.TipoEntrega;
import com.temipora.backend_springboot.modelo.TipoEventoLedger;
import com.temipora.backend_springboot.modelo.TipoProducto;
import com.temipora.backend_springboot.modelo.TipoTopeReventa;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.repositorios.CompraItemRepository;
import com.temipora.backend_springboot.repositorios.DerechoTerminoRepository;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;
import com.temipora.backend_springboot.repositorios.MedioRepository;
import com.temipora.backend_springboot.repositorios.ProductoVarianteRepository;
import com.temipora.backend_springboot.repositorios.PublicacionReventaRepository;
import com.temipora.backend_springboot.repositorios.ReservaStockRepository;
import com.temipora.backend_springboot.repositorios.RolRepository;
import com.temipora.backend_springboot.repositorios.TipoProductoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;
import com.temipora.backend_springboot.servicios.AccesoDenegadoException;
import com.temipora.backend_springboot.servicios.CarritoService;
import com.temipora.backend_springboot.servicios.CompraPagoService;
import com.temipora.backend_springboot.servicios.CompraService;
import com.temipora.backend_springboot.servicios.MonedaService;
import com.temipora.backend_springboot.servicios.ProductoService;
import com.temipora.backend_springboot.servicios.ProductoCatalogoService;
import com.temipora.backend_springboot.servicios.ReventaService;

@SpringBootTest(properties = "pagos.proveedor=simulado")
@Transactional
class ReventaIntegracionTest {

    @Autowired private CarritoService carritoService;
    @Autowired private CompraService compraService;
    @Autowired private CompraPagoService compraPagoService;
    @Autowired private ProductoService productoService;
    @Autowired private ProductoCatalogoService productoCatalogoService;
    @Autowired private ReventaService reventaService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private TipoProductoRepository tipoProductoRepository;
    @Autowired private MedioRepository medioRepository;
    @Autowired private DerechoTerminoRepository derechoTerminoRepository;
    @Autowired private ProductoVarianteRepository varianteRepository;
    @Autowired private CompraItemRepository compraItemRepository;
    @Autowired private EventoLedgerRepository eventoLedgerRepository;
    @Autowired private PublicacionReventaRepository publicacionReventaRepository;
    @Autowired private ReservaStockRepository reservaStockRepository;
    @Autowired private MonedaService monedaService;

    @Test
    void reventa_respetaElTopeDeLaPrimeraVentaYPermiteCancelar() {
        Usuario artista = crearUsuario(ERol.ARTISTA);
        Usuario comprador = crearUsuario(ERol.CLIENTE);
        Usuario otro = crearUsuario(ERol.CLIENTE);
        ProductoDTO producto = crearProductoConReventa(artista);
        Long idVariante = producto.getVariantes().get(0).getIdVariante();

        CompraItem item = comprarYConfirmar(comprador, idVariante);

        var variante = varianteRepository.findByIdVariante(idVariante).orElseThrow();
        assertThat(variante.getPropietarioActual().getIdUsuario())
            .isEqualTo(comprador.getIdUsuario());
        assertThat(variante.getPrecioTopeReventa()).isEqualByComparingTo("150.00");
        assertThat(variante.getTipoTopeReventa())
            .isEqualTo(TipoTopeReventa.PRECIO_PUBLICACION);

        ReventaRequest excedido = request(item.getIdCompraItem(), "160.00");
        assertThatThrownBy(() -> reventaService.publicar(
            comprador.getNombreUsuario(), excedido))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("precio máximo");

        PublicacionReventaDTO publicada = reventaService.publicar(
            comprador.getNombreUsuario(), request(item.getIdCompraItem(), "150.00"));
        assertThat(publicada.getEstado())
            .isEqualTo(EstadoPublicacionReventa.PUBLICADA.name());
        assertThat(publicada.getPrecio()).isEqualByComparingTo("150.00");
        assertThat(publicada.getNombreVendedor())
            .isEqualTo(comprador.getNombreUsuario());
        assertThat(reventaService.mias(comprador.getNombreUsuario())).hasSize(1);

        PublicacionReventaDTO cancelada = reventaService.cancelar(
            comprador.getNombreUsuario(), publicada.getIdPublicacionReventa());
        assertThat(cancelada.getEstado())
            .isEqualTo(EstadoPublicacionReventa.ARCHIVADA.name());
        assertThat(cancelada.getFechaCierre()).isNotNull();

        assertThatThrownBy(() -> reventaService.publicar(
            otro.getNombreUsuario(), request(item.getIdCompraItem(), "150.00")))
            .isInstanceOf(AccesoDenegadoException.class)
            .hasMessageContaining("no es tuya");
    }

    @Test
    void compraDeReventa_transfierePropiedadYRegistraEventoReventa() {
        Usuario artista = crearUsuario(ERol.ARTISTA);
        Usuario vendedor = crearUsuario(ERol.CLIENTE);
        Usuario comprador = crearUsuario(ERol.CLIENTE);
        ProductoDTO producto = crearProductoConReventa(artista);
        Long idVariante = producto.getVariantes().get(0).getIdVariante();

        CompraItem primeraCompra = comprarYConfirmar(vendedor, idVariante);
        assertThat(reventaService.elegibles(vendedor.getNombreUsuario())).hasSize(1);

        PublicacionReventaDTO publicacion = reventaService.publicar(
            vendedor.getNombreUsuario(),
            request(primeraCompra.getIdCompraItem(), "150.00"));
        assertThat(publicacion.getPrecioTope()).isEqualByComparingTo("150.00");
        assertThat(reventaService.elegibles(vendedor.getNombreUsuario())).isEmpty();

        var detallePublico = reventaService.detallePublico(
            publicacion.getIdPublicacionReventa(), null);
        assertThat(detallePublico.getTitulo())
            .isEqualTo(producto.getVariantes().get(0).getNombre());
        assertThat(detallePublico.getTituloOriginal())
            .isEqualTo(producto.getTitulo());
        assertThat(detallePublico.getNombreVendedor())
            .isEqualTo(vendedor.getNombreUsuario());
        assertThat(detallePublico.getNombreArtista())
            .isEqualTo(artista.getNombreUsuario());
        assertThat(detallePublico.isPuedeComprar()).isTrue();

        var reventasPublicas = productoCatalogoService.listarReventas(null, 0, 10);
        var publica = reventasPublicas.getContenido().stream()
            .filter(item -> publicacion.getIdPublicacionReventa()
                .equals(item.getIdPublicacionReventa()))
            .findFirst()
            .orElseThrow();
        assertThat(publica.isEsReventa()).isTrue();
        assertThat(publica.getTitulo())
            .isEqualTo(producto.getVariantes().get(0).getNombre());
        assertThat(publica.getTituloOriginal()).isEqualTo(producto.getTitulo());
        assertThat(publica.getPrecioDesde()).isEqualByComparingTo("150.00");

        var catalogoMixto = productoCatalogoService.listar(
            new FiltroProductoRequest(), null, 0, 100,
            OrdenProducto.RECIENTES);
        assertThat(catalogoMixto.getContenido())
            .anyMatch(catalogo -> catalogo.isEsReventa()
                && publicacion.getIdPublicacionReventa()
                    .equals(catalogo.getIdPublicacionReventa()));

        FiltroProductoRequest soloProductos = new FiltroProductoRequest();
        soloProductos.setIncluirReventas(false);
        var catalogoSoloProductos = productoCatalogoService.listar(
            soloProductos, null, 0, 100, OrdenProducto.RECIENTES);
        assertThat(catalogoSoloProductos.getContenido())
            .noneMatch(catalogo -> publicacion.getIdPublicacionReventa()
                .equals(catalogo.getIdPublicacionReventa()));

        CarritoItemRequest item = new CarritoItemRequest();
        item.setIdVariante(idVariante);
        item.setIdPublicacionReventa(publicacion.getIdPublicacionReventa());
        item.setCantidad(1);
        carritoService.agregar(comprador.getNombreUsuario(), item);

        CheckoutRequest checkout = new CheckoutRequest();
        checkout.setClaveIdempotencia("reventa-compra-" + UUID.randomUUID());
        CompraDTO compra = compraService.crearDesdeCarrito(
            comprador.getNombreUsuario(), checkout);

        assertThat(compra.getGrupos()).hasSize(1);
        assertThat(compra.getGrupos().get(0).getIdArtista())
            .isEqualTo(vendedor.getIdUsuario());
        assertThat(compra.getGrupos().get(0).isEsReventa()).isTrue();
        CompraItemDTO itemComprado = compra.getGrupos().get(0).getItems().get(0);
        assertThat(itemComprado.getOrigen()).isEqualTo(OrigenCompraItem.REVENTA);
        assertThat(itemComprado.getPrecioTotalCobro())
            .isEqualByComparingTo("150.00");
        assertThat(publicacionReventaRepository
            .findByIdPublicacionReventa(publicacion.getIdPublicacionReventa())
            .orElseThrow().getEstado())
            .isEqualTo(EstadoPublicacionReventa.RESERVADA);

        PagoCompraDTO pago = compraPagoService.iniciarPago(
            comprador.getNombreUsuario(), compra.getIdCompra(),
            compra.getGrupos().get(0).getIdCompraGrupo());
        compraPagoService.confirmarSimulado(
            comprador.getNombreUsuario(), pago.getIdIntentoCobro());

        var variante = varianteRepository.findByIdVariante(idVariante).orElseThrow();
        assertThat(variante.getPropietarioActual().getIdUsuario())
            .isEqualTo(comprador.getIdUsuario());
        assertThat(variante.getStockDisponible()).isZero();
        assertThat(variante.getEstado()).isEqualTo(EstadoVariante.AGOTADO);
        assertThat(publicacionReventaRepository
            .findByIdPublicacionReventa(publicacion.getIdPublicacionReventa())
            .orElseThrow().getEstado())
            .isEqualTo(EstadoPublicacionReventa.VENDIDA);

        var eventos = eventoLedgerRepository
            .findByVarianteIdVarianteOrderBySecuenciaAsc(idVariante);
        assertThat(eventos).hasSize(3);
        assertThat(eventos.get(2).getTipo()).isEqualTo(TipoEventoLedger.REVENTA);
        assertThat(eventos.get(2).getSecuencia()).isEqualTo(3L);
        assertThat(eventos.get(2).getHashPrevio())
            .isEqualTo(eventos.get(1).getHashEvento());
        assertThat(eventos.get(2).getActorOrigen().getIdUsuario())
            .isEqualTo(vendedor.getIdUsuario());
        assertThat(eventos.get(2).getActorDestino().getIdUsuario())
            .isEqualTo(comprador.getIdUsuario());
        assertThat(eventos.get(2).getMonto()).isEqualByComparingTo("150.00");

        var detalle = compraService.obtener(
            comprador.getNombreUsuario(), compra.getIdCompra());
        var itemDetalle = detalle.getGrupos().get(0).getItems().get(0);
        assertThat(itemDetalle.getNombreArtistaOriginal())
            .isEqualTo(artista.getNombreUsuario());
        assertThat(itemDetalle.getEstadoEntrega())
            .isEqualTo(EstadoEntregaItem.ENTREGADO);

        var mias = reventaService.mias(vendedor.getNombreUsuario());
        assertThat(mias).hasSize(1);
        assertThat(mias.get(0).getEstado()).isEqualTo("VENDIDA");
        assertThat(mias.get(0).getNombreComprador())
            .isEqualTo(comprador.getNombreUsuario());
        assertThat(mias.get(0).getMontoComision())
            .isEqualByComparingTo("15.00");
        assertThat(mias.get(0).getMontoNeto()).isEqualByComparingTo("135.00");

        assertThat(reventaService.elegibles(vendedor.getNombreUsuario())).isEmpty();
        assertThat(reventaService.elegibles(comprador.getNombreUsuario())).hasSize(1);
        var publicaVendida = reventaService.detallePublico(
            publicacion.getIdPublicacionReventa(), comprador.getIdUsuario());
        assertThat(publicaVendida.isPuedeComprar()).isFalse();
        assertThat(publicaVendida.isEsPropia()).isFalse();
        assertThat(productoCatalogoService.listarReventas(null, 0, 10).getContenido())
            .noneMatch(catalogo -> publicacion.getIdPublicacionReventa()
                .equals(catalogo.getIdPublicacionReventa()));
    }

    @Test
    void cancelarCompraPendienteLiberaLaReservaYLaPublicacion() {
        Usuario artista = crearUsuario(ERol.ARTISTA);
        Usuario vendedor = crearUsuario(ERol.CLIENTE);
        Usuario comprador = crearUsuario(ERol.CLIENTE);
        ProductoDTO producto = crearProductoConReventa(artista);
        Long idVariante = producto.getVariantes().get(0).getIdVariante();

        CompraItem primeraCompra = comprarYConfirmar(vendedor, idVariante);
        PublicacionReventaDTO publicacion = reventaService.publicar(
            vendedor.getNombreUsuario(),
            request(primeraCompra.getIdCompraItem(), "150.00"));

        CarritoItemRequest item = new CarritoItemRequest();
        item.setIdVariante(idVariante);
        item.setIdPublicacionReventa(publicacion.getIdPublicacionReventa());
        item.setCantidad(1);
        carritoService.agregar(comprador.getNombreUsuario(), item);

        CheckoutRequest checkout = new CheckoutRequest();
        checkout.setClaveIdempotencia("reventa-cancelar-" + UUID.randomUUID());
        CompraDTO compra = compraService.crearDesdeCarrito(
            comprador.getNombreUsuario(), checkout);
        var grupo = compra.getGrupos().get(0);
        assertThat(grupo.isPuedeCancelar()).isTrue();
        assertThat(grupo.getReservaExpiraEn()).isNotNull();
        assertThat(publicacionReventaRepository
            .findByIdPublicacionReventa(publicacion.getIdPublicacionReventa())
            .orElseThrow().getEstado())
            .isEqualTo(EstadoPublicacionReventa.RESERVADA);

        CompraDTO cancelada = compraService.cancelarGrupo(
            comprador.getNombreUsuario(), compra.getIdCompra(),
            grupo.getIdCompraGrupo());

        assertThat(cancelada.getEstado()).isEqualTo(EstadoCompra.CANCELADA);
        assertThat(cancelada.getGrupos().get(0).getEstado())
            .isEqualTo(EstadoCompraGrupo.CANCELADO);
        assertThat(cancelada.getGrupos().get(0).isPuedePagar()).isFalse();
        assertThat(publicacionReventaRepository
            .findByIdPublicacionReventa(publicacion.getIdPublicacionReventa())
            .orElseThrow().getEstado())
            .isEqualTo(EstadoPublicacionReventa.PUBLICADA);
        var reserva = reservaStockRepository.findByCompraItemIdCompraItem(
            grupo.getItems().get(0).getIdCompraItem()).orElseThrow();
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.VENCIDA);
        assertThat(varianteRepository.findByIdVariante(idVariante).orElseThrow()
            .getPropietarioActual().getIdUsuario())
            .isEqualTo(vendedor.getIdUsuario());
    }

    private CompraItem comprarYConfirmar(Usuario comprador, Long idVariante) {
        CarritoItemRequest item = new CarritoItemRequest();
        item.setIdVariante(idVariante);
        item.setCantidad(1);
        carritoService.agregar(comprador.getNombreUsuario(), item);

        CheckoutRequest checkout = new CheckoutRequest();
        checkout.setClaveIdempotencia("reventa-test-" + UUID.randomUUID());
        CompraDTO compra = compraService.crearDesdeCarrito(
            comprador.getNombreUsuario(), checkout);
        Long idGrupo = compra.getGrupos().get(0).getIdCompraGrupo();
        PagoCompraDTO pago = compraPagoService.iniciarPago(
            comprador.getNombreUsuario(), compra.getIdCompra(), idGrupo);
        compraPagoService.confirmarSimulado(
            comprador.getNombreUsuario(), pago.getIdIntentoCobro());
        return compraItemRepository.findByGrupoCompraIdCompra(
            compra.getIdCompra()).get(0);
    }

    private ReventaRequest request(Long idCompraItem, String precio) {
        ReventaRequest request = new ReventaRequest();
        request.setIdCompraItem(idCompraItem);
        request.setPrecio(new BigDecimal(precio));
        return request;
    }

    private ProductoDTO crearProductoConReventa(Usuario artista) {
        TipoProducto tipo = tipoProductoRepository
            .findByActivoTrueOrderByNombreAsc().get(0);
        Medio medio = medioRepository.findByActivoTrueOrderByNombreAsc().stream()
            .filter(m -> m.getTipoEntrega() == TipoEntrega.DIGITAL)
            .findFirst()
            .orElseThrow();
        Moneda moneda = monedaService.predeterminada();
        DerechoTermino reventa = derechoTerminoRepository
            .findByActivoTrueOrderByOrdenAsc().stream()
            .filter(derecho -> "REVENTA".equals(derecho.getCodigo()))
            .findFirst()
            .orElseThrow();

        String sha = UUID.randomUUID().toString().replace("-", "")
            + UUID.randomUUID().toString().replace("-", "");
        ProductoImagenRequest imagen = new ProductoImagenRequest();
        imagen.setUrl("/uploads/reventa-" + UUID.randomUUID() + ".webp");
        imagen.setSha256(sha);
        imagen.setClaveArchivoOriginal("productos/reventa-" + UUID.randomUUID() + ".jpg");
        imagen.setOrden(0);

        ProductoVarianteRequest variante = new ProductoVarianteRequest();
        variante.setNombre("Única");
        variante.setPrecio(new BigDecimal("150.00"));
        variante.setStockDisponible(1);
        variante.setOrden(0);
        variante.setUrlImagen(imagen.getUrl());
        variante.setSha256(sha);
        variante.setClaveArchivoOriginal(imagen.getClaveArchivoOriginal());

        LicenciaProductoRequest licencia = new LicenciaProductoRequest();
        licencia.setIdsDerechos(Set.of(reventa.getIdDerechoTermino()));
        licencia.setPorcentajeRegalia(BigDecimal.ZERO);

        ProductoRequest request = new ProductoRequest();
        request.setIdTipoProducto(tipo.getIdTipoProducto());
        request.setIdMedio(medio.getIdMedio());
        request.setIdMoneda(moneda.getIdMoneda());
        request.setTitulo("Reventa " + UUID.randomUUID());
        request.setDescripcion("Producto digital para probar la reventa.");
        request.setImagenes(new ArrayList<>(List.of(imagen)));
        request.setVariantes(new ArrayList<>(List.of(variante)));
        request.setIdsEtiquetas(Set.of());
        request.setContenidosSensibles(Set.of());
        request.setLicencia(licencia);
        return productoService.crear(artista.getIdUsuario(), request);
    }

    private Usuario crearUsuario(ERol rolNombre) {
        Rol rol = rolRepository.findByNombre(rolNombre)
            .orElseThrow(() -> new IllegalStateException(
                "No existe el rol " + rolNombre));
        String nombre = rolNombre.name().toLowerCase()
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(nombre);
        usuario.setNombreDisplay(nombre);
        usuario.setContrasenia("test");
        usuario.setCorreo(nombre + "@test.local");
        usuario.setFechaCreacion(LocalDateTime.now());
        usuario.setMonedaPreferida(monedaService.predeterminada());
        usuario.setRol(rol);
        return usuarioRepository.save(usuario);
    }
}
