package com.temipora.backend_springboot.repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.TokenAcceso;

import jakarta.persistence.LockModeType;

public interface TokenAccesoRepository extends JpaRepository<TokenAcceso, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TokenAcceso t WHERE t.tokenHash = :hash")
    Optional<TokenAcceso> findByTokenHashForUpdate(@Param("hash") String hash);
}
