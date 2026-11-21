package com.temipora.backend_springboot.servicios;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.modelo.ConciliacionResultadoDTO;
import com.temipora.backend_springboot.modelo.EstadoIntentoCobro;
import com.temipora.backend_springboot.modelo.EstadoPago;
import com.temipora.backend_springboot.repositorios.IntentoCobroRepository;
import com.temipora.backend_springboot.repositorios.PagoRepository;
import com.temipora.backend_springboot.servicios.pagos.DatosPagoProveedor;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoClient;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoEstados;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoException;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoOAuthService;
import com.temipora.backend_springboot.servicios.pagos.mercadopago.MercadoPagoPago;

/**
 * Completa el arancel y el neto liquidado de las operaciones que se aprobaron
 * sin que MercadoPago informara esos importes. Sólo escribe campos vacíos: no
 * modifica montos, comisiones ni estados, y nunca sobrescribe un valor
 * existente. Se ejecuta a pedido del administrador.
 */
@Service
public class ConciliacionMercadoPagoService {

    private static final Logger log =
        LoggerFactory.getLogger(ConciliacionMercadoPagoService.class);

    private final PagoRepository pagoRepository;
    private final IntentoCobroRepository intentoCobroRepository;
    private final MercadoPagoClient cliente;
    private final MercadoPagoOAuthService oauthService;
    private final ConciliacionPersistencia persistencia;

    public ConciliacionMercadoPagoService(
            PagoRepository pagoRepository,
            IntentoCobroRepository intentoCobroRepository,
            MercadoPagoClient cliente,
            MercadoPagoOAuthService oauthService,
            ConciliacionPersistencia persistencia) {
        this.pagoRepository = pagoRepository;
        this.intentoCobroRepository = intentoCobroRepository;
        this.cliente = cliente;
        this.oauthService = oauthService;
        this.persistencia = persistencia;
    }

    public ConciliacionResultadoDTO conciliar() {
        Resumen resumen = new Resumen();

        for (Long idPago : pagoRepository.idsPendientesDeConciliar(
                EstadoPago.APROBADO)) {
            resumen.revisados++;
            procesar(idPago, resumen, true);
        }

        for (Long idIntento : intentoCobroRepository.idsPendientesDeConciliar(
                EstadoIntentoCobro.APROBADO)) {
            resumen.revisados++;
            procesar(idIntento, resumen, false);
        }

        log.info("Conciliación con MercadoPago: {} revisados, {} completados, "
            + "{} sin datos, {} errores", resumen.revisados,
            resumen.completados, resumen.sinDatos, resumen.errores);

        return new ConciliacionResultadoDTO(
            resumen.revisados, resumen.completados, resumen.sinDatos,
            resumen.errores);
    }

    private void procesar(Long id, Resumen resumen, boolean esPago) {
        try {
            String idExterno = esPago
                ? pagoRepository.findById(id)
                    .map(pago -> pago.getIdPagoExterno()).orElse(null)
                : intentoCobroRepository.findById(id)
                    .map(intento -> intento.getIdPagoExterno()).orElse(null);
            if (idExterno == null || idExterno.isBlank()) {
                resumen.sinDatos++;
                return;
            }

            DatosPagoProveedor datos = MercadoPagoEstados.datos(
                consultar(idExterno));
            if (datos == null || (datos.netoRecibido() == null
                    && datos.arancel() == null)) {
                resumen.sinDatos++;
                return;
            }

            boolean completo = esPago
                ? persistencia.completarPago(id, datos)
                : persistencia.completarIntento(id, datos);

            if (completo) {
                resumen.completados++;
            } else {
                resumen.sinDatos++;
            }
        } catch (MercadoPagoException e) {
            resumen.errores++;
            log.warn("No se pudo conciliar la operación {}: {}",
                id, e.getMessage());
        } catch (RuntimeException e) {
            resumen.errores++;
            log.error("Error al conciliar la operación {}", id, e);
        }
    }

    private MercadoPagoPago consultar(String idExterno) {
        try {
            return cliente.consultarPago(idExterno);
        } catch (MercadoPagoException e) {
            for (String token : oauthService.tokensVigentes()) {
                try {
                    return cliente.consultarPago(idExterno, token);
                } catch (MercadoPagoException ignorada) {
                    // El pago puede pertenecer a otro artista: se sigue.
                }
            }
            throw e;
        }
    }

    private static final class Resumen {
        private int revisados;
        private int completados;
        private int sinDatos;
        private int errores;
    }
}
