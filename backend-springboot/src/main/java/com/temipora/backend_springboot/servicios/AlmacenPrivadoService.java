package com.temipora.backend_springboot.servicios;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Guarda los archivos de las entregas en un directorio privado, sin
 * re-encodear ni redimensionar: el original se conserva byte a byte.
 * La raíz privada no se expone como recurso estático.
 */
@Service
public class AlmacenPrivadoService {

    static final long MAX_BYTES = 25L * 1024 * 1024;
    static final int MAX_LADO = 12_000;
    static final long MAX_PIXELES = 80_000_000L;

    private final Path directorioRaiz;

    public AlmacenPrivadoService(
            @Value("${storage.private.dir:storage-privado}") String directorio) {
        this.directorioRaiz = Paths.get(directorio).toAbsolutePath().normalize();
    }

    public ArchivoGuardado guardarOriginal(MultipartFile archivo, String subcarpeta) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Tenés que subir al menos un archivo.");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException(
                "Cada archivo puede pesar como máximo 25 MB.");
        }

        byte[] bytes;
        try {
            bytes = archivo.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo.", e);
        }

        Formato formato = detectarFormato(bytes);
        if (formato.ancho() > MAX_LADO || formato.alto() > MAX_LADO
                || (long) formato.ancho() * formato.alto() > MAX_PIXELES) {
            throw new IllegalArgumentException(
                "La imagen es demasiado grande. Máximo 12.000 px por lado y 80 MP.");
        }

        String clave = construirClave(subcarpeta, formato.extension());
        Path destino = resolverParaEscribir(clave);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo.", e);
        }

        return new ArchivoGuardado(
            clave,
            nombreVisible(archivo.getOriginalFilename(), formato.extension()),
            formato.mime(),
            formato.ancho(),
            formato.alto(),
            bytes.length,
            sha256(bytes));
    }

    public String guardarPreview(byte[] bytes, String subcarpeta) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("La preview está vacía.");
        }
        String clave = construirClave(subcarpeta, "webp");
        Path destino = resolverParaEscribir(clave);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la preview.", e);
        }
        return clave;
    }

    public byte[] leer(String clave) {
        try {
            return Files.readAllBytes(resolverParaLeer(clave));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo.", e);
        }
    }

    public Resource abrir(String clave) {
        return new FileSystemResource(resolverParaLeer(clave));
    }

    public boolean eliminar(String clave) {
        try {
            return Files.deleteIfExists(resolverParaLeer(clave));
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }

    Path resolverParaEscribir(String clave) {
        return resolver(clave);
    }

    private Path resolverParaLeer(String clave) {
        Path path = resolver(clave);
        if (!Files.isRegularFile(path)) {
            throw new NoSuchElementException("Archivo no encontrado.");
        }
        return path;
    }

    private Path resolver(String clave) {
        if (clave == null || clave.isBlank() || clave.contains("..")) {
            throw new IllegalArgumentException("Clave de archivo inválida.");
        }
        Path path = directorioRaiz.resolve(clave).normalize();
        if (!path.startsWith(directorioRaiz)) {
            throw new IllegalArgumentException("Clave de archivo inválida.");
        }
        return path;
    }

    private String construirClave(String subcarpeta, String extension) {
        if (subcarpeta == null || !subcarpeta.matches("[a-zA-Z0-9/_-]+")) {
            throw new IllegalArgumentException("Carpeta de archivo inválida.");
        }
        return subcarpeta + "/" + UUID.randomUUID() + "." + extension;
    }

    private Formato detectarFormato(byte[] bytes) {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(
                new ByteArrayInputStream(bytes))) {
            if (entrada == null) {
                throw new IllegalArgumentException("El archivo no es una imagen válida.");
            }
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) {
                throw new IllegalArgumentException("El archivo no es una imagen válida.");
            }
            ImageReader lector = lectores.next();
            try {
                String nombre = lector.getFormatName().toLowerCase(Locale.ROOT);
                Formato formato = switch (nombre) {
                    case "jpeg", "jpg" -> new Formato("jpg", "image/jpeg");
                    case "png" -> new Formato("png", "image/png");
                    case "webp" -> new Formato("webp", "image/webp");
                    default -> throw new IllegalArgumentException(
                        "Formato no permitido. Usá JPG, PNG o WebP.");
                };
                lector.setInput(entrada, true, true);
                return formato.conDimensiones(
                    lector.getWidth(0),
                    lector.getHeight(0));
            } finally {
                lector.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(
                "No se pudo leer la imagen. Verificá que el archivo no esté dañado.", e);
        }
    }

    private String nombreVisible(String original, String extension) {
        String limpio = original == null ? "" : original.replace('\\', '/');
        int separador = limpio.lastIndexOf('/');
        if (separador >= 0) {
            limpio = limpio.substring(separador + 1);
        }
        limpio = limpio.replaceAll("[\\p{Cntrl}]", " ").trim();
        if (limpio.isEmpty()) {
            limpio = "archivo." + extension;
        }
        return limpio.length() > 255 ? limpio.substring(0, 255) : limpio;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible.", e);
        }
    }

    public record ArchivoGuardado(
            String clave,
            String nombreOriginal,
            String mime,
            int ancho,
            int alto,
            long tamanoBytes,
            String sha256) {
    }

    private record Formato(String extension, String mime, int ancho, int alto) {

        Formato(String extension, String mime) {
            this(extension, mime, 0, 0);
        }

        Formato conDimensiones(int ancho, int alto) {
            return new Formato(extension, mime, ancho, alto);
        }
    }
}
