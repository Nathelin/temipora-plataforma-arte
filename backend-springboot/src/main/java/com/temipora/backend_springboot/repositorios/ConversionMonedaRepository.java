package com.temipora.backend_springboot.repositorios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.ConversionMoneda;
import com.temipora.backend_springboot.modelo.Moneda;

public interface ConversionMonedaRepository extends JpaRepository<ConversionMoneda, Long> {

    @Query("""
        select c from ConversionMoneda c
        where c.monedaEntrada = :entrada and c.monedaSalida = :salida
          and c.inicioValidez <= :fecha
          and (c.finValidez is null or c.finValidez > :fecha)
        order by c.inicioValidez desc
        """)
    List<ConversionMoneda> vigente(
            @Param("entrada") Moneda entrada,
            @Param("salida") Moneda salida,
            @Param("fecha") LocalDateTime fecha);

    @Query("""
        select c from ConversionMoneda c
        where c.monedaEntrada = :entrada and c.monedaSalida = :salida
          and c.finValidez is null
        order by c.inicioValidez desc
        """)
    List<ConversionMoneda> abierta(
            @Param("entrada") Moneda entrada,
            @Param("salida") Moneda salida);
}
