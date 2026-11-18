package com.temipora.backend_springboot.servicios.pagos.producto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.Compra;
import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.IntentoCobro;
import com.temipora.backend_springboot.modelo.IntentoCobroGrupo;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.TipoEntrega;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.pagos.EstadoCobroProveedor;
import com.temipora.backend_springboot.servicios.pagos.ResultadoCobro;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoClient;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoException;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoOAuthService;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoPago;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoPreferencia;

@ExtendWith(MockitoExtension.class)
class ProveedorPagoProductoMercadoPagoTest {

    @Mock private MercadoPagoClient cliente;
    @Mock private MercadoPagoOAuthService oauthService;

    private ProveedorPagoProductoMercadoPago proveedor;

    @BeforeEach
    void setUp() {
        proveedor = new ProveedorPagoProductoMercadoPago(
            cliente,
            oauthService,
            "https://host/api/pagos/mercadopago/webhook",
            "",
            "https://front.test/",
            true,
            true,
            true,
            30);
    }

    @Test
    void crearCobro_armaLaPreferenciaDelVendedorYDevuelveSandbox() {
        IntentoCobro intento = intento(1L, 2L);
        CompraGrupo grupo = intento.getGrupos().get(0).getGrupo();
        when(oauthService.tokenVigente(2L)).thenReturn("token-vendedor");
        when(cliente.crearPreferencia(anyMap(), eq("clave"), eq("token-vendedor")))
            .thenReturn(new MercadoPagoPreferencia(
                "pref-1", "https://init", "https://sandbox"));

        ResultadoCobro resultado = proveedor.crearCobro(intento, List.of(grupo));

        assertThat(resultado.idExterno()).isEqualTo("pref-1");
        assertThat(resultado.instruccion()).isEqualTo("https://sandbox");
        assertThat(resultado.expiraEn()).isNotNull();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor =
            ArgumentCaptor.forClass(Map.class);
        verify(cliente).crearPreferencia(
            captor.capture(), eq("clave"), eq("token-vendedor"));
        assertThat(captor.getValue().get("external_reference"))
            .isEqualTo("COMPRA-1");
        assertThat(captor.getValue().get("marketplace_fee"))
            .isEqualTo(new BigDecimal("10.00"));
        assertThat(captor.getValue().get("binary_mode")).isEqualTo(true);
        assertThat(String.valueOf(captor.getValue().get("notification_url")))
            .isEqualTo("https://host/api/pagos/mercadopago/webhook-compras");
        @SuppressWarnings("unchecked")
        Map<String, Object> backUrls =
            (Map<String, Object>) captor.getValue().get("back_urls");
        assertThat(backUrls.get("success"))
            .isEqualTo("https://front.test/compras/1?pago=success");
    }

    @Test
    void crearCobro_conVariosGrupos_lanzaErrorDeSplitNoHabilitado() {
        IntentoCobro intento = intento(1L, 2L);
        CompraGrupo grupo = intento.getGrupos().get(0).getGrupo();

        assertThatThrownBy(() -> proveedor.crearCobro(
            intento, List.of(grupo, grupo)))
            .isInstanceOf(MercadoPagoException.class)
            .hasMessageContaining("1:N");
    }

    @Test
    void crearCobro_sinUrlDeWebhook_lanzaError() {
        ProveedorPagoProductoMercadoPago sinWebhook =
            new ProveedorPagoProductoMercadoPago(
                cliente, oauthService, "", "", "https://front.test",
                false, false, false, 30);
        IntentoCobro intento = intento(1L, 2L);

        assertThatThrownBy(() -> sinWebhook.crearCobro(
            intento, List.of(intento.getGrupos().get(0).getGrupo())))
            .isInstanceOf(MercadoPagoException.class)
            .hasMessageContaining("webhook");
    }

    @Test
    void consultarEstado_mapeaElPagoAprobado() {
        IntentoCobro intento = intento(1L, 2L);
        when(oauthService.tokenVigente(2L)).thenReturn("token-vendedor");
        when(cliente.buscarPagos("COMPRA-1", "token-vendedor"))
            .thenReturn(List.of(pago("approved", "100.00")));

        Optional<EstadoCobroProveedor> estado = proveedor.consultarEstado(intento);

        assertThat(estado).isPresent();
        assertThat(estado.get().estado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(estado.get().idPagoExterno()).isEqualTo("99");
    }

    @Test
    void consultarEstado_sinPagos_devuelveVacio() {
        IntentoCobro intento = intento(1L, 2L);
        when(oauthService.tokenVigente(2L)).thenReturn("token-vendedor");
        when(cliente.buscarPagos("COMPRA-1", "token-vendedor"))
            .thenReturn(List.of());

        assertThat(proveedor.consultarEstado(intento)).isEmpty();
    }

    @Test
    void consultarEstado_montoDistinto_lanzaError() {
        IntentoCobro intento = intento(1L, 2L);
        when(oauthService.tokenVigente(2L)).thenReturn("token-vendedor");
        when(cliente.buscarPagos("COMPRA-1", "token-vendedor"))
            .thenReturn(List.of(pago("approved", "90.00")));

        assertThatThrownBy(() -> proveedor.consultarEstado(intento))
            .isInstanceOf(MercadoPagoException.class)
            .hasMessageContaining("monto");
    }

    private IntentoCobro intento(Long idCompra, Long idArtista) {
        Moneda moneda = new Moneda();
        moneda.setIdMoneda(1L);
        moneda.setCodigo("ARS");
        Usuario artista = new Usuario();
        artista.setIdUsuario(idArtista);
        artista.setNombreUsuario("artista");

        Compra compra = new Compra();
        compra.setIdCompra(idCompra);
        compra.setMoneda(moneda);

        CompraGrupo grupo = new CompraGrupo();
        grupo.setIdCompraGrupo(5L);
        grupo.setCompra(compra);
        grupo.setArtista(artista);
        grupo.setMoneda(moneda);
        grupo.setSubtotal(new BigDecimal("100.00"));
        grupo.setComision(new BigDecimal("10.00"));
        grupo.setNetoArtista(new BigDecimal("90.00"));
        compra.agregarGrupo(grupo);

        CompraItem item = new CompraItem();
        item.setTituloProducto("Pieza");
        item.setNombreVariante("Única");
        item.setTipoEntrega(TipoEntrega.DIGITAL);
        item.setCantidad(1);
        item.setPrecioCobro(new BigDecimal("100.00"));
        grupo.agregarItem(item);

        IntentoCobro intento = new IntentoCobro();
        intento.setIdIntentoCobro(7L);
        intento.setCompra(compra);
        intento.setExternalReference("COMPRA-" + idCompra);
        intento.setClaveIdempotencia("clave");
        intento.setMonto(new BigDecimal("100.00"));
        intento.setMoneda(moneda);
        intento.setFechaCreacion(LocalDateTime.now().minusMinutes(5));

        IntentoCobroGrupo asignacion = new IntentoCobroGrupo();
        asignacion.setGrupo(grupo);
        asignacion.setMonto(new BigDecimal("100.00"));
        asignacion.setComision(new BigDecimal("10.00"));
        asignacion.setNeto(new BigDecimal("90.00"));
        intento.agregarGrupo(asignacion);
        return intento;
    }

    private MercadoPagoPago pago(String status, String monto) {
        return new MercadoPagoPago(
            99L,
            status,
            "accredited",
            new BigDecimal(monto),
            "ARS",
            "COMPRA-1",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    }
}
