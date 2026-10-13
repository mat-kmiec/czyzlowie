package pl.matkmiec.backend.fish.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DifficultyLevel {
    EASY("Łatwy", "Gatunek powszechny, chętnie współpracujący i stosunkowo prosty do złowienia dla początkujących"),
    MEDIUM("Średni", "Wymaga podstawowej znajomości łowiska, odpowiedniego doboru sprzętu i przynęt"),
    HARD("Trudny", "Ostrożna ryba wymagająca precyzji, cichego zachowania i dopracowanej techniki połowu"),
    EXPERT("Ekspert", "Gatunek bardzo płochliwy lub rzadki, wymagający specjalistycznej wiedzy, sprzętu i wielogodzinnej cierpliwości");

    private final String displayName;
    private final String description;
}
