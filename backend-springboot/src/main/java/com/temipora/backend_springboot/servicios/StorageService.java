package com.temipora.backend_springboot.servicios;

import java.time.Duration;
import java.util.function.Predicate;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String guardar(MultipartFile archivo);

    Resource abrir(String url);

    boolean eliminar(String url);

    int eliminarHuerfanos(Predicate<String> enUso, Duration antiguedadMinima);
}
