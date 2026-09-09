package com.temipora.backend_springboot.servicios;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

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

    private final Path directorioRaiz;

    public LocalStorageService(@Value("${storage.local.dir:uploads}") String directorio) {
        this.directorioRaiz = Paths.get(directorio).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
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
}