package com.temipora.backend_springboot.servicios.opentimestamps;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.servicios.ProveedorSellado;
import com.temipora.backend_springboot.servicios.opentimestamps.OtsCodec.Detached;
import com.temipora.backend_springboot.servicios.opentimestamps.OtsCodec.SelloBitcoin;

/**
 * Sellado real en la blockchain de Bitcoin con OpenTimestamps.
 *
 * Se envía el digest a los calendarios públicos (que agrupan miles de hashes
 * en una sola transacción de Bitcoin) y se guarda el archivo .ots resultante.
 * La confirmación llega cuando el lote fue incluido en un bloque; allí se
 * obtiene la altura del bloque y la raíz de Merkle del bloque (el "compromiso"
 * atestiguado).
 */
@Service
public class ProveedorOpenTimestamps implements ProveedorSellado {

    private static final Logger log = LoggerFactory.getLogger(ProveedorOpenTimestamps.class);

    private static final String CALENDARIOS_POR_DEFECTO =
        "https://alice.btc.calendar.opentimestamps.org,"
            + "https://bob.btc.calendar.opentimestamps.org,"
            + "https://finney.calendar.eternitywall.com";

    private final OpenTimestampsClient client;
    private final List<String> calendarios;

    public ProveedorOpenTimestamps(
            OpenTimestampsClient client,
            @Value("${temipora.anclaje.calendario-url:" + CALENDARIOS_POR_DEFECTO + "}")
            String urlsConfiguradas) {
        this.client = client;
        this.calendarios = new ArrayList<>();
        for (String url : urlsConfiguradas.split(",")) {
            String limpia = url.trim();
            if (!limpia.isEmpty()) {
                calendarios.add(limpia);
            }
        }
    }

    @Override
    public ResultadoSellado sellar(byte[] digest) {
        OtsTimestamp raiz = new OtsTimestamp(digest);
        List<String> usados = new ArrayList<>();
        for (String calendario : calendarios) {
            Optional<byte[]> respuesta = client.sellar(calendario, digest);
            if (respuesta.isEmpty()) {
                continue;
            }
            try {
                OtsTimestamp parcial = OtsCodec.deserializar(respuesta.get(), digest);
                OtsCodec.merge(raiz, parcial);
                usados.add(calendario);
            } catch (OtsException e) {
                log.warn("Respuesta invalida del calendario {}: {}", calendario, e.getMessage());
            }
        }
        if (usados.isEmpty()) {
            throw new OtsException(
                "Ningún calendario de OpenTimestamps pudo sellar el anclaje.");
        }
        byte[] cuerpo = OtsCodec.serializar(raiz);
        byte[] ots = OtsCodec.archivoDetached(digest, cuerpo);
        return new ResultadoSellado(ots, String.join(", ", usados));
    }

    @Override
    public Optional<ResultadoConfirmacion> confirmar(byte[] pruebaOts) {
        Detached detached;
        try {
            detached = OtsCodec.leerDetached(pruebaOts);
        } catch (OtsException e) {
            log.warn("Prueba OpenTimestamps invalida: {}", e.getMessage());
            return Optional.empty();
        }
        OtsTimestamp raiz = OtsCodec.deserializar(detached.cuerpo(), detached.digest());

        boolean actualizada = false;
        for (OtsAttestation pendiente : OtsCodec.atestacionesPendientes(raiz)) {
            byte[] compromiso = OtsCodec.mensajeDe(raiz, pendiente);
            if (compromiso == null) {
                continue;
            }
            Optional<byte[]> respuesta = client.actualizar(pendiente.uri(), compromiso);
            if (respuesta.isEmpty()) {
                continue;
            }
            try {
                OtsCodec.merge(raiz, OtsCodec.deserializar(respuesta.get(), compromiso));
                actualizada = true;
            } catch (OtsException e) {
                log.debug("No se pudo incorporar la actualización: {}", e.getMessage());
            }
        }

        Optional<SelloBitcoin> bitcoin = OtsCodec.buscarBitcoin(raiz);
        if (bitcoin.isEmpty()) {
            return Optional.empty();
        }
        byte[] pruebaFinal = pruebaOts;
        if (actualizada) {
            byte[] cuerpo = OtsCodec.serializar(raiz);
            pruebaFinal = OtsCodec.archivoDetached(detached.digest(), cuerpo);
        }
        return Optional.of(new ResultadoConfirmacion(
            pruebaFinal, bitcoin.get().hashBloqueMerkle(), bitcoin.get().altura()));
    }
}
