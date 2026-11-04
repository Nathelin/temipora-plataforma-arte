package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.Pago;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.modelo.Servicio;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.servicios.pagos.DatosPagoProveedor;
import com.temipora.backend_springboot.servicios.pagos.EstadoCobroProveedor;
import com.temipora.backend_springboot.servicios.pagos.ResultadoCobro;

@ExtendWith(MockitoExtension.class)
class ProveedorPagoMercadoPagoTest {

    private static final String WEBHOOK = "https://temipora.test/webhook";
    private static final String FRONT = "https://temipora.test";

    @Mock
    private MercadoPagoClient cliente;
    @Mock
    private MercadoPagoOAuthService oauthService;

    @Test
    void crearCobro_armaLaPreferenciaYDevuelveElCheckoutSandbox() {
        ProveedorPagoMercadoPago proveedor = new ProveedorPagoMercadoPago(
            cliente, oauthService, WEBHOOK, FRONT, true, false, true);
        when(oauthService.tokenVigente(2L)).thenReturn("seller-token");
        when(cliente.crearPreferencia(any(), anyString(), anyString()))
            .thenReturn(new MercadoPagoPreferencia(
                "pref-1", "https://prod", "https://sandbox"));

        ResultadoCobro resultado = proveedor.crearCobro(nuevoPago());

        assertThat(resultado.idExterno()).isEqualTo("pref-1");
        assertThat(resultado.instruccion()).isEqualTo("https://sandbox");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor =
            ArgumentCaptor.forClass(Map.class);
        verify(cliente).crearPreferencia(
            captor.capture(), anyString(), eq("seller-token"));
        Map<String, Object> cuerpo = captor.getValue();

        assertThat(cuerpo.get("external_reference")).isEqualTo("30");
        assertThat(cuerpo.get("notification_url")).isEqualTo(WEBHOOK);
        assertThat(cuerpo.get("auto_return")).isEqualTo("approved");
        assertThat(cuerpo).doesNotContainKey("binary_mode");
        assertThat(cuerpo.get("back_urls")).isEqualTo(Map.of(
            "success", FRONT + "/pedidos/10?pago=success",
            "failure", FRONT + "/pedidos/10?pago=failure",
            "pending", FRONT + "/pedidos/10?pago=pending"));
        assertThat(cuerpo.get("metadata")).isEqualTo(Map.of(
            "idPago", 30L,
            "idPedido", 10L,
            "idHitoPedido", 20L));
        assertThat(cuerpo.get("marketplace_fee"))
            .isEqualTo(new BigDecimal("5.00"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items =
            (List<Map<String, Object>>) cuerpo.get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("currency_id")).isEqualTo("ARS");
        assertThat(items.get(0).get("quantity")).isEqualTo(1);
        assertThat(items.get(0).get("unit_price"))
            .isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    void crearCobro_enSandbox_omiteLaComision() {
        ProveedorPagoMercadoPago proveedor = new ProveedorPagoMercadoPago(
            cliente, oauthService, WEBHOOK, FRONT, true, true, false);
        when(oauthService.tokenVigente(2L)).thenReturn("seller-token");
        when(cliente.crearPreferencia(any(), anyString(), anyString()))
            .thenReturn(new MercadoPagoPreferencia(
                "pref-1", "https://prod", "https://sandbox"));

        proveedor.crearCobro(nuevoPago());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor =
            ArgumentCaptor.forClass(Map.class);
        verify(cliente).crearPreferencia(
            captor.capture(), anyString(), eq("seller-token"));
        assertThat(captor.getValue()).doesNotContainKey("marketplace_fee");
        assertThat(captor.getValue().get("binary_mode")).isEqualTo(true);
    }

    @Test
    void crearCobro_enProduccion_usaInitPoint() {
        ProveedorPagoMercadoPago proveedor = new ProveedorPagoMercadoPago(
            cliente, oauthService, WEBHOOK, FRONT, false, false, false);
        when(oauthService.tokenVigente(2L)).thenReturn("seller-token");
        when(cliente.crearPreferencia(any(), anyString(), anyString()))
            .thenReturn(new MercadoPagoPreferencia(
                "pref-1", "https://prod", "https://sandbox"));

        ResultadoCobro resultado = proveedor.crearCobro(nuevoPago());

        assertThat(resultado.instruccion()).isEqualTo("https://prod");
    }

    @Test
    void crearCobro_conFrontHttp_omiteAutoReturn() {
        String frontLocal = "http://localhost:5173";
        ProveedorPagoMercadoPago proveedor = new ProveedorPagoMercadoPago(
            cliente, oauthService, WEBHOOK, frontLocal, true, false, false);
        when(oauthService.tokenVigente(2L)).thenReturn("seller-token");
        when(cliente.crearPreferencia(any(), anyString(), anyString()))
            .thenReturn(new MercadoPagoPreferencia(
                "pref-1", "https://prod", "https://sandbox"));

        proveedor.crearCobro(nuevoPago());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor =
            ArgumentCaptor.forClass(Map.class);
        verify(cliente).crearPreferencia(
            captor.capture(), anyString(), eq("seller-token"));
        assertThat(captor.getValue()).doesNotContainKey("auto_return");
    }

    @Test
    void crearCobro_sinNotificationUrl_lanza() {
        ProveedorPagoMercadoPago proveedor = new ProveedorPagoMercadoPago(
            cliente, oauthService, "", FRONT, true, false, false);

        assertThatThrownBy(() -> proveedor.crearCobro(nuevoPago()))
            .isInstanceOf(MercadoPagoException.class)
            .hasMessageContaining("notification-url");
    }

    @Test
    void consultarEstado_aprobado_devuelveElPagoDeMercadoPago() {
        ProveedorPagoMercadoPago proveedor = new ProveedorPagoMercadoPago(
            cliente, oauthService, WEBHOOK, FRONT, true, false, false);
        when(oauthService.tokenVigente(2L)).thenReturn("seller-token");
        when(cliente.buscarPagos("30", "seller-token")).thenReturn(List.of(
            new MercadoPagoPago(
                123L, "approved", "accredited",
                new BigDecimal("50.00"), "ARS", "30", "visa",
                "credit_card", "2026-11-02T10:00:00.000-03:00",
                "2026-11-05T10:00:00.000-03:00", "released",
                new MercadoPagoPago.TransactionDetails(
                    new BigDecimal("45.50"), new BigDecimal("50.00")),
                List.of(
                    new MercadoPagoPago.FeeDetail(
                        "mercadopago_fee", new BigDecimal("4.50"),
                        "collector"),
                    new MercadoPagoPago.FeeDetail(
                        "financing_fee", new BigDecimal("1.00"), "payer")),
                3, new MercadoPagoPago.Card("1234"))));

        Optional<EstadoCobroProveedor> estado =
            proveedor.consultarEstado(nuevoPago());

        assertThat(estado).isPresent();
        assertThat(estado.get().estado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(estado.get().idPagoExterno()).isEqualTo("123");

        DatosPagoProveedor datos = estado.get().datos();
        assertThat(datos).isNotNull();
        assertThat(datos.arancel()).isEqualByComparingTo("4.50");
        assertThat(datos.netoRecibido()).isEqualByComparingTo("45.50");
        assertThat(datos.fechaAprobacion()).isNotNull();
        assertThat(datos.fechaLiberacion()).isNotNull();
        assertThat(datos.medioPago()).isEqualTo("visa");
        assertThat(datos.tipoMedioPago()).isEqualTo("credit_card");
        assertThat(datos.cuotas()).isEqualTo(3);
        assertThat(datos.ultimosCuatro()).isEqualTo("1234");
    }

    @Test
    void nombre_esMercadoPago() {
        ProveedorPagoMercadoPago proveedor = new ProveedorPagoMercadoPago(
            cliente, oauthService, WEBHOOK, FRONT, true, false, false);

        assertThat(proveedor.nombre()).isEqualTo("MERCADOPAGO");
    }

    private Pago nuevoPago() {
        Servicio servicio = new Servicio();
        servicio.setIdServicio(5L);
        servicio.setNombre("Ilustración");

        Usuario artista = new Usuario();
        artista.setIdUsuario(2L);

        Pedido pedido = new Pedido();
        pedido.setIdPedido(10L);
        pedido.setArtista(artista);
        pedido.setServicio(servicio);

        HitoPedido hito = new HitoPedido();
        hito.setIdHitoPedido(20L);
        hito.setOrden(0);
        hito.setNombre("Seña");
        hito.setComisionMontoCobro(new BigDecimal("5.00"));
        pedido.agregarHito(hito);

        Pago pago = new Pago();
        pago.setIdPago(30L);
        pago.setHitoPedido(hito);
        pago.setMontoCobro(new BigDecimal("50.00"));
        pago.setComisionMontoCobro(new BigDecimal("5.00"));
        pago.setClaveIdempotencia("clave-1");
        return pago;
    }
}
