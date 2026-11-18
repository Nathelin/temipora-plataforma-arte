package com.temipora.backend_springboot.servicios;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoOAuthService;

/**
 * Resuelve si un usuario puede cobrar una venta.
 * Hoy depende de la cuenta de MercadoPago; cuando se habilite el Split de Pagos
 * 1:N, acá se consulta el consentimiento ACTIVE sin tocar el flujo de reventa.
 */
@Service
public class CobroCapacidadService {

    private final MercadoPagoOAuthService oauthService;
    private final String proveedorPago;

    public CobroCapacidadService(
            MercadoPagoOAuthService oauthService,
            @Value("${pagos.proveedor:simulado}") String proveedorPago) {
        this.oauthService = oauthService;
        this.proveedorPago = proveedorPago;
    }

    public boolean puedeCobrar(Long idUsuario) {
        if (idUsuario == null) {
            return false;
        }
        if (!"mercadopago".equalsIgnoreCase(proveedorPago)) {
            return true;
        }
        return oauthService.estado(idUsuario).conectada();
    }
}
