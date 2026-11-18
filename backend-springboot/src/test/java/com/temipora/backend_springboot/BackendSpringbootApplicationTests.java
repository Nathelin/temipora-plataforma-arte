package com.temipora.backend_springboot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.temipora.backend_springboot.config.filters.TerminosAceptadosFilter;

@SpringBootTest
class BackendSpringbootApplicationTests {

	@Autowired
	private TerminosAceptadosFilter terminosAceptadosFilter;

	@Test
	void contextLoads() {
		assertThat(terminosAceptadosFilter).isNotNull();
	}

}
