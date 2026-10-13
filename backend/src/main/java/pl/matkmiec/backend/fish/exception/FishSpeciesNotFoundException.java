package pl.matkmiec.backend.fish.exception;

public class FishSpeciesNotFoundException extends RuntimeException {
    public FishSpeciesNotFoundException(String message) {
        super(message);
    }

    public FishSpeciesNotFoundException(Long id) {
        super("Nie znaleziono gatunku ryby o identyfikatorze ID: " + id);
    }
}
