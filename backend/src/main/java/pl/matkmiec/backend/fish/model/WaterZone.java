package pl.matkmiec.backend.fish.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WaterZone {
    BOTTOM("Przydenna (Dno / Bentos)", "Przebywa i żeruje głównie przy dnie zbiornika lub cieku wodnego"),
    MIDWATER("Toń wodna (Pelagial)", "Aktywna w toni wodnej, często patrolująca wolne przestrzenie wodne"),
    SURFACE("Powierzchniowa (Epipelagial)", "Żeruje tuż pod powierzchnią wody, zbierając owady lub małe rybki"),
    LITTORAL("Strefa przybrzeżna (Litoral)", "Przebywa wśród roślinności szuwarowej, grążeli i podwodnych zarośli"),
    ALL_ZONES("Wszystkie partie wody", "Gatunek plastyczny, zmieniający głębokość w zależności od pory dnia i warunków");

    private final String displayName;
    private final String description;
}
