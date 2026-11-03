package com.temipora.backend_springboot.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuditoriaRequestFilterTest {

    @Test
    void asignaUnIdentificadorPorRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AuditoriaRequestFilter().doFilter(request, response, new MockFilterChain());

        assertThat(request.getAttribute(AuditoriaRequestFilter.REQUEST_ID_ATTR)).isNotNull();
    }
}
