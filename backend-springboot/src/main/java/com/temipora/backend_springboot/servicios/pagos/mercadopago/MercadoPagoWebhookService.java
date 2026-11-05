package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.Pago;
import com.temipora.backend_springboot.repositorios.PagoRepository;
import com.temipora.backend_springboot.servicios.PagoService;

@Service
@ConditionalOnProperty(name = "pagos.proveedor", havingValue = "mercadopago")
public class MercadoPagoWebhookService {

    private static final Logger log =
        LoggerFactory.getLogger(MercadoPagoWebhookService.class);

    private final MercadoPagoClient cliente;
    private final MercadoPagoOAuthService oauthService;
    private final PagoRepository pagoRepository;
    private final PagoService pagoService;

    public MercadoPagoWebhookService(
            MercadoPagoClient cliente,
            MercadoPagoOAuthService oauthService,
            PagoRepository pagoRepository,
            PagoService pagoService) {
        this.cliente = cliente;
        this.oauthService = oauthService;
        this.pagoRepository = pagoRepository;
        this.pagoService = pagoService;
    }

    public void procesarPago(String idPagoExterno) {
        log.info("Webhook de MercadoPago: pago {}", idPagoExterno);
        MercadoPagoPago pagoMp = consultarPago(idPagoExterno);
        Long idPago = idPagoLocal(pagoMp.externalReference());
        if (idPago == null) {
            return;
        }
        Pago pago = pagoRepository.findById(idPago).orElse(null);
        if (pago == null) {
            return;
        }
        MercadoPagoEstados.verificarMontoYMoneda(pagoMp, pago);
        pagoService.aplicarEstadoProveedor(
            idPago,
            MercadoPagoEstados.mapear(pagoMp.status()),
            pagoMp.id() != null ? String.valueOf(pagoMp.id()) : idPagoExterno,
            pagoMp.status(),
            pagoMp.statusDetail(),
            MercadoPagoEstados.datos(pagoMp));
    }

    /**
     * En marketplace el pago pertenece al vendedor, así que el token de la
     * plataforma no siempre puede leerlo. Se reintenta con el token de cada
     * cuenta conectada hasta encontrar la que cobró.
     */
    private MercadoPagoPago consultarPago(String idPagoExterno) {
        try {
            return cliente.consultarPago(idPagoExterno);
        } catch (MercadoPagoException e) {
            log.warn("El token de la plataforma no pudo leer el pago {} ({}); "
                + "se prueba con el token de cada artista conectado.",
                idPagoExterno, e.getMessage());
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

    private Long idPagoLocal(String externalReference) {
        if (externalReference == null || externalReference.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(externalReference);
        } catch (NumberFormatException e) {
            throw new MercadoPagoException(
                "external_reference inválido: " + externalReference, e);
        }
    }

}
