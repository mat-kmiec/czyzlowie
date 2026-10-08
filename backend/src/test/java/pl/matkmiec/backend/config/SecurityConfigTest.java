package pl.matkmiec.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import pl.matkmiec.backend.weather.service.WeatherSyncService;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private WeatherSyncService weatherSyncService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void shouldPermitUnauthenticatedGetRequest() throws Exception {
        mockMvc.perform(get("/public/weather/stations/synop"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldPermitUnauthenticatedPostRequestWithoutCsrf() throws Exception {
        mockMvc.perform(post("/admin/weather/sync/synop"))
                .andExpect(status().isOk());
    }
}
