package pl.matkmiec.backend.moon.dto;

public record LocationInfoDto(
        double latitude,
        double longitude,
        String stationId,
        String stationName,
        Double distanceToStationKm,
        String timeZone
) {
}
