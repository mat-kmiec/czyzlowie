package pl.matkmiec.backend.fish.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.matkmiec.backend.fish.dto.FishSpeciesCreateUpdateDto;
import pl.matkmiec.backend.fish.dto.FishSpeciesDetailsDto;
import pl.matkmiec.backend.fish.service.FishManagementService;

import java.util.Map;

@RestController
@RequestMapping({"/admin/fish", "/v1/admin/fish"})
@RequiredArgsConstructor
@Tag(name = "Fish Species Admin", description = "Admin endpoints for creating, updating, deleting and seeding fish species")
public class FishAdminController {

    private final FishManagementService fishManagementService;

    @PostMapping
    @Operation(summary = "Create a new fish species")
    public ResponseEntity<FishSpeciesDetailsDto> createSpecies(@Valid @RequestBody FishSpeciesCreateUpdateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fishManagementService.createSpecies(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing fish species by ID")
    public ResponseEntity<FishSpeciesDetailsDto> updateSpecies(
            @PathVariable Long id,
            @Valid @RequestBody FishSpeciesCreateUpdateDto dto
    ) {
        return ResponseEntity.ok(fishManagementService.updateSpecies(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete fish species by ID")
    public ResponseEntity<Void> deleteSpecies(@PathVariable Long id) {
        fishManagementService.deleteSpecies(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle active visibility status of fish species")
    public ResponseEntity<FishSpeciesDetailsDto> toggleActiveStatus(@PathVariable Long id) {
        return ResponseEntity.ok(fishManagementService.toggleActiveStatus(id));
    }

    @PostMapping("/seed")
    @Operation(summary = "Seed default database of Polish fish species")
    public ResponseEntity<Map<String, Object>> seedDatabase(
            @RequestParam(required = false, defaultValue = "false") boolean overwrite
    ) {
        int count = fishManagementService.seedDefaultDatabase(overwrite);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Pomyślnie załadowano bazę gatunków ryb",
                "seededCount", count
        ));
    }
}
