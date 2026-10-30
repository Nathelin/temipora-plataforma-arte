package com.temipora.backend_springboot.servicios.pagos.mercadopago;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.modelo.Pago;
import com.temipora.backend_springboot.repositorios.PagoRepository;
import com.temipora.backend_springboot.servicios.PagoService;

@Service
@ConditionalOnProperty(name = "pagos.proveedor", havingValue = "mercadopago")
public class MercadoPagoWebhookService {

    private final MercadoPagoClient cliente;
    private final PagoRepository pagoRepository;
    private final PagoService pagoService;

    public MercadoPagoWebhookService(
            MercadoPagoClient cliente,
            PagoRepository pagoRepository,
            PagoService pagoService) {
        this.cliente = cliente;
        this.pagoRepository = pagoRepository;
        this.pagoService = pagoService;
    }

    public void procesarPago(String idPagoExterno) {
        MercadoPagoPago pagoMp = cliente.consultarPago(idPagoExterno);
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
            pagoMp.statusDetail());
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
