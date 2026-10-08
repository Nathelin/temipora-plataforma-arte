package com.temipora.backend_springboot.servicios;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.temipora.backend_springboot.repositorios.ImagenRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class LimpiezaArchivosService {

    static final Duration ANTIGUEDAD_HUERFANOS = Duration.ofHours(24);

    private final StorageService storageService;
    private final ImagenRepository imagenRepository;
    private final UsuarioRepository usuarioRepository;

    public LimpiezaArchivosService(StorageService storageService,
            ImagenRepository imagenRepository,
            UsuarioRepository usuarioRepository) {
        this.storageService = storageService;
        this.imagenRepository = imagenRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Elimina del disco los archivos indicados que no estén referenciados
     * en la base de datos (portfolio, fotos de perfil y banners).
     * Devuelve la cantidad de archivos realmente eliminados.
     */
    public int eliminar(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return 0;
        }
        int borrados = 0;
        for (String url : urls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            if (enUso(url)) {
                continue;
            }
            if (storageService.eliminar(url)) {
                borrados++;
            }
        }
        return borrados;
    }

    /**
     * Elimina los archivos con más de 24 horas que ninguna fila de la base
     * de datos referencia. Se ejecuta periódicamente como red de seguridad
     * para las pestañas cerradas sin llegar a limpiar.
     */
    public int limpiarHuerfanos() {
        Set<String> referenciadas = new HashSet<>(imagenRepository.findUrls());
        usuarioRepository.findUrlsFotoPerfil().forEach(referenciadas::add);
        usuarioRepository.findUrlsFotoPerfilMiniatura().forEach(referenciadas::add);
        usuarioRepository.findUrlsBanner().forEach(referenciadas::add);
        return storageService.eliminarHuerfanos(referenciadas::contains, ANTIGUEDAD_HUERFANOS);
    }

    public boolean enUso(String url) {
        return imagenRepository.existsByUrl(url) || usuarioRepository.existeUrlEnPerfil(url);
    }
}
