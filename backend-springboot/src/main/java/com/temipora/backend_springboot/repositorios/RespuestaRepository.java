package com.temipora.backend_springboot.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.Respuesta;

public interface RespuestaRepository extends JpaRepository<Respuesta, Long> {

    @Query("""
        SELECT r.respuesta
        FROM Respuesta r
        WHERE r.tipoCampo.codigo = :codigo
        """)
    List<String> findRespuestasByTipoCampoCodigo(@Param("codigo") String codigo);

    boolean existsByRespuestaAndTipoCampoCodigo(String respuesta, String codigo);
}
