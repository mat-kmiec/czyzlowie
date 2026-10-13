package pl.matkmiec.backend.fish.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.fish.model.FishSpecies;
import pl.matkmiec.backend.fish.repository.FishSpeciesRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FishDataInitializer implements ApplicationRunner {

    private final FishSpeciesRepository fishSpeciesRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long count = fishSpeciesRepository.count();
        if (count == 0) {
            log.info("Baza danych gatunków ryb jest pusta. Inicjalizuję domyślny katalog polskich gatunków ryb...");
            seedInitialData();
        } else {
            log.info("W bazie danych znajduje się już {} gatunków ryb. Pomijam początkowy seed.", count);
        }
    }

    @Transactional
    public int seedInitialData() {
        List<FishSpecies> initialList = DefaultFishSpeciesProvider.getInitialSpeciesList();
        int savedCount = 0;

        for (FishSpecies species : initialList) {
            if (fishSpeciesRepository.findBySlug(species.getSlug()).isEmpty() &&
                fishSpeciesRepository.findByNameIgnoreCase(species.getName()).isEmpty()) {
                fishSpeciesRepository.save(species);
                savedCount++;
            }
        }

        log.info("Pomyślnie zainicjalizowano {} nowych gatunków ryb.", savedCount);
        return savedCount;
    }
}
