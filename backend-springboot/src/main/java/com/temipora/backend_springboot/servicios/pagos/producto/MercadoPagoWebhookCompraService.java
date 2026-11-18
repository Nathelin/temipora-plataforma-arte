package com.temipora.backend_springboot.servicios.pagos.producto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.IntentoCobro;
import com.temipora.backend_springboot.repositorios.IntentoCobroRepository;
import com.temipora.backend_springboot.servicios.CompraPagoService;
import com.temipora.backend_springboot.servicios.pagos.EstadoCobroProveedor;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoClient;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoEstados;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoException;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoOAuthService;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoPago;

@Service
@ConditionalOnProperty(name = "pagos.proveedor", havingValue = "mercadopago")
public class MercadoPagoWebhookCompraService {

    private static final Logger log =
        LoggerFactory.getLogger(MercadoPagoWebhookCompraService.class);

    private final MercadoPagoClient cliente;
    private final MercadoPagoOAuthService oauthService;
    private final IntentoCobroRepository intentoCobroRepository;
    private final CompraPagoService compraPagoService;

    public MercadoPagoWebhookCompraService(
            MercadoPagoClient cliente,
            MercadoPagoOAuthService oauthService,
            IntentoCobroRepository intentoCobroRepository,
            CompraPagoService compraPagoService) {
        this.cliente = cliente;
        this.oauthService = oauthService;
        this.intentoCobroRepository = intentoCobroRepository;
        this.compraPagoService = compraPagoService;
    }

    public void procesarPago(String idPagoExterno) {
        log.info("Webhook de MercadoPago (compras): pago {}", idPagoExterno);
        MercadoPagoPago pagoMp = consultarPago(idPagoExterno);
        String referencia = pagoMp.externalReference();
        if (referencia == null || !referencia.startsWith("COMPRA-")) {
            return;
        }
        IntentoCobro intento = intentoCobroRepository
            .findByExternalReference(referencia)
            .orElse(null);
        if (intento == null) {
            return;
        }
        verificarMontoYMoneda(pagoMp, intento);
        EstadoPago estado = MercadoPagoEstados.mapear(pagoMp.status());
        compraPagoService.aplicarEstadoProveedor(
            intento.getIdIntentoCobro(),
            new EstadoCobroProveedor(
                estado,
                pagoMp.id() != null ? String.valueOf(pagoMp.id()) : idPagoExterno,
                pagoMp.status(),
                pagoMp.statusDetail(),
                MercadoPagoEstados.datos(pagoMp)));
    }

    private MercadoPagoPago consultarPago(String idPagoExterno) {
        try {
            return cliente.consultarPago(idPagoExterno);
        } catch (MercadoPagoException e) {
            for (String token : oauthService.tokensVigentes()) {
                try {
                    return cliente.consultarPago(idPagoExterno, token);
                } catch (MercadoPagoException ignorada) {
                    // Puede ser el pago de otro artista: se sigue probando.
                }
            }
            throw e;
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
}
