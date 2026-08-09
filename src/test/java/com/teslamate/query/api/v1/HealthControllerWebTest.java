package com.teslamate.query.api.v1;

import com.teslamate.query.config.QueryProperties;
import com.teslamate.query.dao.HealthDao;
import com.teslamate.query.security.ApiKeyAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthController.class)
@Import({ApiKeyAuthFilter.class, QueryProperties.class})
class HealthControllerWebTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    HealthDao healthDao;

    @Test
    void healthIsPublicAndUp() throws Exception {
        when(healthDao.ping()).thenReturn(1);
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"));
    }
}
