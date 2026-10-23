package com.temipora.backend_springboot.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import com.temipora.backend_springboot.modelo.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
}
