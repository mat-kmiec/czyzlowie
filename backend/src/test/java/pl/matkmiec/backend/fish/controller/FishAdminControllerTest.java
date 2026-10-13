package pl.matkmiec.backend.fish.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.matkmiec.backend.fish.dto.FishSpeciesCreateUpdateDto;
import pl.matkmiec.backend.fish.dto.FishSpeciesDetailsDto;
import pl.matkmiec.backend.fish.model.FishType;
import pl.matkmiec.backend.fish.model.LegalStatus;
import pl.matkmiec.backend.fish.service.FishManagementService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FishAdminControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private FishManagementService fishManagementService;

    @InjectMocks
    private FishAdminController fishAdminController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(fishAdminController)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    @DisplayName("POST /admin/fish should create fish species and return 201 Created")
    void shouldCreateFishSpecies() throws Exception {
        FishSpeciesCreateUpdateDto dto = FishSpeciesCreateUpdateDto.builder()
                .name("Troć wędrowna")
                .latinName("Salmo trutta m. trutta")
                .family("Łososiowate (Salmonidae)")
                .fishType(FishType.SALMONID)
                .legalStatus(LegalStatus.NATIVE)
                .minSizeLimit(35)
                .build();

        FishSpeciesDetailsDto response = FishSpeciesDetailsDto.builder()
                .id(15L)
                .name("Troć wędrowna")
                .slug("troc-wedrowna")
                .build();

        when(fishManagementService.createSpecies(any())).thenReturn(response);

        mockMvc.perform(post("/admin/fish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.name").value("Troć wędrowna"));
    }

    @Test
    @DisplayName("PUT /admin/fish/{id} should update fish species and return 200 OK")
    void shouldUpdateFishSpecies() throws Exception {
        FishSpeciesCreateUpdateDto dto = FishSpeciesCreateUpdateDto.builder()
                .name("Troć jeziorowa")
                .latinName("Salmo trutta m. lacustris")
                .family("Łososiowate (Salmonidae)")
                .fishType(FishType.SALMONID)
                .legalStatus(LegalStatus.NATIVE)
                .minSizeLimit(50)
                .build();

        FishSpeciesDetailsDto response = FishSpeciesDetailsDto.builder()
                .id(15L)
                .name("Troć jeziorowa")
                .build();

        when(fishManagementService.updateSpecies(eq(15L), any())).thenReturn(response);

        mockMvc.perform(put("/admin/fish/15")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Troć jeziorowa"));
    }

    @Test
    @DisplayName("DELETE /admin/fish/{id} should delete species and return 204 No Content")
    void shouldDeleteFishSpecies() throws Exception {
        doNothing().when(fishManagementService).deleteSpecies(15L);

        mockMvc.perform(delete("/admin/fish/15"))
                .andExpect(status().isNoContent());

        verify(fishManagementService).deleteSpecies(15L);
    }

    @Test
    @DisplayName("POST /admin/fish/seed should trigger seeding and return summary")
    void shouldTriggerSeed() throws Exception {
        when(fishManagementService.seedDefaultDatabase(false)).thenReturn(20);

        mockMvc.perform(post("/admin/fish/seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.seededCount").value(20));
    }
}
