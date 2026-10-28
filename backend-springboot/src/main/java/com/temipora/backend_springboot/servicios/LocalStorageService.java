package com.temipora.backend_springboot.servicios;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalStorageService implements StorageService {

    static final int MAX_LADO = 2048;
    static final long MAX_BYTES_PUBLICO = 5L * 1024 * 1024;

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
        int nuevoAlto = Math.max(1, (int) Math.round(alto * escala));

        BufferedImage reducida = new BufferedImage(nuevoAncho, nuevoAlto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D grafico = reducida.createGraphics();
        grafico.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        grafico.drawImage(imagen.getScaledInstance(nuevoAncho, nuevoAlto, Image.SCALE_AREA_AVERAGING), 0, 0, null);
        grafico.dispose();
        return reducida;
    }

    @Override
    public boolean eliminar(String url) {
        String nombre = nombreValido(url);
        if (nombre == null) {
            return false;
        }
        try {
            return Files.deleteIfExists(directorioRaiz.resolve(nombre));
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