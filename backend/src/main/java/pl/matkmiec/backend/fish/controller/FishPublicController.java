package pl.matkmiec.backend.fish.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.matkmiec.backend.fish.dto.*;
import pl.matkmiec.backend.fish.model.FishType;
import pl.matkmiec.backend.fish.model.LegalStatus;
import pl.matkmiec.backend.fish.service.FishQueryService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/v1/fish", "/public/fish"})
@RequiredArgsConstructor
@Tag(name = "Fish Species Public", description = "Public endpoints for fish species catalog, categories, detailed profiles, regulations, biology, records, and tactics")
public class FishPublicController {

    private final FishQueryService fishQueryService;

    @GetMapping
    @Operation(summary = "Get list of fish species with flexible filtering",
            description = "Returns compact list of fish species. Supports filtering by type (predator, peaceful, etc.), legal status, text search, active month, and reference date.")
    public ResponseEntity<List<FishSpeciesListItemDto>> getSpeciesList(
            @Parameter(description = "Category/type of fish (PREDATOR, PEACEFUL, OMNIVORE, SALMONID, INVASIVE)")
            @RequestParam(required = false) FishType type,
            @Parameter(description = "Legal protection status (NATIVE, STRICTLY_PROTECTED, INVASIVE_NO_RELEASE, COMMERCIAL_REGULATED)")
            @RequestParam(required = false) LegalStatus legalStatus,
            @Parameter(description = "Search query (matches Polish name, Latin name, family, description, similar species)")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by active month (1-12)")
            @RequestParam(required = false) Integer activeMonth,
            @Parameter(description = "Reference date for active protection checks (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(fishQueryService.getSpeciesList(type, legalStatus, search, activeMonth, date));
    }

    @GetMapping("/{idOrSlug}")
    @Operation(summary = "Get full detailed profile of a fish species by ID or slug",
            description = "Returns comprehensive data divided into 6 major sections: I. Basic info, II. Regulations & legal status, III. Biology & records, IV. Fishing tactics & tips, V. Tackle parameters guide, VI. Catch & Release & safety.")
    public ResponseEntity<FishSpeciesDetailsDto> getSpeciesDetails(
            @Parameter(description = "Fish ID (e.g. 1) or URL slug (e.g. 'szczupak-pospolity', 'sandacz')")
            @PathVariable String idOrSlug,
            @Parameter(description = "Reference date for checking active protection period (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(fishQueryService.getSpeciesByIdOrSlug(idOrSlug, date));
    }

    @GetMapping("/{idOrSlug}/tackle")
    @Operation(summary = "Get technical tackle guide for a specific fish species",
            description = "Returns recommended monofilament line, braided line, hook/treble sizes, rod casting weight (CW), rod action, and leader requirements.")
    public ResponseEntity<FishTackleGuideDto> getSpeciesTackleGuide(
            @Parameter(description = "Fish ID or URL slug") @PathVariable String idOrSlug
    ) {
        return ResponseEntity.ok(fishQueryService.getTackleGuide(idOrSlug));
    }

    @GetMapping("/{idOrSlug}/catch-and-release")
    @Operation(summary = "Get Catch & Release and safety handling guide for a specific fish species",
            description = "Returns ethical handling guidelines: landing methods, unhooking tools, unhooking mat requirements, safety alerts (teeth, spines, gill plates), and fish care tips.")
    public ResponseEntity<FishCatchAndReleaseDto> getSpeciesCatchAndReleaseGuide(
            @Parameter(description = "Fish ID or URL slug") @PathVariable String idOrSlug
    ) {
        return ResponseEntity.ok(fishQueryService.getCatchAndReleaseGuide(idOrSlug));
    }

    @GetMapping("/tackle-guide")
    @Operation(summary = "Get tackle parameters summary table for all fish species",
            description = "Returns overview table of tackle recommendations (lines, hooks, rod CW, leader) for all species or filtered by fish category.")
    public ResponseEntity<List<FishTackleSummaryDto>> getAllTackleGuides(
            @Parameter(description = "Optional filter by fish category (PREDATOR, PEACEFUL, SALMONID, etc.)")
            @RequestParam(required = false) FishType type
    ) {
        return ResponseEntity.ok(fishQueryService.getAllTackleGuides(type));
    }

    @GetMapping("/safety-alerts")
    @Operation(summary = "Get fish safety alerts and handling warnings catalog",
            description = "Returns catalog of species with potential safety hazards (venomous spines, sharp teeth, sharp gill covers) and required protective care.")
    public ResponseEntity<List<FishSafetyAlertDto>> getSafetyAlerts() {
        return ResponseEntity.ok(fishQueryService.getSafetyAlerts());
    }

    @GetMapping("/categories")
    @Operation(summary = "Get fish categories summary and statistics",
            description = "Returns summary of all categories (Predators, Peaceful, Salmonids, Invasive, etc.) with count and top species.")
    public ResponseEntity<List<FishCategorySummaryDto>> getCategories(
            @Parameter(description = "Reference date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(fishQueryService.getCategoriesSummary(date));
    }

    @GetMapping("/calendar")
    @Operation(summary = "Get fishing calendar and seasonal feeding activity for a month",
            description = "Returns active feeding fish, species in protection period, and seasonal tactics for the selected month (1-12).")
    public ResponseEntity<FishCalendarMonthDto> getCalendar(
            @Parameter(description = "Month number (1-12, default: current month)")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Reference date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(fishQueryService.getCalendarMonth(month, date));
    }

    @GetMapping("/records")
    @Operation(summary = "Get Polish official fish records leaderboard",
            description = "Returns ranking of official Polish records (weight, length, year, angler) for species.")
    public ResponseEntity<List<FishRecordLeaderboardDto>> getRecords() {
        return ResponseEntity.ok(fishQueryService.getRecordLeaderboard());
    }

    @GetMapping("/protection")
    @Operation(summary = "Get legal protection status and size limits overview",
            description = "Returns active protection periods for current date, strictly protected species, invasive species with release prohibition, and size limits.")
    public ResponseEntity<FishProtectionOverviewDto> getProtectionOverview(
            @Parameter(description = "Reference date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(fishQueryService.getProtectionOverview(date));
    }

    @GetMapping("/random")
    @Operation(summary = "Get random fish species of the day",
            description = "Returns full details of a randomly selected active fish species.")
    public ResponseEntity<FishSpeciesDetailsDto> getRandomSpecies(
            @Parameter(description = "Reference date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(fishQueryService.getRandomSpecies(date));
    }

    @GetMapping("/search")
    @Operation(summary = "Autocomplete and quick search for fish species",
            description = "Returns instant search suggestions by name and Latin name for frontend search bars.")
    public ResponseEntity<List<FishSearchSuggestionDto>> searchAutocomplete(
            @Parameter(description = "Query text") @RequestParam String query,
            @Parameter(description = "Max results (default: 8)") @RequestParam(required = false, defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(fishQueryService.searchAutocomplete(query, limit));
    }

    @GetMapping("/enums/types")
    @Operation(summary = "Get list of fish types/categories dictionary")
    public ResponseEntity<List<FishEnumOptionDto>> getFishTypes() {
        return ResponseEntity.ok(fishQueryService.getFishTypes());
    }

    @GetMapping("/enums/legal-statuses")
    @Operation(summary = "Get list of legal protection statuses dictionary")
    public ResponseEntity<List<FishEnumOptionDto>> getLegalStatuses() {
        return ResponseEntity.ok(fishQueryService.getLegalStatuses());
    }

    @GetMapping("/enums/water-zones")
    @Operation(summary = "Get list of water feeding zones dictionary")
    public ResponseEntity<List<FishEnumOptionDto>> getWaterZones() {
        return ResponseEntity.ok(fishQueryService.getWaterZones());
    }

    @GetMapping("/enums/difficulty-levels")
    @Operation(summary = "Get list of fishing difficulty levels dictionary")
    public ResponseEntity<List<FishEnumOptionDto>> getDifficultyLevels() {
        return ResponseEntity.ok(fishQueryService.getDifficultyLevels());
    }
}
