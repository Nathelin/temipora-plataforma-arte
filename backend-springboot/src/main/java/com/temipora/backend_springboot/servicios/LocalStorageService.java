package com.temipora.backend_springboot.servicios;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    static final int MAX_LADO = 2048;
    static final long MAX_BYTES_PUBLICO = 5L * 1024 * 1024;

    /**
     * Anchos de las copias de visualización. Se guardan en subcarpetas
     * ({@code uploads/320/<uuid>.webp}) para no tocar nunca el archivo
     * original que referencia la base de datos y el ledger.
     */
    static final int[] ANCHOS_VARIANTE = {128, 320, 480, 640, 960, 1280};

    private static final String PREFIJO_UPLOADS = "/uploads/";
    private static final Pattern NOMBRE_UUID_WEBP = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.webp");

    private final Path directorioRaiz;

    public LocalStorageService(@Value("${storage.local.dir:uploads}") String directorio) {
        this.directorioRaiz = Paths.get(directorio).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        if (archivo.getSize() > MAX_BYTES_PUBLICO) {
            throw new FormatoArchivoInvalidoException(
                "La imagen no puede superar los 5 MB.");
        }

        try {
            Files.createDirectories(directorioRaiz);

            try (ImageInputStream entrada = ImageIO.createImageInputStream(archivo.getInputStream())) {
                if (entrada == null) {
                    throw new FormatoArchivoInvalidoException();
                }

                Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
                if (!lectores.hasNext()) {
                    throw new FormatoArchivoInvalidoException();
                }

                ImageReader lector = lectores.next();
                try {
                    String formato = lector.getFormatName().toLowerCase(Locale.ROOT);
                    if (!formato.equals("jpg") && !formato.equals("jpeg")
                            && !formato.equals("png") && !formato.equals("webp")) {
                        throw new FormatoArchivoInvalidoException();
                    }

                    lector.setInput(entrada);
                    BufferedImage imagen;
                    try {
                        imagen = lector.read(0);
                    } catch (IOException e) {
                        throw new FormatoArchivoInvalidoException();
                    }
                    if (imagen == null) {
                        throw new FormatoArchivoInvalidoException();
                    }
                    imagen = limitarDimensiones(imagen);
                    String nombreArchivo = UUID.randomUUID() + ".webp";
                    Path destino = directorioRaiz.resolve(nombreArchivo);
                    if (!ImageIO.write(imagen, "webp", destino.toFile())) {
                        throw new IOException("No hay un codificador WebP disponible");
                    }
                    generarVariantes(imagen, nombreArchivo);

                    return "/uploads/" + nombreArchivo;
                } finally {
                    lector.dispose();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el archivo", e);
        }
    }

    BufferedImage limitarDimensiones(BufferedImage imagen) {
        int ancho = imagen.getWidth();
        int alto = imagen.getHeight();
        int ladoMayor = Math.max(ancho, alto);
        if (ladoMayor <= MAX_LADO) {
            return imagen;
        }

        double escala = (double) MAX_LADO / ladoMayor;
        int nuevoAncho = Math.max(1, (int) Math.round(ancho * escala));
        return redimensionar(imagen, nuevoAncho);
    }

    private BufferedImage redimensionar(BufferedImage imagen, int ancho) {
        int alto = Math.max(1, (int) Math.round(
            imagen.getHeight() * (double) ancho / imagen.getWidth()));
        BufferedImage reducida = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D grafico = reducida.createGraphics();
        grafico.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        grafico.drawImage(imagen.getScaledInstance(ancho, alto, Image.SCALE_AREA_AVERAGING), 0, 0, null);
        grafico.dispose();
        return reducida;
    }

    static boolean esAnchoVariante(int ancho) {
        for (int valor : ANCHOS_VARIANTE) {
            if (valor == ancho) {
                return true;
            }
        }
        return false;
    }

    private Path rutaVariante(int ancho, String nombre) {
        return directorioRaiz.resolve(Integer.toString(ancho)).resolve(nombre);
    }

    private void generarVariantes(BufferedImage imagen, String nombre) {
        int anchoOriginal = imagen.getWidth();
        for (int ancho : ANCHOS_VARIANTE) {
            if (ancho >= anchoOriginal) {
                continue;
            }
            Path destino = rutaVariante(ancho, nombre);
            if (Files.isRegularFile(destino)) {
                continue;
            }
            try {
                escribirWebp(redimensionar(imagen, ancho), destino);
            } catch (IOException e) {
                // Se puede regenerar on-demand al pedirla; no se interrumpe la subida.
                log.warn("No se pudo generar la variante {} de {}: {}",
                    ancho, nombre, e.getMessage());
            }
        }
    }

    private void escribirWebp(BufferedImage imagen, Path destino) throws IOException {
        Path carpeta = destino.getParent();
        Files.createDirectories(carpeta);
        Path temporal = Files.createTempFile(carpeta, "tmp-", ".webp");
        try {
            if (!ImageIO.write(imagen, "webp", temporal.toFile())) {
                throw new IOException("No hay un codificador WebP disponible");
            }
            try {
                Files.move(temporal, destino,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    /**
     * Devuelve la copia de visualización del ancho pedido, generándola a partir
     * del original si todavía no existe. Nunca escribe sobre el original ni lo
     * re-encodea: solo lee sus píxeles. Si el ancho pedido supera al original,
     * devuelve el original para no ampliarlo.
     */
    public Resource abrirVariante(String nombre, int ancho) {
        if (!esAnchoVariante(ancho) || nombre == null
                || !NOMBRE_UUID_WEBP.matcher(nombre).matches()) {
            throw new NoSuchElementException("Imagen no encontrada.");
        }
        Path original = directorioRaiz.resolve(nombre).normalize();
        if (!original.startsWith(directorioRaiz) || !Files.isRegularFile(original)) {
            throw new NoSuchElementException("Imagen no encontrada.");
        }
        Path variante = rutaVariante(ancho, nombre);
        if (Files.isRegularFile(variante)) {
            return new FileSystemResource(variante);
        }

        BufferedImage imagen;
        try (var entrada = Files.newInputStream(original)) {
            imagen = ImageIO.read(entrada);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer la imagen", e);
        }
        if (imagen == null) {
            throw new NoSuchElementException("Imagen no encontrada.");
        }
        if (ancho >= imagen.getWidth()) {
            return new FileSystemResource(original);
        }

        BufferedImage reducida = redimensionar(imagen, ancho);
        try {
            escribirWebp(reducida, variante);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo generar la variante", e);
        }
        return new FileSystemResource(variante);
    }

    @Override
    public Resource abrir(String url) {
        String nombre = nombreValido(url);
        if (nombre == null) {
            throw new NoSuchElementException("Imagen no encontrada.");
        }
        Path archivo = directorioRaiz.resolve(nombre).normalize();
        if (!archivo.startsWith(directorioRaiz) || !Files.isRegularFile(archivo)) {
            throw new NoSuchElementException("Imagen no encontrada.");
        }
        return new FileSystemResource(archivo);
    }

    @Override
    public boolean eliminar(String url) {
        String nombre = nombreValido(url);
        if (nombre == null) {
            return false;
        }
        try {
            boolean borrado = Files.deleteIfExists(directorioRaiz.resolve(nombre));
            for (int ancho : ANCHOS_VARIANTE) {
                Files.deleteIfExists(rutaVariante(ancho, nombre));
            }
            return borrado;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public int eliminarHuerfanos(Predicate<String> enUso, Duration antiguedadMinima) {
        int borrados = 0;
        long corte = System.currentTimeMillis() - antiguedadMinima.toMillis();
        if (!Files.isDirectory(directorioRaiz)) {
            return 0;
        }
        try (Stream<Path> archivos = Files.list(directorioRaiz)) {
            for (Path archivo : (Iterable<Path>) archivos::iterator) {
                if (!Files.isRegularFile(archivo)) {
                    continue;
                }
                String nombre = archivo.getFileName().toString();
                if (!NOMBRE_UUID_WEBP.matcher(nombre).matches()) {
                    continue;
                }
                if (Files.getLastModifiedTime(archivo).toMillis() > corte) {
                    continue;
                }
                if (enUso.test(PREFIJO_UPLOADS + nombre)) {
                    continue;
                }
                try {
                    if (Files.deleteIfExists(archivo)) {
                        borrados++;
                    }
                } catch (IOException e) {
                    // Se omite este archivo y se volverá a intentar en la próxima ejecución.
                }
            }
        } catch (IOException e) {
            // No se pudo listar el directorio; se reintenta en la próxima ejecución.
        }

        borrados += eliminarVariantesHuerfanas();
        return borrados;
    }

    /**
     * Borra las copias de visualización cuyo original ya no existe. Se ejecuta
     * después de la limpieza del directorio raíz, así una imagen eliminada se
     * lleva también sus variantes.
     */
    private int eliminarVariantesHuerfanas() {
        int borrados = 0;
        for (int ancho : ANCHOS_VARIANTE) {
            Path carpeta = directorioRaiz.resolve(Integer.toString(ancho));
            if (!Files.isDirectory(carpeta)) {
                continue;
            }
            try (Stream<Path> archivos = Files.list(carpeta)) {
                for (Path archivo : (Iterable<Path>) archivos::iterator) {
                    if (!Files.isRegularFile(archivo)) {
                        continue;
                    }
                    String nombre = archivo.getFileName().toString();
                    if (!NOMBRE_UUID_WEBP.matcher(nombre).matches()) {
                        continue;
                    }
                    if (Files.isRegularFile(directorioRaiz.resolve(nombre))) {
                        continue;
                    }
                    try {
                        if (Files.deleteIfExists(archivo)) {
                            borrados++;
                        }
                    } catch (IOException e) {
                        // Se reintenta en la próxima ejecución.
                    }
                }
            } catch (IOException e) {
                // Se reintenta en la próxima ejecución.
            }
        }
        return borrados;
    }

    private String nombreValido(String url) {
        if (url == null || !url.startsWith(PREFIJO_UPLOADS)) {
            return null;
        }
        String nombre = url.substring(PREFIJO_UPLOADS.length());
        return NOMBRE_UUID_WEBP.matcher(nombre).matches() ? nombre : null;
    }
}