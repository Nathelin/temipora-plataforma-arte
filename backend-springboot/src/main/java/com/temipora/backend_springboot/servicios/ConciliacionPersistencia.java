package com.temipora.backend_springboot.servicios;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.IntentoCobro;
import com.temipora.backend_springboot.modelo.Pago;
import com.temipora.backend_springboot.repositorios.IntentoCobroRepository;
import com.temipora.backend_springboot.repositorios.PagoRepository;
import com.temipora.backend_springboot.servicios.pagos.DatosPagoProveedor;

/**
 * Escritura transaccional de la conciliación. Cada operación se actualiza en
 * su propia transacción, con su registro de auditoría: si la auditoría falla,
 * no se completa el dato.
 */
@Service
public class ConciliacionPersistencia {

    private static final String ENTIDAD_PAGO = "Pago";
    private static final String ENTIDAD_INTENTO = "Intento de cobro";
    private static final String MOTIVO = "Conciliación manual con MercadoPago";

    private final PagoRepository pagoRepository;
    private final IntentoCobroRepository intentoCobroRepository;
    private final AuditoriaService auditoria;

    public ConciliacionPersistencia(
            PagoRepository pagoRepository,
            IntentoCobroRepository intentoCobroRepository,
            AuditoriaService auditoria) {
        this.pagoRepository = pagoRepository;
        this.intentoCobroRepository = intentoCobroRepository;
        this.auditoria = auditoria;
    }

    @Transactional
    public boolean completarPago(Long idPago, DatosPagoProveedor datos) {
        Pago pago = pagoRepository.findById(idPago).orElse(null);
        if (pago == null) {
            return false;
        }
        boolean completo = false;
        if (pago.getNetoRecibidoMp() == null && datos.netoRecibido() != null) {
            pago.setNetoRecibidoMp(datos.netoRecibido());
            completo = true;
        }
        if (pago.getArancelMp() == null && datos.arancel() != null) {
            pago.setArancelMp(datos.arancel());
            completo = true;
        }
        if (!completo) {
            return false;
        }
        pagoRepository.save(pago);
        auditoria.registrar(AccionesAdmin.PAGO_CONCILIAR, ENTIDAD_PAGO, idPago,
            "Completó importes liquidados por MercadoPago", MOTIVO);
        return true;
    }

    @Transactional
    public boolean completarIntento(Long idIntento, DatosPagoProveedor datos) {
        IntentoCobro intento = intentoCobroRepository.findById(idIntento)
            .orElse(null);
        if (intento == null) {
            return false;
        }
        boolean completo = false;
        if (intento.getNetoRecibidoMp() == null
                && datos.netoRecibido() != null) {
            intento.setNetoRecibidoMp(datos.netoRecibido());
            completo = true;
        }
        if (intento.getArancelMp() == null && datos.arancel() != null) {
            intento.setArancelMp(datos.arancel());
            completo = true;
        }
        if (!completo) {
            return false;
        }
        intentoCobroRepository.save(intento);
        auditoria.registrar(AccionesAdmin.PAGO_CONCILIAR, ENTIDAD_INTENTO,
            idIntento, "Completó importes liquidados por MercadoPago", MOTIVO);
        return true;
    }
}
