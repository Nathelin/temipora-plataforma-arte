package com.temipora.backend_springboot.servicios;

import java.util.NoSuchElementException;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.EntregaArchivo;
import com.temipora.backend_springboot.repositorios.EntregaArchivoRepository;

@Service
public class ImagenGaleriaService {

    private final AlmacenPrivadoService almacen;
    private final PreviewService previewService;
    private final EntregaArchivoRepository archivoRepository;

    public ImagenGaleriaService(
            AlmacenPrivadoService almacen,
            PreviewService previewService,
            EntregaArchivoRepository archivoRepository) {
        this.almacen = almacen;
        this.previewService = previewService;
        this.archivoRepository = archivoRepository;
    }

    @Transactional
    public String asegurarLimpia(EntregaArchivo archivo) {
        if (archivo.getClaveLimpia() != null) {
            try {
                almacen.abrir(archivo.getClaveLimpia());
                return archivo.getClaveLimpia();
            } catch (NoSuchElementException e) {
                archivo.setClaveLimpia(null);
            }
        }

        byte[] original = almacen.leer(archivo.getClaveOriginal());
        byte[] limpia = previewService.generarLimpia(original);
        String clave = almacen.guardarPreview(limpia, "galeria");
        archivo.setClaveLimpia(clave);
        archivoRepository.save(archivo);
        return clave;
    }

    @Transactional
    public Resource abrirLimpia(EntregaArchivo archivo) {
        return almacen.abrir(asegurarLimpia(archivo));
    }
}
