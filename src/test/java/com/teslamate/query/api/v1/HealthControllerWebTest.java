package com.teslamate.query.api.v1;

import com.teslamate.query.dao.HealthDao;
import com.teslamate.query.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc (no full Boot context) — avoids Boot 4 slice + @EnableCaching coupling.
 */
@ExtendWith(MockitoExtension.class)
class HealthControllerWebTest {

    @Mock
    private HealthDao healthDao;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HealthController(healthDao))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void healthIsPublicAndUp() throws Exception {
        when(healthDao.ping()).thenReturn(1);
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"));
    }
}
