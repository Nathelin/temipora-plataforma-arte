package com.temipora.backend_springboot.servicios.pagos.producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.temipora.backend_springboot.modelo.Compra;
import com.temipora.backend_springboot.modelo.CompraGrupo;
import com.temipora.backend_springboot.modelo.CompraItem;
import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.IntentoCobro;
import com.temipora.backend_springboot.servicios.pagos.EstadoCobroProveedor;
import com.temipora.backend_springboot.servicios.pagos.ResultadoCobro;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoClient;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoEstados;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoException;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoOAuthService;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoPago;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoPreferencia;

@Component
@ConditionalOnProperty(name = "pagos.proveedor", havingValue = "mercadopago")
public class ProveedorPagoProductoMercadoPago implements ProveedorPagoProducto {

    public static final String NOMBRE = "MERCADOPAGO";

    private final MercadoPagoClient cliente;
    private final MercadoPagoOAuthService oauthService;
    private final String notificationUrl;
    private final String notificationUrlCompras;
    private final String frontendUrl;
    private final boolean sandbox;
    private final boolean binaryMode;
    private final boolean comisionEnSandbox;
    private final int reservaMinutos;

    public ProveedorPagoProductoMercadoPago(
            MercadoPagoClient cliente,
            MercadoPagoOAuthService oauthService,
            @Value("${pagos.mercadopago.notification-url:}") String notificationUrl,
            @Value("${pagos.mercadopago.notification-url-compras:}") String notificationUrlCompras,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl,
            @Value("${pagos.mercadopago.sandbox:false}") boolean sandbox,
            @Value("${pagos.mercadopago.binary-mode:false}") boolean binaryMode,
            @Value("${pagos.mercadopago.comision-en-sandbox:false}") boolean comisionEnSandbox,
            @Value("${compra.reserva-minutos:30}") int reservaMinutos) {
        this.cliente = cliente;
        this.oauthService = oauthService;
        this.notificationUrl = notificationUrl;
        this.notificationUrlCompras = notificationUrlCompras;
        this.frontendUrl = frontendUrl;
        this.sandbox = sandbox;
        this.binaryMode = binaryMode;
        this.comisionEnSandbox = comisionEnSandbox;
        this.reservaMinutos = reservaMinutos;
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public ResultadoCobro crearCobro(
            IntentoCobro intento, List<CompraGrupo> grupos) {
        if (grupos == null || grupos.size() != 1) {
            throw new MercadoPagoException(
                "El Split de Pagos 1:N todavía no está habilitado. "
                    + "Se requiere un grupo por preferencia.");
        }
        CompraGrupo grupo = grupos.get(0);
        Compra compra = intento.getCompra();
        String urlNotificacion = urlNotificacion();
        if (urlNotificacion == null || urlNotificacion.isBlank()) {
            throw new MercadoPagoException(
                "Falta configurar la URL de webhook de compras "
                    + "(pagos.mercadopago.notification-url-compras).");
        }
        String tokenVendedor = oauthService.tokenVigente(
            grupo.getArtista().getIdUsuario());

        List<Map<String, Object>> items = new ArrayList<>();
        for (CompraItem item : grupo.getItems()) {
            Map<String, Object> linea = new LinkedHashMap<>();
            linea.put("title", item.getTituloProducto()
                + (item.getNombreVariante() != null
                    ? " - " + item.getNombreVariante() : ""));
            linea.put("description", "Compra en Temipora");
            linea.put("quantity", item.getCantidad());
            linea.put("currency_id", "ARS");
            linea.put("unit_price", item.getPrecioCobro());
            items.add(linea);
        }

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("items", items);
        cuerpo.put("external_reference", intento.getExternalReference());
        cuerpo.put("notification_url", urlNotificacion);
        cuerpo.put("back_urls", Map.of(
            "success", urlRetorno(compra, "success"),
            "failure", urlRetorno(compra, "failure"),
            "pending", urlRetorno(compra, "pending")));
        if (frontendUrl != null && frontendUrl.startsWith("https://")) {
            cuerpo.put("auto_return", "approved");
        }
        if (binaryMode) {
            cuerpo.put("binary_mode", true);
        }
        cuerpo.put("metadata", Map.of(
            "idCompra", compra.getIdCompra(),
            "idIntentoCobro", intento.getIdIntentoCobro(),
            "idCompraGrupo", grupo.getIdCompraGrupo()));
        if (enviarComision(grupo)) {
            cuerpo.put("marketplace_fee", grupo.getComision());
        }

        LocalDateTime expira = expiracion(intento);
        cuerpo.put("expiration_date_to", expira
            .atZone(ZoneId.systemDefault())
            .toOffsetDateTime()
            .truncatedTo(ChronoUnit.SECONDS)
            .toString());

        MercadoPagoPreferencia preferencia = cliente.crearPreferencia(
            cuerpo, intento.getClaveIdempotencia(), tokenVendedor);
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
    public Optional<EstadoCobroProveedor> consultarEstado(IntentoCobro intento) {
        if (intento.getExternalReference() == null) {
            return Optional.empty();
        }
        String tokenVendedor = tokenVendedor(intento);
        List<MercadoPagoPago> pagos = tokenVendedor != null
            ? cliente.buscarPagos(intento.getExternalReference(), tokenVendedor)
            : cliente.buscarPagos(intento.getExternalReference());
        if (pagos.isEmpty() && tokenVendedor != null) {
            pagos = cliente.buscarPagos(intento.getExternalReference());
        }
        if (pagos.isEmpty()) {
            return Optional.empty();
        }
        MercadoPagoPago pagoMp = pagos.stream()
            .filter(candidato -> "approved".equals(candidato.status()))
            .findFirst()
            .orElse(pagos.get(0));
        verificarMontoYMoneda(pagoMp, intento);
        EstadoPago estado = MercadoPagoEstados.mapear(pagoMp.status());
        return Optional.of(new EstadoCobroProveedor(
            estado,
            pagoMp.id() != null ? String.valueOf(pagoMp.id()) : null,
            pagoMp.status(),
            pagoMp.statusDetail(),
            MercadoPagoEstados.datos(pagoMp)));
    }

    private String tokenVendedor(IntentoCobro intento) {
        try {
            if (intento.getGrupos().isEmpty()) {
                return null;
            }
            Long idArtista = intento.getGrupos().get(0)
                .getGrupo().getArtista().getIdUsuario();
            return oauthService.tokenVigente(idArtista);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void verificarMontoYMoneda(MercadoPagoPago pagoMp, IntentoCobro intento) {
        if (pagoMp.transactionAmount() == null
                || intento.getMonto() == null
                || pagoMp.transactionAmount().compareTo(intento.getMonto()) != 0) {
            throw new MercadoPagoException(
                "El monto del pago " + pagoMp.id() + " no coincide con el esperado.");
        }
        if (pagoMp.currencyId() != null
                && intento.getMoneda() != null
                && !pagoMp.currencyId().equalsIgnoreCase(
                    intento.getMoneda().getCodigo())) {
            throw new MercadoPagoException(
                "La moneda del pago " + pagoMp.id() + " no coincide.");
        }
    }

    private boolean enviarComision(CompraGrupo grupo) {
        BigDecimal comision = grupo.getComision();
        return comision != null && comision.signum() > 0
            && (!sandbox || comisionEnSandbox);
    }

    private String urlRetorno(Compra compra, String resultado) {
        return baseFrontend() + "/compras/" + compra.getIdCompra()
            + "?pago=" + resultado;
    }

    private String baseFrontend() {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            return "http://localhost:5173";
        }
        return frontendUrl.replaceAll("/+$", "");
    }

    private String urlNotificacion() {
        if (notificationUrlCompras != null && !notificationUrlCompras.isBlank()) {
            return notificationUrlCompras;
        }
        if (notificationUrl == null || notificationUrl.isBlank()) {
            return null;
        }
        return notificationUrl.replaceAll("/webhook/?$", "/webhook-compras");
    }

    private LocalDateTime expiracion(IntentoCobro intento) {
        LocalDateTime base = intento.getFechaCreacion() != null
            ? intento.getFechaCreacion() : LocalDateTime.now();
        return base.plusMinutes(reservaMinutos);
    }
}
