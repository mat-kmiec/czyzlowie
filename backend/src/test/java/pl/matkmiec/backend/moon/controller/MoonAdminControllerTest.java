package pl.matkmiec.backend.moon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.matkmiec.backend.moon.dto.LocationInfoDto;
import pl.matkmiec.backend.moon.dto.MoonGenerationResultDto;
import pl.matkmiec.backend.moon.dto.MoonYearlyAdminDto;
import pl.matkmiec.backend.moon.service.MoonService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MoonAdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MoonService moonService;

    @InjectMocks
    private MoonAdminController controller;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    @DisplayName("POST /v1/admin/moon/generate-year should trigger database generation")
    void shouldTriggerYearlyGeneration() throws Exception {
        MoonGenerationResultDto resultDto = new MoonGenerationResultDto(
                2027, 365, 100, 36500L, 1200L, "SUCCESS", "Pomyślnie wygenerowano dane");

        when(moonService.generateYearlyData(2027)).thenReturn(resultDto);

        mockMvc.perform(post("/v1/admin/moon/generate-year")
                        .param("year", "2027"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2027))
                .andExpect(jsonPath("$.generalDaysProcessed").value(365))
                .andExpect(jsonPath("$.stationsProcessed").value(100))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /v1/admin/moon/year should return yearly calculations")
    void shouldReturnYearlyCalculations() throws Exception {
        LocationInfoDto location = new LocationInfoDto(52.23, 21.01, "352200375", "WARSZAWA", 0.0, "Europe/Warsaw");
        MoonYearlyAdminDto adminDto = new MoonYearlyAdminDto(2027, location, 365, List.of(), Map.of("totalDays", 365, "excellentFishingDays", 45));

        when(moonService.getYearlyAdminData(eq(2027), any(), any(), any())).thenReturn(adminDto);

        mockMvc.perform(get("/v1/admin/moon/year")
                        .param("year", "2027"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2027))
                .andExpect(jsonPath("$.totalDays").value(365))
                .andExpect(jsonPath("$.summaryStats.excellentFishingDays").value(45));
    }
}
