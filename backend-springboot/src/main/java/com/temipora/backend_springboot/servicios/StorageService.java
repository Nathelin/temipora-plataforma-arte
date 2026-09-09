package com.temipora.backend_springboot.servicios;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String guardar(MultipartFile archivo);
}