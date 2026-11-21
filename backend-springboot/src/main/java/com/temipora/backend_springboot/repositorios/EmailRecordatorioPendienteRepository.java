package com.temipora.backend_springboot.repositorios;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.EmailRecordatorioPendiente;
import com.temipora.backend_springboot.modelo.EstadoEmailRecordatorio;

public interface EmailRecordatorioPendienteRepository
        extends JpaRepository<EmailRecordatorioPendiente, Long> {

    boolean existsByClave(String clave);

    List<EmailRecordatorioPendiente>
        findByUsuarioIdUsuarioAndEstadoOrderByCreadoEnAsc(
            Long idUsuario, EstadoEmailRecordatorio estado, Pageable pageable);

    @Query("""
        select distinct r.usuario.idUsuario
        from EmailRecordatorioPendiente r
        where r.estado = :estado
        """)
    List<Long> findUsuariosConPendientes(
        @Param("estado") EstadoEmailRecordatorio estado);
}
