package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import com.temipora.backend_springboot.modelo.Imagen;
import com.temipora.backend_springboot.modelo.Portfolio;
import com.temipora.backend_springboot.modelo.TipoArte;

import java.util.Collection;
import java.util.List;

public interface ImagenRepository extends JpaRepository<Imagen,Long>{

	List<Imagen> findByPortfolioAndEstadoTrueOrderByFechaSubidaDesc(Portfolio portfolio);

	@EntityGraph(attributePaths = { "usuario", "tipoArte", "tipoArte.medio" })
	List<Imagen> findByUsuarioIdUsuarioInAndEstadoTrue(Collection<Long> idsUsuario);

	boolean existsByUrl(String url);

	@Query("SELECT i.url FROM Imagen i WHERE i.url IS NOT NULL")
	List<String> findUrls();

	boolean existsByPortfolioAndTituloIgnoreCaseAndTipoArteAndEstadoTrue(
			Portfolio portfolio, String titulo, TipoArte tipoArte);

    @Query("""
        SELECT i FROM Imagen i
        WHERE i.portfolio.idPortfolio = :portfolioId
          AND i.estado = true
          AND (:titulo IS NULL OR LOWER(i.titulo) LIKE :titulo)
          AND (:idEtiqueta IS NULL OR EXISTS (
                SELECT ie FROM ImagenEtiqueta ie
                WHERE ie.imagen = i AND ie.etiqueta.idEtiqueta = :idEtiqueta))
          AND (:idTipoArte IS NULL OR i.tipoArte.id = :idTipoArte)
          AND (:conSensible IS NULL
               OR (:conSensible = true AND i.contenidoSensible IS NOT NULL)
               OR (:conSensible = false AND i.contenidoSensible IS NULL))
          AND (i.contenidoSensible IS NULL
               OR :ocultarSensibles = false
               OR i.usuario.idUsuario = :viewerId)
        ORDER BY CASE WHEN :ordenAsc = true THEN i.fechaSubida END ASC,
                 CASE WHEN :ordenAsc = false THEN i.fechaSubida END DESC
        """)
    Page<Imagen> findParaVisor(
            @Param("portfolioId") Long portfolioId,
            @Param("viewerId") Long viewerId,
            @Param("titulo") String titulo,
            @Param("idEtiqueta") Long idEtiqueta,
            @Param("idTipoArte") Long idTipoArte,
            @Param("conSensible") Boolean conSensible,
            @Param("ocultarSensibles") boolean ocultarSensibles,
            @Param("ordenAsc") boolean ordenAsc,
            Pageable pageable);
}
