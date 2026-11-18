package com.temipora.backend_springboot.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.temipora.backend_springboot.modelo.CambioAuditoriaDTO;
import com.temipora.backend_springboot.modelo.ContenidoSensible;
import com.temipora.backend_springboot.modelo.EstadoVariante;
import com.temipora.backend_springboot.modelo.LicenciaProducto;
import com.temipora.backend_springboot.modelo.Medio;
import com.temipora.backend_springboot.modelo.Moneda;
import com.temipora.backend_springboot.modelo.Producto;
import com.temipora.backend_springboot.modelo.ProductoEtiqueta;
import com.temipora.backend_springboot.modelo.ProductoImagen;
import com.temipora.backend_springboot.modelo.ProductoVariante;
import com.temipora.backend_springboot.modelo.TipoProducto;

/**
 * Foto de los datos auditables de un producto antes de modificarlo, incluidas
 * sus variantes, licencia, imágenes, etiquetas y contenidos sensibles. Copia
 * valores, no referencias, porque el producto se muta en el mismo objeto.
 */
public final class SnapshotProducto {

    private static final int MAX_TEXTO = 140;

    private final String titulo;
    private final String descripcion;
    private final String tipoProducto;
    private final String medio;
    private final String moneda;
    private final boolean contenidoSensible;
    private final Set<String> contenidosSensibles;
    private final String estado;
    private final String imagenPortada;
    private final Map<Long, VarianteSnapshot> variantes;
    private final LicenciaSnapshot licencia;
    private final Map<String, Integer> imagenes;
    private final Set<String> etiquetas;

    private SnapshotProducto(
            String titulo,
            String descripcion,
            String tipoProducto,
            String medio,
            String moneda,
            boolean contenidoSensible,
            Set<String> contenidosSensibles,
            String estado,
            String imagenPortada,
            Map<Long, VarianteSnapshot> variantes,
            LicenciaSnapshot licencia,
            Map<String, Integer> imagenes,
            Set<String> etiquetas) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.tipoProducto = tipoProducto;
        this.medio = medio;
        this.moneda = moneda;
        this.contenidoSensible = contenidoSensible;
        this.contenidosSensibles = contenidosSensibles;
        this.estado = estado;
        this.imagenPortada = imagenPortada;
        this.variantes = variantes;
        this.licencia = licencia;
        this.imagenes = imagenes;
        this.etiquetas = etiquetas;
    }

    public static SnapshotProducto de(Producto producto) {
        return new SnapshotProducto(
            limpiar(producto.getTitulo()),
            limpiar(producto.getDescripcion()),
            nombre(producto.getTipoProducto()),
            nombre(producto.getMedio()),
            nombreMoneda(producto.getMoneda()),
            producto.isContenidoSensible(),
            contenidos(producto.getContenidosSensibles()),
            producto.getEstado() != null ? producto.getEstado().name() : null,
            producto.getImagenPortada(),
            variantes(producto.getVariantes()),
            LicenciaSnapshot.de(producto.licenciaVigente()),
            imagenes(producto.getImagenes()),
            etiquetas(producto.getEtiquetas()));
    }

    public List<CambioAuditoriaDTO> diferencias(Producto despues) {
        List<CambioAuditoriaDTO> cambios = new ArrayList<>();
        String codigoMoneda = despues.getMoneda() != null
            ? despues.getMoneda().getCodigo() : null;
        agregarEscalares(cambios, despues);
        agregarVariantes(cambios, despues, codigoMoneda);
        agregarLicencia(cambios, despues);
        agregarEtiquetas(cambios, despues);
        agregarImagenes(cambios, despues);
        agregarContenidosSensibles(cambios, despues);
        return cambios;
    }

    private void agregarEscalares(List<CambioAuditoriaDTO> cambios, Producto despues) {
        agregarTexto(cambios, "titulo", "Título",
            titulo, limpiar(despues.getTitulo()));
        agregarTexto(cambios, "descripcion", "Descripción",
            descripcion, limpiar(despues.getDescripcion()));
        agregarSimple(cambios, "tipoProducto", "Tipo de producto",
            tipoProducto, nombre(despues.getTipoProducto()));
        agregarSimple(cambios, "medio", "Medio",
            medio, nombre(despues.getMedio()));
        agregarSimple(cambios, "moneda", "Moneda",
            moneda, nombreMoneda(despues.getMoneda()));
        agregarSimple(cambios, "contenidoSensible", "Contenido sensible",
            booleano(contenidoSensible), booleano(despues.isContenidoSensible()));
        agregarSimple(cambios, "estado", "Estado",
            estado, despues.getEstado() != null ? despues.getEstado().name() : null);
        agregarSimple(cambios, "imagenPortada", "Imagen de portada",
            imagenPortada, despues.getImagenPortada());
    }

    private void agregarVariantes(
            List<CambioAuditoriaDTO> cambios,
            Producto despues,
            String codigoMoneda) {
        List<ProductoVariante> ordenadas = despues.getVariantes().stream()
            .sorted(Comparator
                .comparing(ProductoVariante::getOrden,
                    Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ProductoVariante::getIdVariante,
                    Comparator.nullsLast(Long::compareTo)))
            .toList();

        Set<Long> presentes = new TreeSet<>();
        for (ProductoVariante variante : ordenadas) {
            Long id = variante.getIdVariante();
            if (id != null) {
                presentes.add(id);
            }
            VarianteSnapshot antes = id != null ? variantes.get(id) : null;
            if (antes == null) {
                cambios.add(new CambioAuditoriaDTO(
                    "variante.nueva." + id,
                    "Variante «" + limpiar(variante.getNombre()) + "»",
                    null, describirAlta(variante, codigoMoneda)));
            } else {
                compararVariante(cambios, id, antes, variante, codigoMoneda);
            }
        }

        for (Map.Entry<Long, VarianteSnapshot> entry : variantes.entrySet()) {
            if (!presentes.contains(entry.getKey())) {
                cambios.add(new CambioAuditoriaDTO(
                    "variante." + entry.getKey() + ".quitada",
                    "Variante «" + entry.getValue().nombre() + "»",
                    null, "quitada"));
            }
        }
    }

    private void compararVariante(
            List<CambioAuditoriaDTO> cambios,
            Long id,
            VarianteSnapshot antes,
            ProductoVariante despues,
            String codigoMoneda) {
        String referencia = antes.nombre();
        String clave = "variante." + id + ".";
        agregarTexto(cambios, clave + "nombre", "Nombre · «" + referencia + "»",
            antes.nombre(), limpiar(despues.getNombre()));
        agregarTexto(cambios, clave + "descripcion",
            "Descripción · «" + referencia + "»",
            antes.descripcion(), limpiar(despues.getDescripcion()));
        agregarMonto(cambios, clave + "precio", "Precio · «" + referencia + "»",
            antes.precio(), despues.getPrecio(), codigoMoneda);
        agregarSimple(cambios, clave + "stock", "Stock · «" + referencia + "»",
            antes.stock(), despues.getStockDisponible());
        agregarEstadoVariante(cambios, clave + "estado",
            "Estado · «" + referencia + "»", antes.estado(),
            despues.getEstado() != null ? despues.getEstado().name() : null);
        agregarSimple(cambios, clave + "numeroEdicion",
            "N° de edición · «" + referencia + "»",
            antes.numeroEdicion(), despues.getNumeroEdicion());
        if (!Objects.equals(antes.sha256(), despues.getSha256())) {
            cambios.add(new CambioAuditoriaDTO(
                clave + "imagen", "Imagen · «" + referencia + "»",
                shaCorto(antes.sha256()), shaCorto(despues.getSha256())));
        }
    }

    private void agregarLicencia(List<CambioAuditoriaDTO> cambios, Producto despues) {
        LicenciaSnapshot actual = LicenciaSnapshot.de(despues.licenciaVigente());
        if (licencia == null && actual == null) {
            return;
        }
        if (licencia == null) {
            cambios.add(new CambioAuditoriaDTO(
                "licencia.version", "Licencia", null, "v" + actual.version()));
            cambios.add(new CambioAuditoriaDTO(
                "licencia.derechos", "Derechos", null, lista(actual.derechos())));
            cambios.add(new CambioAuditoriaDTO(
                "licencia.permiteModificaciones", "Permite modificaciones",
                null, booleano(actual.permiteModificaciones())));
            cambios.add(new CambioAuditoriaDTO(
                "licencia.requiereCredito", "Requiere crédito",
                null, booleano(actual.requiereCredito())));
            cambios.add(new CambioAuditoriaDTO(
                "licencia.regalia", "Regalía", null, porcentaje(actual.regalia())));
            agregarTexto(cambios, "licencia.textoAdicional", "Texto adicional",
                null, actual.textoAdicional());
            return;
        }
        if (actual == null) {
            cambios.add(new CambioAuditoriaDTO(
                "licencia.version", "Licencia", "v" + licencia.version(), null));
            return;
        }
        if (!Objects.equals(licencia.version(), actual.version())) {
            cambios.add(new CambioAuditoriaDTO(
                "licencia.version", "Licencia",
                "v" + licencia.version(), "v" + actual.version()));
        }
        agregarSimple(cambios, "licencia.derechos", "Derechos",
            lista(licencia.derechos()), lista(actual.derechos()));
        agregarSimple(cambios, "licencia.permiteModificaciones",
            "Permite modificaciones",
            booleano(licencia.permiteModificaciones()),
            booleano(actual.permiteModificaciones()));
        agregarSimple(cambios, "licencia.requiereCredito", "Requiere crédito",
            booleano(licencia.requiereCredito()),
            booleano(actual.requiereCredito()));
        if (licencia.regalia().compareTo(actual.regalia()) != 0) {
            cambios.add(new CambioAuditoriaDTO(
                "licencia.regalia", "Regalía",
                porcentaje(licencia.regalia()), porcentaje(actual.regalia())));
        }
        agregarTexto(cambios, "licencia.textoAdicional", "Texto adicional",
            licencia.textoAdicional(), actual.textoAdicional());
    }

    private void agregarEtiquetas(List<CambioAuditoriaDTO> cambios, Producto despues) {
        Set<String> actuales = etiquetas(despues.getEtiquetas());
        Set<String> agregadas = new TreeSet<>(actuales);
        agregadas.removeAll(etiquetas);
        Set<String> quitadas = new TreeSet<>(etiquetas);
        quitadas.removeAll(actuales);
        if (!agregadas.isEmpty()) {
            cambios.add(new CambioAuditoriaDTO(
                "etiquetas.agregadas", "Etiquetas agregadas",
                null, String.join(", ", agregadas)));
        }
        if (!quitadas.isEmpty()) {
            cambios.add(new CambioAuditoriaDTO(
                "etiquetas.quitadas", "Etiquetas quitadas",
                String.join(", ", quitadas), null));
        }
    }

    private void agregarImagenes(List<CambioAuditoriaDTO> cambios, Producto despues) {
        Map<String, Integer> actuales = imagenes(despues.getImagenes());
        actuales.entrySet().stream()
            .filter(entry -> !imagenes.containsKey(entry.getKey()))
            .sorted(Map.Entry.comparingByValue(
                Comparator.nullsLast(Integer::compareTo)))
            .forEach(entry -> cambios.add(new CambioAuditoriaDTO(
                "imagen." + shaCorto(entry.getKey()) + ".agregada",
                "Imagen (orden " + entry.getValue() + ")", null, "agregada")));
        imagenes.entrySet().stream()
            .filter(entry -> !actuales.containsKey(entry.getKey()))
            .sorted(Map.Entry.comparingByValue(
                Comparator.nullsLast(Integer::compareTo)))
            .forEach(entry -> cambios.add(new CambioAuditoriaDTO(
                "imagen." + shaCorto(entry.getKey()) + ".quitada",
                "Imagen (orden " + entry.getValue() + ")", "quitada", null)));
    }

    private void agregarContenidosSensibles(
            List<CambioAuditoriaDTO> cambios, Producto despues) {
        agregarSimple(cambios, "contenidosSensibles",
            "Tipos de contenido sensible",
            lista(contenidosSensibles),
            lista(contenidos(despues.getContenidosSensibles())));
    }

    private void agregarTexto(
            List<CambioAuditoriaDTO> cambios,
            String campo,
            String etiqueta,
            String antes,
            String despues) {
        if (Objects.equals(antes, despues)) {
            return;
        }
        cambios.add(new CambioAuditoriaDTO(
            campo, etiqueta, truncar(antes), truncar(despues)));
    }

    private void agregarSimple(
            List<CambioAuditoriaDTO> cambios,
            String campo,
            String etiqueta,
            Object antes,
            Object despues) {
        if (Objects.equals(antes, despues)) {
            return;
        }
        cambios.add(new CambioAuditoriaDTO(
            campo, etiqueta, texto(antes), texto(despues)));
    }

    private void agregarMonto(
            List<CambioAuditoriaDTO> cambios,
            String campo,
            String etiqueta,
            BigDecimal antes,
            BigDecimal despues,
            String codigoMoneda) {
        if (normalizar(antes).compareTo(normalizar(despues)) == 0) {
            return;
        }
        cambios.add(new CambioAuditoriaDTO(
            campo, etiqueta,
            monto(antes, codigoMoneda), monto(despues, codigoMoneda)));
    }

    private void agregarEstadoVariante(
            List<CambioAuditoriaDTO> cambios,
            String campo,
            String etiqueta,
            String antes,
            String despues) {
        if (Objects.equals(antes, despues)) {
            return;
        }
        String valor = despues;
        if (EstadoVariante.ARCHIVADO.name().equals(despues)
                && !EstadoVariante.ARCHIVADO.name().equals(antes)) {
            valor = despues + " (quitada de la publicación)";
        }
        cambios.add(new CambioAuditoriaDTO(campo, etiqueta, antes, valor));
    }

    private String describirAlta(ProductoVariante variante, String codigoMoneda) {
        String precio = monto(variante.getPrecio(), codigoMoneda);
        String stock = variante.getStockDisponible() != null
            ? "stock " + variante.getStockDisponible() : "stock —";
        String estado = variante.getEstado() != null
            ? variante.getEstado().name() : "—";
        return "agregada"
            + (precio != null ? " · " + precio : "")
            + " · " + stock + " · " + estado;
    }

    private static Map<Long, VarianteSnapshot> variantes(
            Collection<ProductoVariante> valores) {
        Map<Long, VarianteSnapshot> mapa = new LinkedHashMap<>();
        for (ProductoVariante variante : valores) {
            if (variante.getIdVariante() != null) {
                mapa.put(variante.getIdVariante(), VarianteSnapshot.de(variante));
            }
        }
        return mapa;
    }

    private static Map<String, Integer> imagenes(Collection<ProductoImagen> valores) {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        for (ProductoImagen imagen : valores) {
            if (imagen.getSha256() != null) {
                mapa.put(imagen.getSha256(), imagen.getOrden());
            }
        }
        return mapa;
    }

    private static Set<String> etiquetas(Collection<ProductoEtiqueta> valores) {
        Set<String> nombres = new TreeSet<>();
        for (ProductoEtiqueta asignacion : valores) {
            if (asignacion.getEtiqueta() != null
                    && asignacion.getEtiqueta().getNombre() != null) {
                nombres.add(asignacion.getEtiqueta().getNombre());
            }
        }
        return nombres;
    }

    private static Set<String> contenidos(Collection<ContenidoSensible> valores) {
        if (valores == null) {
            return new TreeSet<>();
        }
        return valores.stream()
            .filter(Objects::nonNull)
            .map(Enum::name)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private static String nombre(TipoProducto tipo) {
        return tipo != null ? limpiar(tipo.getNombre()) : null;
    }

    private static String nombre(Medio medio) {
        return medio != null ? limpiar(medio.getNombre()) : null;
    }

    private static String nombreMoneda(Moneda moneda) {
        if (moneda == null) {
            return null;
        }
        String nombre = limpiar(moneda.getNombre());
        return nombre != null ? nombre : limpiar(moneda.getCodigo());
    }

    private static String lista(Set<String> valores) {
        return valores == null || valores.isEmpty()
            ? null : String.join(", ", valores);
    }

    private static String texto(Object valor) {
        return valor != null ? String.valueOf(valor) : null;
    }

    private static String booleano(boolean valor) {
        return valor ? "Sí" : "No";
    }

    private static String monto(BigDecimal valor, String codigoMoneda) {
        if (valor == null) {
            return null;
        }
        String base = valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
        return codigoMoneda != null && !codigoMoneda.isBlank()
            ? base + " " + codigoMoneda : base;
    }

    private static String porcentaje(BigDecimal valor) {
        return normalizar(valor).setScale(2, RoundingMode.HALF_UP).toPlainString()
            + "%";
    }

    private static BigDecimal normalizar(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private static String shaCorto(String sha) {
        if (sha == null || sha.isBlank()) {
            return null;
        }
        return sha.length() <= 8 ? sha : sha.substring(0, 8);
    }

    private static String truncar(String valor) {
        if (valor == null || valor.length() <= MAX_TEXTO) {
            return valor;
        }
        return valor.substring(0, MAX_TEXTO).stripTrailing() + "…";
    }

    private static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private record VarianteSnapshot(
            String nombre,
            String descripcion,
            BigDecimal precio,
            Integer stock,
            String estado,
            Integer numeroEdicion,
            String sha256) {

        static VarianteSnapshot de(ProductoVariante variante) {
            return new VarianteSnapshot(
                limpiar(variante.getNombre()),
                limpiar(variante.getDescripcion()),
                variante.getPrecio(),
                variante.getStockDisponible(),
                variante.getEstado() != null ? variante.getEstado().name() : null,
                variante.getNumeroEdicion(),
                variante.getSha256());
        }
    }

    private record LicenciaSnapshot(
            Integer version,
            Set<String> derechos,
            boolean permiteModificaciones,
            boolean requiereCredito,
            BigDecimal regalia,
            String textoAdicional) {

        static LicenciaSnapshot de(LicenciaProducto licencia) {
            if (licencia == null) {
                return null;
            }
            Set<String> derechos = licencia.getDerechos().stream()
                .map(asignacion -> asignacion.getDerecho() != null
                    ? asignacion.getDerecho().getNombre() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
            return new LicenciaSnapshot(
                licencia.getVersion(),
                derechos,
                licencia.isPermiteModificaciones(),
                licencia.isRequiereCredito(),
                normalizar(licencia.getPorcentajeRegalia()),
                limpiar(licencia.getTextoAdicional()));
        }
    }
}
