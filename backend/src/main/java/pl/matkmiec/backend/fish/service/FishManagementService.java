package pl.matkmiec.backend.fish.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.fish.dto.FishSpeciesCreateUpdateDto;
import pl.matkmiec.backend.fish.dto.FishSpeciesDetailsDto;
import pl.matkmiec.backend.fish.exception.FishSpeciesNotFoundException;
import pl.matkmiec.backend.fish.mapper.FishMapper;
import pl.matkmiec.backend.fish.model.FishSpecies;
import pl.matkmiec.backend.fish.repository.FishSpeciesRepository;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FishManagementService {

    private final FishSpeciesRepository fishSpeciesRepository;
    private final FishMapper fishMapper;
    private final FishDataInitializer fishDataInitializer;

    public int seedDefaultDatabase(boolean overwrite) {
        if (overwrite) {
            log.warn("Wymuszenie przeładowania bazy danych gatunków ryb (usunięcie dotychczasowych)");
            fishSpeciesRepository.deleteAll();
        }
        return fishDataInitializer.seedInitialData();
    }

    public FishSpeciesDetailsDto createSpecies(FishSpeciesCreateUpdateDto dto) {
        log.info("Tworzenie nowego gatunku ryby: {}", dto.getName());
        FishSpecies entity = fishMapper.toEntity(dto);
        FishSpecies saved = fishSpeciesRepository.save(entity);
        return fishMapper.toDetailsDto(saved, LocalDate.now());
    }

    public FishSpeciesDetailsDto updateSpecies(Long id, FishSpeciesCreateUpdateDto dto) {
        log.info("Aktualizacja gatunku ryby o ID: {}", id);
        FishSpecies entity = fishSpeciesRepository.findById(id)
                .orElseThrow(() -> new FishSpeciesNotFoundException(id));

        fishMapper.updateEntityFromDto(entity, dto);
        FishSpecies updated = fishSpeciesRepository.save(entity);
        return fishMapper.toDetailsDto(updated, LocalDate.now());
    }

    public void deleteSpecies(Long id) {
        log.info("Usuwanie gatunku ryby o ID: {}", id);
        FishSpecies entity = fishSpeciesRepository.findById(id)
                .orElseThrow(() -> new FishSpeciesNotFoundException(id));
        fishSpeciesRepository.delete(entity);
    }

    public FishSpeciesDetailsDto toggleActiveStatus(Long id) {
        FishSpecies entity = fishSpeciesRepository.findById(id)
                .orElseThrow(() -> new FishSpeciesNotFoundException(id));

        entity.setIsActive(entity.getIsActive() == null || !entity.getIsActive());
        FishSpecies updated = fishSpeciesRepository.save(entity);
        log.info("Zmieniono status aktywności gatunku {} (ID: {}) na: {}", entity.getName(), id, entity.getIsActive());
        return fishMapper.toDetailsDto(updated, LocalDate.now());
    }
}
