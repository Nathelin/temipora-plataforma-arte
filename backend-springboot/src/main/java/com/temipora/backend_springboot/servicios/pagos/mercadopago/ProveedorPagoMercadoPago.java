package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.modelo.HitoPedido;
import com.temipora.backend_springboot.modelo.Pago;
import com.temipora.backend_springboot.modelo.Pedido;
import com.temipora.backend_springboot.servicios.pagos.EstadoCobroProveedor;
import com.temipora.backend_springboot.servicios.pagos.ProveedorPago;
import com.temipora.backend_springboot.servicios.pagos.ResultadoCobro;

@Component
@ConditionalOnProperty(name = "pagos.proveedor", havingValue = "mercadopago")
public class ProveedorPagoMercadoPago implements ProveedorPago {

    public static final String NOMBRE = "MERCADOPAGO";

    private final MercadoPagoClient cliente;
    private final MercadoPagoOAuthService oauthService;
    private final String notificationUrl;
    private final String frontendUrl;
    private final boolean sandbox;

    public ProveedorPagoMercadoPago(
            MercadoPagoClient cliente,
            MercadoPagoOAuthService oauthService,
            @Value("${pagos.mercadopago.notification-url:}") String notificationUrl,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl,
            @Value("${pagos.mercadopago.sandbox:true}") boolean sandbox) {
        this.cliente = cliente;
        this.oauthService = oauthService;
        this.notificationUrl = notificationUrl;
        this.frontendUrl = frontendUrl;
        this.sandbox = sandbox;
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public ResultadoCobro crearCobro(Pago pago) {
        if (notificationUrl == null || notificationUrl.isBlank()) {
            throw new MercadoPagoException(
                "Falta configurar pagos.mercadopago.notification-url (MP_NOTIFICATION_URL).");
        }
        HitoPedido hito = pago.getHitoPedido();
        Pedido pedido = hito.getPedido();
        // En Marketplace la preferencia se crea con el token del vendedor.
        String tokenVendedor = oauthService.tokenVigente(
            pedido.getArtista().getIdUsuario());
        String nombreServicio = pedido.getServicio() != null
            && pedido.getServicio().getNombre() != null
                ? pedido.getServicio().getNombre()
                : "tu pedido";

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("items", List.of(Map.of(
            "title", "Pedido " + pedido.getIdPedido() + " - " + hito.getNombre(),
            "description", "Hito " + hito.getOrden() + " de " + nombreServicio,
            "quantity", 1,
            "currency_id", "ARS",
            "unit_price", pago.getMontoCobro())));
        cuerpo.put("external_reference", String.valueOf(pago.getIdPago()));
        cuerpo.put("notification_url", notificationUrl);
        cuerpo.put("back_urls", Map.of(
            "success", urlRetorno(pedido, "success"),
            "failure", urlRetorno(pedido, "failure"),
            "pending", urlRetorno(pedido, "pending")));
        // MercadoPago solo respeta auto_return con back_urls HTTPS.
        if (frontendUrl != null && frontendUrl.startsWith("https://")) {
            cuerpo.put("auto_return", "approved");
        }
        cuerpo.put("binary_mode", true);
        cuerpo.put("metadata", Map.of(
            "idPago", pago.getIdPago(),
            "idPedido", pedido.getIdPedido(),
            "idHitoPedido", hito.getIdHitoPedido()));
        if (hito.getComisionMontoCobro() != null
                && hito.getComisionMontoCobro().signum() > 0) {
            cuerpo.put("marketplace_fee", hito.getComisionMontoCobro());
        }

        LocalDateTime expira = expiracion(hito);
        cuerpo.put("expiration_date_to", expira
            .atZone(ZoneId.systemDefault())
            .toOffsetDateTime()
            .truncatedTo(ChronoUnit.SECONDS)
            .toString());

        // Cada intento de cobro genera una preferencia nueva. La clave del
        // pago se conserva como prefijo para poder rastrearlo.
        String clavePreferencia =
            pago.getClaveIdempotencia() + "-" + UUID.randomUUID();
        MercadoPagoPreferencia preferencia = cliente.crearPreferencia(
            cuerpo, clavePreferencia, tokenVendedor);
        String url = sandbox
            && preferencia.sandboxInitPoint() != null
            && !preferencia.sandboxInitPoint().isBlank()
                ? preferencia.sandboxInitPoint()
                : preferencia.initPoint();
        if (url == null || url.isBlank()) {
            throw new MercadoPagoException(
                "MercadoPago no devolvió una URL de checkout.");
        }
        return new ResultadoCobro(preferencia.id(), url, expira);
    }

    @Override
    public Optional<EstadoCobroProveedor> consultarEstado(Pago pago) {
        if (pago.getIdPago() == null) {
            return Optional.empty();
        }
        String token = tokenVendedor(pago);
        List<MercadoPagoPago> pagos = token != null
            ? cliente.buscarPagos(String.valueOf(pago.getIdPago()), token)
            : cliente.buscarPagos(String.valueOf(pago.getIdPago()));
        if (pagos.isEmpty()) {
            return Optional.empty();
        }
        MercadoPagoPago pagoMp = pagos.stream()
            .filter(candidato -> "approved".equals(candidato.status()))
            .findFirst()
            .orElse(pagos.get(0));
        MercadoPagoEstados.verificarMontoYMoneda(pagoMp, pago);
        return Optional.of(new EstadoCobroProveedor(
            MercadoPagoEstados.mapear(pagoMp.status()),
            pagoMp.id() != null ? String.valueOf(pagoMp.id()) : null,
            pagoMp.status(),
            pagoMp.statusDetail()));
    }

    private String tokenVendedor(Pago pago) {
        try {
            return oauthService.tokenVigente(
                pago.getHitoPedido().getPedido().getArtista().getIdUsuario());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String urlRetorno(Pedido pedido, String resultado) {
        return frontendUrl + "/pedidos/" + pedido.getIdPedido()
            + "?pago=" + resultado;
    }

    private LocalDateTime expiracion(HitoPedido hito) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = hito.getFechaLimitePago();
        return limite != null && limite.isAfter(ahora)
            ? limite
            : ahora.plusHours(24);
    }
}
