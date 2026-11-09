package com.temipora.backend_springboot.modelo;

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
public class ProductoResumenEstadoDTO {

    private Long idProducto;
    private int totalVariantes;
    private long variantesDisponibles;
    private long variantesAgotadas;
    private long variantesArchivadas;
    private long variantesConLedger;
}
