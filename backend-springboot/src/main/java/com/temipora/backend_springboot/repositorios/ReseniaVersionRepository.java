package com.temipora.backend_springboot.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.ReseniaVersion;

public interface ReseniaVersionRepository
        extends JpaRepository<ReseniaVersion, Long> {

    List<ReseniaVersion> findByReseniaIdReseniaOrderByNumeroAsc(Long idResenia);

    long countByReseniaIdResenia(Long idResenia);
}
