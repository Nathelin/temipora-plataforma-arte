package com.temipora.backend_springboot.controladores;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoSignatureValidator;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoWebhookService;

@RestController
@RequestMapping("/api/pagos/mercadopago")
@ConditionalOnProperty(name = "pagos.proveedor", havingValue = "mercadopago")
public class MercadoPagoWebhookController {

    private final MercadoPagoWebhookService webhookService;
    private final MercadoPagoSignatureValidator signatureValidator;

    public MercadoPagoWebhookController(
            MercadoPagoWebhookService webhookService,
            MercadoPagoSignatureValidator signatureValidator) {
        this.webhookService = webhookService;
        this.signatureValidator = signatureValidator;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> recibir(
            @RequestParam(required = false) String type,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String id,
            @RequestHeader(name = "x-signature", required = false) String firma,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            @RequestBody(required = false) Map<String, Object> cuerpo) {

        String tipo = type != null ? type : topic;
        String idEvento = dataId != null ? dataId : id;
        if (cuerpo != null) {
            if (tipo == null && cuerpo.get("type") instanceof String valor) {
                tipo = valor;
            }
            if (idEvento == null
                    && cuerpo.get("data") instanceof Map<?, ?> datos
                    && datos.get("id") != null) {
                idEvento = String.valueOf(datos.get("id"));
            }
        }

        if (!signatureValidator.validar(idEvento, requestId, firma)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if ("payment".equalsIgnoreCase(tipo) && idEvento != null) {
            webhookService.procesarPago(idEvento);
        }
        return ResponseEntity.ok().build();
    }
}
