package com.temipora.backend_springboot.servicios;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.temipora.backend_springboot.modelo.DescargaArchivo;
import com.temipora.backend_springboot.modelo.TipoDescarga;
import com.temipora.backend_springboot.repositorios.DescargaArchivoRepository;
import com.temipora.backend_springboot.repositorios.EntregaArchivoRepository;
import com.temipora.backend_springboot.repositorios.PedidoRepository;
import com.temipora.backend_springboot.repositorios.UsuarioRepository;

@Service
public class DescargaService {

    private static final int MAX_IP = 45;

    private final DescargaArchivoRepository descargaRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntregaArchivoRepository archivoRepository;

    public DescargaService(
            DescargaArchivoRepository descargaRepository,
            PedidoRepository pedidoRepository,
            UsuarioRepository usuarioRepository,
            EntregaArchivoRepository archivoRepository) {
        this.descargaRepository = descargaRepository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.archivoRepository = archivoRepository;
    }

    @Transactional
    public void registrar(
            Long idPedido,
            Long idUsuario,
            Long idArchivo,
            TipoDescarga tipo,
            String ip) {
        DescargaArchivo descarga = new DescargaArchivo();
        descarga.setPedido(pedidoRepository.getReferenceById(idPedido));
        descarga.setUsuario(usuarioRepository.getReferenceById(idUsuario));
        if (idArchivo != null) {
            descarga.setArchivo(
                archivoRepository.getReferenceById(idArchivo));
        }
        descarga.setTipo(tipo);
        descarga.setIp(recortarIp(ip));
        descargaRepository.save(descarga);
    }

    private String recortarIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        return ip.length() > MAX_IP ? ip.substring(0, MAX_IP) : ip;
    }
}
