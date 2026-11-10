package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EventoLedger;
import com.temipora.backend_springboot.modelo.EventoLedgerDTO;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoLedgerDTO;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.TipoEventoLedger;
import com.temipora.backend_springboot.modelo.Usuario;
import com.temipora.backend_springboot.modelo.VarianteLedgerDTO;
import com.temipora.backend_springboot.repositorios.EventoLedgerRepository;

@Service
public class LedgerService {

    private final EventoLedgerRepository eventoRepository;

    public LedgerService(EventoLedgerRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    /**
     * Crea el evento inicial de una variante única. Es idempotente: si el
     * génesis ya existe, devuelve el registrado. Se ejecuta dentro de la
     * transacción del alta para que un fallo revierta todo el producto.
     */
    @Transactional
    public EventoLedger registrarGenesis(ProductoVariante variante, String hashLicencia) {
        if (variante == null || variante.getIdVariante() == null) {
            throw new IllegalArgumentException(
                "No se puede registrar el génesis de una variante sin guardar.");
        }
        if (!variante.esUnica()) {
            throw new IllegalArgumentException(
                "Solo las variantes con stock 1 tienen registro de autenticidad.");
        }
        Optional<EventoLedger> existente = eventoRepository
            .findTopByVarianteIdVarianteOrderBySecuenciaDesc(variante.getIdVariante());
        if (existente.isPresent() && existente.get().getSecuencia() == 1L
                && existente.get().getTipo() == TipoEventoLedger.GENESIS) {
            return existente.get();
        }

        Producto producto = variante.getProducto();
        Usuario artista = producto != null ? producto.getArtista() : null;
        if (artista == null) {
            throw new IllegalStateException(
                "La variante no tiene un artista asociado para registrar el génesis.");
        }
        Moneda moneda = producto.getMoneda();
        LocalDateTime ocurrido = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        String hashPrevio = "";
        String hashImagen = variante.getSha256();
        String datos = datosGenesis(producto, variante, artista, hashLicencia, hashImagen);
        String hashDatos = sha256Hex(datos);

        EventoLedger evento = new EventoLedger();
        evento.setVariante(variante);
        evento.setSecuencia(1L);
        evento.setTipo(TipoEventoLedger.GENESIS);
        evento.setActorOrigen(artista);
        evento.setActorDestino(artista);
        evento.setMoneda(moneda);
        evento.setOcurridoEn(ocurrido);
        evento.setHashPrevio(hashPrevio);
        evento.setHashImagen(hashImagen);
        evento.setDatos(datos);
        evento.setVersionHash(EventoLedger.VERSION_ACTUAL);
        evento.setHashDatos(hashDatos);
        evento.setHashEvento(calcularHash(
            EventoLedger.VERSION_ACTUAL,
            variante.getIdVariante(),
            1L,
            TipoEventoLedger.GENESIS,
            null,
            artista.getIdUsuario(),
            artista.getIdUsuario(),
            null,
            moneda != null ? moneda.getCodigo() : null,
            ocurrido,
            hashPrevio,
            hashImagen,
            hashDatos));
        return eventoRepository.save(evento);
    }

    @Transactional(readOnly = true)
    public VarianteLedgerDTO verificarVariante(ProductoVariante variante) {
        if (variante == null || variante.getIdVariante() == null) {
            return VarianteLedgerDTO.builder()
                .tieneLedger(false)
                .integridad(true)
                .mensaje("Pieza sin historial.")
                .eventos(List.of())
                .build();
        }

        List<EventoLedger> eventos = eventoRepository
            .findByVarianteIdVarianteOrderBySecuenciaAsc(variante.getIdVariante());
        if (eventos.isEmpty()) {
            return VarianteLedgerDTO.builder()
                .idVariante(variante.getIdVariante())
                .nombre(variante.getNombre())
                .tieneLedger(false)
                .integridad(true)
                .mensaje("Esta variante no tiene historial porque no es una pieza única.")
                .eventos(List.of())
                .build();
        }

        boolean integridad = true;
        String mensaje = "Historial verificado.";
        String hashPrevioEsperado = "";
        List<EventoLedgerDTO> dtos = new ArrayList<>();
        for (int i = 0; i < eventos.size(); i++) {
            EventoLedger evento = eventos.get(i);
            boolean secuenciaOk = evento.getSecuencia() != null
                && evento.getSecuencia() == (i + 1L);
            boolean previoOk = Objects.equals(evento.getHashPrevio(), hashPrevioEsperado);
            boolean hashOk = Objects.equals(
                evento.getHashEvento(), recomputarHash(evento));
            boolean datosOk = hashDatosCoincide(evento);
            boolean ok = secuenciaOk && previoOk && hashOk && datosOk;
            if (!ok && integridad) {
                integridad = false;
                mensaje = !datosOk
                    ? "El contenido del registro " + evento.getSecuencia()
                        + " fue alterado: el hash de datos no coincide."
                    : "El historial no coincide en el registro "
                        + evento.getSecuencia() + ".";
            }
            dtos.add(aDTO(evento, ok));
            hashPrevioEsperado = evento.getHashEvento();
        }

        return VarianteLedgerDTO.builder()
            .idVariante(variante.getIdVariante())
            .nombre(variante.getNombre())
            .tieneLedger(true)
            .integridad(integridad)
            .mensaje(mensaje)
            .eventos(dtos)
            .build();
    }

    @Transactional(readOnly = true)
    public ProductoLedgerDTO verificarProducto(Producto producto) {
        List<VarianteLedgerDTO> variantes = producto.getVariantes().stream()
            .map(this::verificarVariante)
            .toList();
        boolean integridad = variantes.stream()
            .allMatch(VarianteLedgerDTO::isIntegridad);
        String mensaje = integridad
            ? "El historial de todas las piezas coincide."
            : "Hay al menos una pieza con el historial alterado.";
        return ProductoLedgerDTO.builder()
            .idProducto(producto.getIdProducto())
            .titulo(producto.getTitulo())
            .integridad(integridad)
            .mensaje(mensaje)
            .variantes(variantes)
            .build();
    }

    @Transactional(readOnly = true)
    public ProductoLedgerDTO verificarProducto(
            Producto producto, int pagina, int tamano) {
        ProductoLedgerDTO completo = verificarProducto(producto);
        List<VarianteLedgerDTO> registros = completo.getVariantes().stream()
            .filter(VarianteLedgerDTO::isTieneLedger)
            .toList();
        int tamanoSeguro = Math.min(Math.max(tamano, 1), 50);
        int total = registros.size();
        int totalPaginas = total == 0
            ? 0
            : (int) Math.ceil((double) total / tamanoSeguro);
        int paginaSegura = totalPaginas == 0
            ? 1
            : Math.min(Math.max(pagina, 1), totalPaginas);
        int desde = total == 0
            ? 0
            : Math.min((paginaSegura - 1) * tamanoSeguro, total);
        int hasta = total == 0
            ? 0
            : Math.min(desde + tamanoSeguro, total);

        return ProductoLedgerDTO.builder()
            .idProducto(completo.getIdProducto())
            .titulo(completo.getTitulo())
            .integridad(completo.isIntegridad())
            .mensaje(completo.getMensaje())
            .variantes(registros.subList(desde, hasta))
            .pagina(paginaSegura)
            .tamano(tamanoSeguro)
            .totalElementos(total)
            .totalPaginas(totalPaginas)
            .build();
    }

    /**
     * Recalcula el hash desde el contenido actual de la fila. Para la versión 2
     * se vuelve a hashear la columna `datos` en vez de confiar en `hash_datos`:
     * si alguien reescribe `datos` sin tocar `hash_datos`, el hash del evento
     * deja de coincidir.
     */
    private String recomputarHash(EventoLedger evento) {
        int version = evento.getVersionHash() != null
            ? evento.getVersionHash()
            : 1;
        String hashDatos = version >= 2
            ? sha256Hex(evento.getDatos() != null ? evento.getDatos() : "")
            : null;
        return calcularHash(
            version,
            evento.getVariante() != null ? evento.getVariante().getIdVariante() : null,
            evento.getSecuencia(),
            evento.getTipo(),
            evento.getIdVenta(),
            evento.getActorOrigen() != null
                ? evento.getActorOrigen().getIdUsuario() : null,
            evento.getActorDestino() != null
                ? evento.getActorDestino().getIdUsuario() : null,
            evento.getMonto(),
            evento.getMoneda() != null ? evento.getMoneda().getCodigo() : null,
            evento.getOcurridoEn(),
            evento.getHashPrevio(),
            evento.getHashImagen(),
            hashDatos);
    }

    /**
     * Comprueba que `hash_datos` corresponda al contenido guardado en `datos`.
     * Los eventos de versión 1 no protegían ese campo, así que no se validan.
     */
    private boolean hashDatosCoincide(EventoLedger evento) {
        int version = evento.getVersionHash() != null
            ? evento.getVersionHash()
            : 1;
        if (version < 2) {
            return true;
        }
        String esperado = sha256Hex(evento.getDatos() != null ? evento.getDatos() : "");
        return Objects.equals(evento.getHashDatos(), esperado);
    }

    private String calcularHash(
            int version,
            Long idVariante,
            Long secuencia,
            TipoEventoLedger tipo,
            Long idVenta,
            Long idOrigen,
            Long idDestino,
            BigDecimal monto,
            String moneda,
            LocalDateTime ocurrido,
            String hashPrevio,
            String hashImagen,
            String hashDatos) {
        String canonico = String.join("|",
            String.valueOf(idVariante),
            String.valueOf(secuencia),
            tipo != null ? tipo.name() : "",
            idVenta != null ? String.valueOf(idVenta) : "",
            idOrigen != null ? String.valueOf(idOrigen) : "",
            idDestino != null ? String.valueOf(idDestino) : "",
            monto != null ? monto.stripTrailingZeros().toPlainString() : "",
            moneda != null ? moneda : "",
            ocurrido != null
                ? String.valueOf(ocurrido.toInstant(ZoneOffset.UTC).toEpochMilli())
                : "",
            hashPrevio != null ? hashPrevio : "",
            hashImagen != null ? hashImagen : "",
            // Desde la versión 2 el hash cubre también el contenido de `datos`.
            version >= 2 ? (hashDatos != null ? hashDatos : "") : "");
        return sha256Hex(canonico);
    }

    private String datosGenesis(
            Producto producto,
            ProductoVariante variante,
            Usuario artista,
            String hashLicencia,
            String hashImagen) {
        return "{\"titulo\":" + json(producto != null ? producto.getTitulo() : null)
            + ",\"variante\":" + json(variante.getNombre())
            + ",\"artista\":" + json(artista != null ? artista.getNombreUsuario() : null)
            + ",\"sha256\":" + json(hashImagen)
            + ",\"licencia\":" + json(hashLicencia)
            + "}";
    }

    private EventoLedgerDTO aDTO(EventoLedger evento, boolean verificado) {
        return EventoLedgerDTO.builder()
            .idEventoLedger(evento.getIdEventoLedger())
            .secuencia(evento.getSecuencia())
            .tipo(evento.getTipo())
            .ocurridoEn(evento.getOcurridoEn())
            .idActorOrigen(evento.getActorOrigen() != null
                ? evento.getActorOrigen().getIdUsuario() : null)
            .nombreOrigen(evento.getActorOrigen() != null
                ? evento.getActorOrigen().getNombreUsuario() : null)
            .idActorDestino(evento.getActorDestino() != null
                ? evento.getActorDestino().getIdUsuario() : null)
            .nombreDestino(evento.getActorDestino() != null
                ? evento.getActorDestino().getNombreUsuario() : null)
            .hashPrevio(evento.getHashPrevio())
            .hashEvento(evento.getHashEvento())
            .versionHash(evento.getVersionHash())
            .verificado(verificado)
            .build();
    }

    private String sha256Hex(String valor) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible.", e);
        }
    }

    private String json(String valor) {
        if (valor == null) {
            return "null";
        }
        StringBuilder salida = new StringBuilder("\"");
        for (char caracter : valor.toCharArray()) {
            switch (caracter) {
                case '\\' -> salida.append("\\\\");
                case '"' -> salida.append("\\\"");
                case '\n' -> salida.append("\\n");
                case '\r' -> salida.append("\\r");
                case '\t' -> salida.append("\\t");
                default -> {
                    if (caracter < 0x20) {
                        salida.append(String.format("\\u%04x", (int) caracter));
                    } else {
                        salida.append(caracter);
                    }
                }
            }
        }
        return salida.append('"').toString();
    }
}
