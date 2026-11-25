package com.temipora.backend_springboot.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.temipora.backend_springboot.modelo.Compra;
import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EstadoCompraGrupo;
import com.temipora.backend_springboot.modelo.EstadoIntentoCobro;
import com.temipora.backend_springboot.modelo.IntentoCobro;
import com.temipora.backend_springboot.modelo.IntentoCobroGrupo;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.PaginaDTO;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.TipoEntrega;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.VentaProductoDTO;
import com.temipora.backend_springboot.repositorios.CompraGrupoRepository;
import com.temipora.backend_springboot.repositorios.CompraItemRepository;
import com.temipora.backend_springboot.repositorios.IntentoCobroGrupoRepository;

@ExtendWith(MockitoExtension.class)
class VentaProductoServiceTest {

    private static final LocalDateTime PAGO_A =
        LocalDateTime.of(2026, 11, 10, 15, 0);
    private static final LocalDateTime CREACION_B =
        LocalDateTime.of(2026, 11, 12, 8, 0);

    @Mock private CompraGrupoRepository compraGrupoRepository;
    @Mock private CompraItemRepository compraItemRepository;
    @Mock private IntentoCobroGrupoRepository intentoCobroGrupoRepository;
    @Mock private MonedaVistaService monedaVistaService;
    @Mock private ImagenVentaProductoService imagenVentaProductoService;

    private VentaProductoService service;
    private Moneda mostrada;

    @BeforeEach
    void setUp() {
        service = new VentaProductoService(
            compraGrupoRepository, compraItemRepository,
            intentoCobroGrupoRepository, monedaVistaService,
            imagenVentaProductoService);
        mostrada = moneda("USD");
    }

    @Test
    void listarArmaLasVentasConItemsIntentosYConversion() {
        CompraGrupo grupoA = grupoA();
        CompraGrupo grupoB = grupoB();
        PageImpl<CompraGrupo> page = new PageImpl<>(
            List.of(grupoA, grupoB), PageRequest.of(0, 10), 2);
        when(compraGrupoRepository.findAll(
            org.mockito.ArgumentMatchers
                .<Specification<CompraGrupo>>any(),
            any(Pageable.class))).thenReturn(page);
        when(monedaVistaService.preferidaDe(1L)).thenReturn(mostrada);
        when(monedaVistaService.factorCobroAMostrada(mostrada, PAGO_A))
            .thenReturn(new BigDecimal("2.00"));

        CompraItem itemA = item(1L, grupoA, 3L, 4L,
            new BigDecimal("10.50"), 2, TipoEntrega.DIGITAL);
        CompraItem itemA2 = item(2L, grupoA, null, null, null, 1, null);
        CompraItem itemB = item(3L, grupoB, null, null, null, 0, null);
        when(compraItemRepository.buscarPorGrupos(anyCollection()))
            .thenReturn(List.of(itemA, itemA2, itemB));
        when(imagenVentaProductoService.url(any())).thenReturn("url.png");

        IntentoCobroGrupo sinFecha = asignacion(grupoA, null, null);
        IntentoCobroGrupo primero = asignacion(grupoA, PAGO_A, null);
        IntentoCobroGrupo sinIntento = asignacion(grupoA, null, null);
        sinIntento.setIntentoCobro(null);
        IntentoCobro aprobado = intento(
            PAGO_A.plusMinutes(5), "mercadopago");
        IntentoCobroGrupo ultimo = asignacion(grupoA, null, aprobado);
        when(intentoCobroGrupoRepository.buscarPorGrupos(
            anyCollection(), eq(EstadoIntentoCobro.APROBADO)))
            .thenReturn(List.of(sinFecha, primero, sinIntento, ultimo));

        PaginaDTO<VentaProductoDTO> pagina = service.listar(
            1L, "2026-11", 9L, 0, 10);

        assertThat(pagina.getPagina()).isZero();
        assertThat(pagina.getTamanio()).isEqualTo(10);
        assertThat(pagina.getTotalElementos()).isEqualTo(2);
        assertThat(pagina.getTotalPaginas()).isEqualTo(1);
        assertThat(pagina.isHayMas()).isFalse();
        assertThat(pagina.getContenido()).hasSize(2);
        verify(compraGrupoRepository).cargarDetalle(List.of(50L, 51L));

        VentaProductoDTO venta = pagina.getContenido().get(0);
        assertThat(venta.getIdCompraGrupo()).isEqualTo(50L);
        assertThat(venta.getIdCompra()).isEqualTo(7L);
        assertThat(venta.getFecha()).isEqualTo(PAGO_A);
        assertThat(venta.getNombreUsuarioCliente()).isEqualTo("cliente");
        assertThat(venta.getCantidadItems()).isEqualTo(3);
        assertThat(venta.getMontoCobro()).isEqualByComparingTo("100.00");
        assertThat(venta.getMonedaCobro()).isEqualTo("ARS");
        assertThat(venta.getComisionMontoCobro()).isEqualByComparingTo("10.00");
        assertThat(venta.getMontoNetoCobro()).isEqualByComparingTo("90.00");
        assertThat(venta.getNetoMostrado()).isEqualByComparingTo("180.00");
        assertThat(venta.getMonedaMostrada()).isEqualTo("USD");
        assertThat(venta.getArancelMp()).isEqualByComparingTo("1.50");
        assertThat(venta.getNetoRecibidoMp()).isEqualByComparingTo("88.00");
        assertThat(venta.getEstado()).isEqualTo("PAGADO");
        assertThat(venta.getProveedor()).isEqualTo("mercadopago");
        assertThat(venta.getItems()).hasSize(2);
        assertThat(venta.getItems().get(0).getPrecioTotalCobro())
            .isEqualByComparingTo("21.00");
        assertThat(venta.getItems().get(0).getIdProducto()).isEqualTo(3L);
        assertThat(venta.getItems().get(0).getIdVariante()).isEqualTo(4L);
        assertThat(venta.getItems().get(0).getTipoEntrega())
            .isEqualTo(TipoEntrega.DIGITAL);
        assertThat(venta.getItems().get(0).getImagenUrl())
            .isEqualTo("url.png");
        assertThat(venta.getItems().get(1).getPrecioTotalCobro()).isNull();
        assertThat(venta.getItems().get(1).getIdProducto()).isNull();

        VentaProductoDTO sinDatos = pagina.getContenido().get(1);
        assertThat(sinDatos.getIdCompra()).isNull();
        assertThat(sinDatos.getFecha()).isEqualTo(CREACION_B);
        assertThat(sinDatos.getNombreUsuarioCliente()).isNull();
        assertThat(sinDatos.getMontoCobro()).isNull();
        assertThat(sinDatos.getMonedaCobro()).isEqualTo("ARS");
        assertThat(sinDatos.getComisionMontoCobro()).isNull();
        assertThat(sinDatos.getMontoNetoCobro()).isNull();
        assertThat(sinDatos.getNetoMostrado()).isNull();
        assertThat(sinDatos.getMonedaMostrada()).isEqualTo("USD");
        assertThat(sinDatos.getEstado()).isNull();
        assertThat(sinDatos.getProveedor()).isNull();
    }

    @Test
    void listarSinResultadosDevuelveUnaPaginaVacia() {
        when(compraGrupoRepository.findAll(
            org.mockito.ArgumentMatchers
                .<Specification<CompraGrupo>>any(),
            any(Pageable.class))).thenReturn(
            new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(monedaVistaService.preferidaDe(1L)).thenReturn(mostrada);

        PaginaDTO<VentaProductoDTO> pagina = service.listar(
            1L, null, null, 0, 10);

        assertThat(pagina.getContenido()).isEmpty();
        assertThat(pagina.getTotalElementos()).isZero();
        assertThat(pagina.isHayMas()).isFalse();
    }

    @Test
    void listarUsaElNetoDelArtistaAunqueNoHayaFactorDeConversion() {
        CompraGrupo grupo = grupoA();
        grupo.setNetoArtista(new BigDecimal("70.00"));
        when(compraGrupoRepository.findAll(
            org.mockito.ArgumentMatchers
                .<Specification<CompraGrupo>>any(),
            any(Pageable.class))).thenReturn(
            new PageImpl<>(List.of(grupo), PageRequest.of(0, 10), 1));
        when(monedaVistaService.preferidaDe(1L)).thenReturn(mostrada);
        when(monedaVistaService.factorCobroAMostrada(mostrada, PAGO_A))
            .thenReturn(null);
        when(compraItemRepository.buscarPorGrupos(anyCollection()))
            .thenReturn(List.of());
        when(intentoCobroGrupoRepository.buscarPorGrupos(
            anyCollection(), eq(EstadoIntentoCobro.APROBADO)))
            .thenReturn(List.of());

        VentaProductoDTO venta = service.listar(1L, "2026-11", null, 0, 10)
            .getContenido().get(0);

        assertThat(venta.getMontoNetoCobro()).isEqualByComparingTo("70.00");
        assertThat(venta.getNetoMostrado()).isEqualByComparingTo("70.00");
        assertThat(venta.getCantidadItems()).isZero();
        assertThat(venta.getArancelMp()).isNull();
    }

    @Test
    void rangoDeInterpretaElMesOFallaSinRomper() {
        assertThat(service.rangoDe(null)).isNull();
        assertThat(service.rangoDe("  ")).isNull();
        assertThat(service.rangoDe("no-es-mes")).isNull();

        VentaProductoService.RangoMes rango = service.rangoDe(" 2026-11 ");
        assertThat(rango).isNotNull();
        assertThat(rango.desde())
            .isEqualTo(LocalDateTime.of(2026, 11, 1, 0, 0));
        assertThat(rango.hasta())
            .isEqualTo(LocalDateTime.of(2026, 12, 1, 0, 0));
    }

    private static CompraGrupo grupoA() {
        Usuario cliente = new Usuario();
        cliente.setNombreUsuario("cliente");
        Compra compra = new Compra();
        compra.setIdCompra(7L);
        compra.setCliente(cliente);

        CompraGrupo grupo = new CompraGrupo();
        grupo.setIdCompraGrupo(50L);
        grupo.setCompra(compra);
        grupo.setEstado(EstadoCompraGrupo.PAGADO);
        grupo.setFechaPago(PAGO_A);
        grupo.setFechaCreacion(PAGO_A.minusDays(1));
        grupo.setSubtotal(new BigDecimal("100.00"));
        grupo.setComision(new BigDecimal("10.00"));
        grupo.setNetoArtista(null);
        grupo.setMoneda(moneda("ARS"));
        return grupo;
    }

    private static CompraGrupo grupoB() {
        CompraGrupo grupo = new CompraGrupo();
        grupo.setIdCompraGrupo(51L);
        grupo.setEstado(null);
        grupo.setFechaPago(null);
        grupo.setFechaCreacion(CREACION_B);
        grupo.setSubtotal(null);
        grupo.setComision(null);
        grupo.setNetoArtista(null);
        grupo.setMoneda(null);
        return grupo;
    }

    private static CompraItem item(
            Long id, CompraGrupo grupo, Long idProducto, Long idVariante,
            BigDecimal precioCobro, int cantidad, TipoEntrega tipoEntrega) {
        CompraItem item = new CompraItem();
        item.setIdCompraItem(id);
        item.setGrupo(grupo);
        item.setCantidad(cantidad);
        item.setPrecioCobro(precioCobro);
        item.setTipoEntrega(tipoEntrega);
        item.setTituloProducto("Obra");
        item.setNombreVariante("Única");
        if (idProducto != null) {
            Producto producto = new Producto();
            producto.setIdProducto(idProducto);
            item.setProducto(producto);
        }
        if (idVariante != null) {
            ProductoVariante variante = new ProductoVariante();
            variante.setIdVariante(idVariante);
            item.setVariante(variante);
        }
        return item;
    }

    private static IntentoCobroGrupo asignacion(
            CompraGrupo grupo, LocalDateTime aprobacion, IntentoCobro intento) {
        if (intento == null) {
            intento = intento(aprobacion, "mercadopago");
        }
        IntentoCobroGrupo asignacion = new IntentoCobroGrupo();
        asignacion.setGrupo(grupo);
        asignacion.setIntentoCobro(intento);
        return asignacion;
    }

    private static IntentoCobro intento(
            LocalDateTime aprobacion, String proveedor) {
        IntentoCobro intento = new IntentoCobro();
        intento.setFechaAprobacion(aprobacion);
        intento.setArancelMp(new BigDecimal("1.50"));
        intento.setNetoRecibidoMp(new BigDecimal("88.00"));
        intento.setProveedor(proveedor);
        return intento;
    }

    private static Moneda moneda(String codigo) {
        Moneda moneda = new Moneda();
        moneda.setCodigo(codigo);
        return moneda;
    }
}
