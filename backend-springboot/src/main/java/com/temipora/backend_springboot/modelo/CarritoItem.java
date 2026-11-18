package com.temipora.backend_springboot.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "carrito_item",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_carrito_item_variante",
        columnNames = { "id_carrito", "id_variante" }))
@Getter
@Setter
@NoArgsConstructor
public class CarritoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrito_item")
    private Long idCarritoItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_carrito", nullable = false)
    private Carrito carrito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_variante", nullable = false)
    private ProductoVariante variante;

    @Column(name = "id_publicacion_reventa")
    private Long idPublicacionReventa;

    @Column(nullable = false)
    private int cantidad = 1;

    @Column(name = "fecha_agregado", nullable = false, updatable = false)
    private LocalDateTime fechaAgregado = LocalDateTime.now();
}
