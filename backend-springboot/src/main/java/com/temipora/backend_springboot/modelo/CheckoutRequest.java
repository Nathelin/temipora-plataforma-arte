package com.temipora.backend_springboot.modelo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CheckoutRequest {

    private String claveIdempotencia;

    /**
     * Si se informa, solo se compra el grupo de ese artista y el resto queda
     * en el carrito. Sin valor, se compra todo el carrito.
     */
    private Long idArtista;
}
