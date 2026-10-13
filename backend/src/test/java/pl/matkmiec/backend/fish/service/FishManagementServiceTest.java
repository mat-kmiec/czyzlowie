package pl.matkmiec.backend.fish.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.matkmiec.backend.fish.dto.FishSpeciesCreateUpdateDto;
import pl.matkmiec.backend.fish.dto.FishSpeciesDetailsDto;
import pl.matkmiec.backend.fish.exception.FishSpeciesNotFoundException;
import pl.matkmiec.backend.fish.mapper.FishMapper;
import pl.matkmiec.backend.fish.model.FishSpecies;
import pl.matkmiec.backend.fish.model.FishType;
import pl.matkmiec.backend.fish.model.LegalStatus;
import pl.matkmiec.backend.fish.repository.FishSpeciesRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FishManagementServiceTest {

    @Mock
    private FishSpeciesRepository fishSpeciesRepository;

    @Mock
    private FishDataInitializer fishDataInitializer;

    @Spy
    private FishMapper fishMapper = new FishMapper();

    @InjectMocks
    private FishManagementService fishManagementService;

    private FishSpecies sandacz;

    @BeforeEach
    void setUp() {
        sandacz = FishSpecies.builder()
                .id(10L)
                .slug("sandacz")
                .name("Sandacz")
                .latinName("Sander lucioperca")
                .family("Okoniowate (Percidae)")
                .fishType(FishType.PREDATOR)
                .legalStatus(LegalStatus.NATIVE)
                .minSizeLimit(50)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should create new fish species successfully")
    void shouldCreateFishSpecies() {
        FishSpeciesCreateUpdateDto dto = FishSpeciesCreateUpdateDto.builder()
                .name("Sandacz")
                .latinName("Sander lucioperca")
                .family("Okoniowate (Percidae)")
                .fishType(FishType.PREDATOR)
                .legalStatus(LegalStatus.NATIVE)
                .minSizeLimit(50)
                .build();

        when(fishSpeciesRepository.save(any(FishSpecies.class))).thenAnswer(inv -> {
            FishSpecies entity = inv.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        FishSpeciesDetailsDto result = fishManagementService.createSpecies(dto);

        assertNotNull(result);
        assertEquals("Sandacz", result.getName());
        assertEquals("sandacz", result.getSlug());
        verify(fishSpeciesRepository).save(any(FishSpecies.class));
    }

    @Test
    @DisplayName("Should update existing fish species successfully")
    void shouldUpdateFishSpecies() {
        FishSpeciesCreateUpdateDto updateDto = FishSpeciesCreateUpdateDto.builder()
                .name("Sandacz Zaktualizowany")
                .latinName("Sander lucioperca")
                .family("Okoniowate (Percidae)")
                .fishType(FishType.PREDATOR)
                .legalStatus(LegalStatus.NATIVE)
                .minSizeLimit(55)
                .build();

        when(fishSpeciesRepository.findById(10L)).thenReturn(Optional.of(sandacz));
        when(fishSpeciesRepository.save(any(FishSpecies.class))).thenAnswer(inv -> inv.getArgument(0));

        FishSpeciesDetailsDto result = fishManagementService.updateSpecies(10L, updateDto);

        assertNotNull(result);
        assertEquals("Sandacz Zaktualizowany", result.getName());
        assertEquals(55, result.getLegalInfo().getMinSizeLimit());
    }

    @Test
    @DisplayName("Should delete fish species by ID")
    void shouldDeleteFishSpecies() {
        when(fishSpeciesRepository.findById(10L)).thenReturn(Optional.of(sandacz));

        fishManagementService.deleteSpecies(10L);

        verify(fishSpeciesRepository).delete(sandacz);
    }

    @Test
    @DisplayName("Should toggle active status of fish species")
    void shouldToggleActiveStatus() {
        when(fishSpeciesRepository.findById(10L)).thenReturn(Optional.of(sandacz));
        when(fishSpeciesRepository.save(any(FishSpecies.class))).thenAnswer(inv -> inv.getArgument(0));

        FishSpeciesDetailsDto result = fishManagementService.toggleActiveStatus(10L);

        assertNotNull(result);
        assertFalse(sandacz.getIsActive());
    }

    @Test
    @DisplayName("Should throw FishSpeciesNotFoundException when updating non-existing species")
    void shouldThrowWhenUpdatingNotFound() {
        when(fishSpeciesRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(FishSpeciesNotFoundException.class, () ->
                fishManagementService.updateSpecies(999L, new FishSpeciesCreateUpdateDto()));
    }

    @Test
    @DisplayName("Should seed default database when requested")
    void shouldSeedDefaultDatabase() {
        when(fishDataInitializer.seedInitialData()).thenReturn(20);

        int count = fishManagementService.seedDefaultDatabase(false);

        assertEquals(20, count);
        verify(fishDataInitializer).seedInitialData();
        verify(fishSpeciesRepository, never()).deleteAll();
    }
}
