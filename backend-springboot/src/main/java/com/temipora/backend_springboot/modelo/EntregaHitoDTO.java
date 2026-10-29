package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntregaHitoDTO {

    private Long idEntrega;
    private Integer numero;
    private LocalDateTime fechaEntrega;
    private UsuarioResumenDTO autor;
    private String descripcion;
    private boolean vigente;
    private boolean esFinal;
    private boolean aprobada;
    private String revisionMotivo;
    private LocalDateTime revisionFecha;
    private UsuarioResumenDTO revisionPor;
    private List<EntregaArchivoDTO> archivos;

    public static EntregaHitoDTO desdeEntrega(
            EntregaHito entrega,
            List<EntregaArchivo> archivosEntrega,
            Long idPedido,
            boolean incluirOriginal,
            boolean aprobada) {
        List<EntregaArchivoDTO> archivos = archivosEntrega.stream()
            .filter(archivo -> !archivo.isRetirado())
            .map(archivo -> EntregaArchivoDTO.desdeArchivo(
                archivo, idPedido, entrega.getIdEntrega(), incluirOriginal))
            .toList();

        return EntregaHitoDTO.builder()
            .idEntrega(entrega.getIdEntrega())
            .numero(entrega.getNumero())
            .fechaEntrega(entrega.getFechaEntrega())
            .autor(UsuarioResumenDTO.desdeUsuario(entrega.getAutor()))
            .descripcion(entrega.getDescripcion())
            .vigente(entrega.isVigente())
            .esFinal(entrega.isEsFinal())
            .aprobada(aprobada)
            .revisionMotivo(entrega.getRevisionMotivo())
            .revisionFecha(entrega.getRevisionFecha())
            .revisionPor(UsuarioResumenDTO.desdeUsuario(entrega.getRevisionPor()))
            .archivos(archivos)
            .build();
    }
}
