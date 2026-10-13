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
import pl.matkmiec.backend.moon.dto.*;
import pl.matkmiec.backend.moon.service.MoonService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MoonPublicControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MoonService moonService;

    @InjectMocks
    private MoonPublicController controller;

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
    @DisplayName("GET /v1/moon/daily should return daily moon data")
    void shouldReturnDailyMoonData() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 13);
        LocationInfoDto location = new LocationInfoDto(52.23, 21.01, "352200375", "WARSZAWA", 0.0, "Europe/Warsaw");
        MoonPhaseDto phase = MoonPhaseDto.of(MoonPhase.FULL_MOON, 0.5);
        GeneralMoonDataDto general = new GeneralMoonDataDto(phase, 99.5, 14.8, 368000.0, false, false, "Normalna", date, date, date, date);
        SunTimesDto sunTimes = new SunTimesDto(LocalTime.of(6, 45), LocalTime.of(17, 30), LocalTime.of(12, 10), "10:45:00", 38700, 120L, null, null, null, null, null, null, null);
        MoonTimesDto moonTimes = new MoonTimesDto(LocalTime.of(18, 0), LocalTime.of(6, 30), LocalTime.of(0, 15), LocalTime.of(12, 15), 45.0, 180.0);
        SolunarDayDto solunar = new SolunarDayDto(SolunarActivityLevel.EXCELLENT, 95, 5, "Doskonałe warunki", List.of(), List.of(), List.of(), List.of("17:30 - 18:30"), "Pełnia", "Wypłyń na łowisko");

        MoonDailyDto response = new MoonDailyDto(date, location, general, sunTimes, moonTimes, solunar);

        when(moonService.getDailyData(any(), any(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/v1/moon/daily")
                        .param("lat", "52.23")
                        .param("lon", "21.01")
                        .param("date", "2026-10-13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-13"))
                .andExpect(jsonPath("$.location.latitude").value(52.23))
                .andExpect(jsonPath("$.generalMoon.phase.polishName").value("Pełnia"))
                .andExpect(jsonPath("$.solunar.overallScore").value(95))
                .andExpect(jsonPath("$.solunar.stars").value(5));
    }

    @Test
    @DisplayName("GET /v1/moon/monthly should return monthly moon calendar")
    void shouldReturnMonthlyMoonData() throws Exception {
        LocationInfoDto location = new LocationInfoDto(52.23, 21.01, "352200375", "WARSZAWA", 0.0, "Europe/Warsaw");
        MoonPhaseDto phase = MoonPhaseDto.of(MoonPhase.FULL_MOON, 0.5);
        MoonCalendarDayDto day = new MoonCalendarDayDto(LocalDate.of(2026, 10, 1), phase, 99.0, 14.0, LocalTime.of(6, 30), LocalTime.of(18, 0), LocalTime.of(18, 30), LocalTime.of(6, 0), SolunarActivityLevel.EXCELLENT, 5, 95, false, false);
        MoonMonthlyDto monthlyDto = new MoonMonthlyDto(2026, 10, location, List.of(day), LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 24), 8);

        when(moonService.getMonthlyData(any(), any(), any(), any(), any())).thenReturn(monthlyDto);

        mockMvc.perform(get("/v1/moon/monthly")
                        .param("year", "2026")
                        .param("month", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(10))
                .andExpect(jsonPath("$.days[0].solunarStars").value(5))
                .andExpect(jsonPath("$.bestFishingDaysCount").value(8));
    }

    @Test
    @DisplayName("GET /v1/moon/phases should return list of moon phases")
    void shouldReturnMoonPhases() throws Exception {
        List<MoonPhaseDto> phases = List.of(MoonPhaseDto.of(MoonPhase.NEW_MOON, 0.0), MoonPhaseDto.of(MoonPhase.FULL_MOON, 0.5));
        when(moonService.getAllMoonPhases()).thenReturn(phases);

        mockMvc.perform(get("/v1/moon/phases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].polishName").value("Nów"))
                .andExpect(jsonPath("$[1].polishName").value("Pełnia"));
    }
}
