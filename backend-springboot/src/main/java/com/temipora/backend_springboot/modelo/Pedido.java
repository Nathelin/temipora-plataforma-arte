package com.temipora.backend_springboot.modelo;


import java.math.BigInteger;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="Pedido")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPedido;

    @NotBlank
    private LocalDateTime fechaEmision;

    @NotNull
    private BigInteger precioBase;

    @NotNull
    private BigInteger precioFinal;

    @NotNull
    private LocalDateTime limiteEntrega;

    private String mensaje;
    
    @NotNull
    private String estado;







    
}
