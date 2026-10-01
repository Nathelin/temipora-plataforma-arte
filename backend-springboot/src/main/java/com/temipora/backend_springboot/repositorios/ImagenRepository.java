package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.Imagen;
import com.temipora.backend_springboot.modelo.MedioPortfolio;
import com.temipora.backend_springboot.modelo.Portfolio;

import java.util.List;

public interface ImagenRepository extends JpaRepository<Imagen,Long>{

	List<Imagen> findByPortfolioAndEstadoTrueOrderByFechaSubidaDesc(Portfolio portfolio);

	boolean existsByPortfolioAndTituloIgnoreCaseAndMedioAndEstadoTrue(
			Portfolio portfolio, String titulo, MedioPortfolio medio);

    @Query("""
        SELECT i FROM Imagen i
          WHERE i.portfolio.idPortfolio = :portfolioId
          AND i.estado = true
        ORDER BY i.fechaSubida DESC
        """)
    Page<Imagen> findVisibleForViewer(
            @Param("portfolioId") Long portfolioId,
            Pageable pageable);

    @Query("""
        SELECT i FROM Imagen i
          WHERE i.portfolio.idPortfolio = :portfolioId
          AND i.estado = true
          AND (i.contenidoSensible IS NULL OR i.usuario.idUsuario = :viewerId)
        ORDER BY i.fechaSubida DESC
        """)
    Page<Imagen> findVisibleForViewerOcultandoSensibles(
            @Param("portfolioId") Long portfolioId,
            @Param("viewerId") Long viewerId,
            Pageable pageable);
}