package com.temipora.backend_springboot.modelo;

import com.fasterxml.jackson.annotation.JsonInclude;

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
public class EntregaArchivoDTO {

    private Long idArchivo;
    private String nombre;
    private String mime;
    private Integer ancho;
    private Integer alto;
    private String sha256;
    private Integer orden;
    private DibujoResumenDTO dibujo;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String urlOriginal;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String urlPreview;

    public static EntregaArchivoDTO desdeArchivo(
            EntregaArchivo archivo,
            Long idPedido,
            Long idEntrega,
            boolean incluirOriginal) {
        String base = "/pedidos/" + idPedido
            + "/entregas/" + idEntrega
            + "/archivos/" + archivo.getIdArchivo();

        return EntregaArchivoDTO.builder()
            .idArchivo(archivo.getIdArchivo())
            .nombre(archivo.getNombreOriginal())
            .mime(archivo.getMime())
            .ancho(archivo.getAncho())
            .alto(archivo.getAlto())
            .sha256(archivo.getSha256())
            .orden(archivo.getOrden())
            .dibujo(DibujoResumenDTO.desdeItem(archivo.getItemSolicitud()))
            .urlOriginal(incluirOriginal ? base + "/original" : null)
            .urlPreview(archivo.getClavePreview() != null
                ? base + "/preview" : null)
            .build();
    }
}
