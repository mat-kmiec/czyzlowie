package pl.matkmiec.backend.fish.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FishType {
    PREDATOR("Drapieżnik", "Ryby drapieżne polujące aktywnie na inne ryby i organizmy wodne"),
    PEACEFUL("Spokojnego żeru", "Ryby niedrapieżne żerujące na pokarmie roślinnym, planktonie i bezkręgowcach dennych"),
    OMNIVORE("Wszystkożerna", "Ryby polujące oportunistycznie zarówno na drobne organizmy, jak i pokarm roślinny"),
    SALMONID("Łososiowata", "Szlachetne ryby zimnych, czystych i dobrze natlenionych wód krainy pstrąga i lipienia"),
    INVASIVE("Inwazyjna / Obca", "Gatunki obce i inwazyjne stanowiące zagrożenie dla rodzimej ichtiofauny");

    private final String displayName;
    private final String description;
}
