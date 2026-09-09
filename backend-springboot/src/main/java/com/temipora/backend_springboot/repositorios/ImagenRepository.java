package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.Imagen;
import com.temipora.backend_springboot.modelo.MedioPortfolio;
import com.temipora.backend_springboot.modelo.Portfolio;

import java.util.List;

public interface ImagenRepository extends JpaRepository<Imagen,Long>{

	List<Imagen> findByPortfolioAndEstadoTrueOrderByFechaSubidaDesc(Portfolio portfolio);

	boolean existsByPortfolioAndTituloIgnoreCaseAndMedioAndEstadoTrue(
			Portfolio portfolio, String titulo, MedioPortfolio medio);
}