package com.temipora.backend_springboot.modelo;

/**
 * Alcance de una sanción.
 *
 * <ul>
 *   <li>{@code ACCESO}: bloqueo total de la cuenta (v1).</li>
 *   <li>{@code VENTAS}: no puede vender ni recibir encargos nuevos, pero
 *       conserva el acceso al resto de la plataforma.</li>
 *   <li>{@code ENTREGAS}: acceso restringido a subir entregables de los
 *       pedidos que tenía abiertos.</li>
 * </ul>
 */
public enum AlcanceSancion {
    ACCESO,
    VENTAS,
    ENTREGAS
}
