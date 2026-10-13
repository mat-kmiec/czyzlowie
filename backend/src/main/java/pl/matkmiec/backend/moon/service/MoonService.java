package pl.matkmiec.backend.moon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.matkmiec.backend.airquality.model.GiosStation;
import pl.matkmiec.backend.airquality.repository.GiosStationRepository;
import pl.matkmiec.backend.moon.calculator.LunarCalculator;
import pl.matkmiec.backend.moon.calculator.SolarCalculator;
import pl.matkmiec.backend.moon.calculator.SolunarCalculator;
import pl.matkmiec.backend.moon.dto.*;
import pl.matkmiec.backend.moon.model.MoonGeneralDay;
import pl.matkmiec.backend.moon.model.MoonStationDay;
import pl.matkmiec.backend.moon.repository.MoonGeneralDayRepository;
import pl.matkmiec.backend.moon.repository.MoonStationDayRepository;
import pl.matkmiec.backend.weather.config.CacheConfig;
import pl.matkmiec.backend.weather.model.ImgwMeteoStation;
import pl.matkmiec.backend.weather.model.ImgwSynopStation;
import pl.matkmiec.backend.weather.repository.ImgwMeteoStationRepository;
import pl.matkmiec.backend.weather.repository.ImgwSynopStationRepository;
import pl.matkmiec.backend.weather.util.GeoUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MoonService {

    public static final ZoneId POLAND_ZONE = ZoneId.of("Europe/Warsaw");
    public static final double DEFAULT_LATITUDE = 52.2297;  // Warszawa
    public static final double DEFAULT_LONGITUDE = 21.0122;

    private final ImgwSynopStationRepository synopStationRepo;
    private final ImgwMeteoStationRepository meteoStationRepo;
    private final GiosStationRepository giosStationRepo;
    private final MoonGeneralDayRepository moonGeneralDayRepo;
    private final MoonStationDayRepository moonStationDayRepo;

    @Transactional
    public MoonGenerationResultDto generateYearlyData(int year) {
        long startTime = System.currentTimeMillis();
        log.info("Rozpoczęcie masowego generowania danych astronomiczno-solunarnych dla roku {}", year);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        int totalDays = startDate.lengthOfYear();

        // 1. Obliczenie i zapis danych ogólnopolskich
        List<MoonGeneralDay> generalDays = new ArrayList<>(totalDays);
        Map<LocalDate, GeneralMoonDataDto> generalDataMap = new HashMap<>(totalDays);

        for (int dayOfYear = 1; dayOfYear <= totalDays; dayOfYear++) {
            LocalDate date = startDate.plusDays(dayOfYear - 1);
            GeneralMoonDataDto generalDto = LunarCalculator.calculateGeneralMoonData(date);
            generalDataMap.put(date, generalDto);
            generalDays.add(toMoonGeneralDayEntity(date, generalDto));
        }
        moonGeneralDayRepo.saveAll(generalDays);
        log.info("Zapisano {} rekordów ogólnopolskich dla roku {}", generalDays.size(), year);

        // 2. Pobranie stacji meteo IMGW
        List<ImgwMeteoStation> stations = meteoStationRepo.findAllByIsActiveTrue();
        if (stations.isEmpty()) {
            stations = meteoStationRepo.findAll();
        }
        List<ImgwMeteoStation> validStations = stations.stream()
                .filter(s -> s.getLat() != null && s.getLon() != null)
                .toList();

        log.info("Liczba stacji meteo IMGW do przetworzenia: {}", validStations.size());

        // Usunięcie ewentualnych starych danych dla tego roku
        LocalDate endDate = LocalDate.of(year, 12, 31);
        moonStationDayRepo.deleteByCalculationDateBetween(startDate, endDate);

        long stationDaysCount = 0;
        int batchSize = 500;
        List<MoonStationDay> buffer = new ArrayList<>(batchSize);

        for (ImgwMeteoStation station : validStations) {
            for (int dayOfYear = 1; dayOfYear <= totalDays; dayOfYear++) {
                LocalDate date = startDate.plusDays(dayOfYear - 1);
                GeneralMoonDataDto generalMoon = generalDataMap.get(date);

                SunTimesDto sunTimes = SolarCalculator.calculateSunTimes(station.getLat(), station.getLon(), date, POLAND_ZONE);
                MoonTimesDto moonTimes = LunarCalculator.calculateMoonTimes(station.getLat(), station.getLon(), date, POLAND_ZONE);
                SolunarDayDto solunar = SolunarCalculator.calculateSolunarDay(generalMoon, sunTimes, moonTimes);

                MoonStationDay stationDay = toMoonStationDayEntity(station, date, generalMoon, sunTimes, moonTimes, solunar);
                buffer.add(stationDay);
                stationDaysCount++;

                if (buffer.size() >= batchSize) {
                    moonStationDayRepo.saveAll(buffer);
                    buffer.clear();
                }
            }
        }

        if (!buffer.isEmpty()) {
            moonStationDayRepo.saveAll(buffer);
            buffer.clear();
        }

        long executionTime = System.currentTimeMillis() - startTime;
        log.info("Zakończono generowanie danych astronomiczno-solunarnych dla roku {}. Przetworzono {} stacji, {} rekordów w {} ms",
                year, validStations.size(), stationDaysCount, executionTime);

        return new MoonGenerationResultDto(
                year,
                generalDays.size(),
                validStations.size(),
                stationDaysCount,
                executionTime,
                "SUCCESS",
                String.format("Pomyślnie wygenerowano i zapisano w bazie dane dla roku %d (%d dni ogólnych, %d stacji, %d rekordów stacyjnych)",
                        year, generalDays.size(), validStations.size(), stationDaysCount)
        );
    }

    @Cacheable(value = CacheConfig.CACHE_MOON_DAILY, key = "'daily_' + #lat + '_' + #lon + '_' + #stationId + '_' + #date")
    public MoonDailyDto getDailyData(Double lat, Double lon, String stationId, LocalDate date) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now(POLAND_ZONE);
        LocationInfoDto location = resolveLocation(lat, lon, stationId);

        // 1. Sprawdź, czy dane są już zapisane trwale w bazie danych
        if (location.stationId() != null) {
            Optional<MoonStationDay> stationDayOpt = moonStationDayRepo.findByStationIdAndCalculationDate(location.stationId(), queryDate);
            Optional<MoonGeneralDay> generalDayOpt = moonGeneralDayRepo.findByCalculationDate(queryDate);

            if (stationDayOpt.isPresent() && generalDayOpt.isPresent()) {
                GeneralMoonDataDto generalMoon = toGeneralMoonDataDto(generalDayOpt.get());
                SunTimesDto sunTimes = toSunTimesDto(stationDayOpt.get());
                MoonTimesDto moonTimes = toMoonTimesDto(stationDayOpt.get());
                SolunarDayDto solunar = toSolunarDayDto(stationDayOpt.get());

                return new MoonDailyDto(
                        queryDate,
                        location,
                        generalMoon,
                        sunTimes,
                        moonTimes,
                        solunar
                );
            }
        }

        // 2. Jeśli brak w bazie lub zapytanie dotyczy niestandardowych współrzędnych: oblicz bezpośrednio
        GeneralMoonDataDto generalMoon = LunarCalculator.calculateGeneralMoonData(queryDate);
        SunTimesDto sunTimes = SolarCalculator.calculateSunTimes(location.latitude(), location.longitude(), queryDate, POLAND_ZONE);
        MoonTimesDto moonTimes = LunarCalculator.calculateMoonTimes(location.latitude(), location.longitude(), queryDate, POLAND_ZONE);
        SolunarDayDto solunar = SolunarCalculator.calculateSolunarDay(generalMoon, sunTimes, moonTimes);

        return new MoonDailyDto(
                queryDate,
                location,
                generalMoon,
                sunTimes,
                moonTimes,
                solunar
        );
    }

    public SolunarDayDto getSolunarSummary(Double lat, Double lon, String stationId, LocalDate date) {
        return getDailyData(lat, lon, stationId, date).solunar();
    }

    @Cacheable(value = CacheConfig.CACHE_MOON_MONTHLY, key = "'monthly_' + #lat + '_' + #lon + '_' + #stationId + '_' + #year + '_' + #month")
    public MoonMonthlyDto getMonthlyData(Double lat, Double lon, String stationId, Integer year, Integer month) {
        LocalDate now = LocalDate.now(POLAND_ZONE);
        int queryYear = (year != null) ? year : now.getYear();
        int queryMonth = (month != null) ? month : now.getMonthValue();

        YearMonth ym = YearMonth.of(queryYear, queryMonth);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        LocationInfoDto location = resolveLocation(lat, lon, stationId);

        // 1. Sprawdź, czy dane dla całego miesiąca znajdują się w bazie danych
        if (location.stationId() != null) {
            List<MoonStationDay> stationDays = moonStationDayRepo.findByStationIdAndCalculationDateBetweenOrderByCalculationDateAsc(
                    location.stationId(), startDate, endDate);
            List<MoonGeneralDay> generalDays = moonGeneralDayRepo.findByCalculationDateBetweenOrderByCalculationDateAsc(startDate, endDate);

            if (stationDays.size() == ym.lengthOfMonth() && generalDays.size() == ym.lengthOfMonth()) {
                Map<LocalDate, MoonGeneralDay> generalMap = generalDays.stream()
                        .collect(Collectors.toMap(MoonGeneralDay::getCalculationDate, g -> g));

                List<MoonCalendarDayDto> days = new ArrayList<>(stationDays.size());
                int bestFishingDaysCount = 0;

                for (MoonStationDay sd : stationDays) {
                    MoonGeneralDay gd = generalMap.get(sd.getCalculationDate());
                    MoonPhaseDto phaseDto = MoonPhaseDto.of(gd.getPhaseName(), gd.getPhaseValue() != null ? gd.getPhaseValue() : 0.0);

                    MoonCalendarDayDto calDay = new MoonCalendarDayDto(
                            sd.getCalculationDate(),
                            phaseDto,
                            gd.getIlluminationPercentage() != null ? gd.getIlluminationPercentage() : 0.0,
                            gd.getMoonAgeDays() != null ? gd.getMoonAgeDays() : 0.0,
                            sd.getSunrise(),
                            sd.getSunset(),
                            sd.getMoonrise(),
                            sd.getMoonset(),
                            sd.getSolunarRating() != null ? sd.getSolunarRating() : SolunarActivityLevel.FAIR,
                            sd.getSolunarStars() != null ? sd.getSolunarStars() : 2,
                            sd.getSolunarScore() != null ? sd.getSolunarScore() : 50,
                            Boolean.TRUE.equals(gd.getIsPerigee()),
                            Boolean.TRUE.equals(gd.getIsApogee())
                    );
                    if (sd.getSolunarStars() != null && sd.getSolunarStars() >= 4) {
                        bestFishingDaysCount++;
                    }
                    days.add(calDay);
                }

                MoonGeneralDay firstDay = generalDays.get(0);
                return new MoonMonthlyDto(
                        queryYear,
                        queryMonth,
                        location,
                        days,
                        firstDay.getNextNewMoon(),
                        firstDay.getNextFullMoon(),
                        bestFishingDaysCount
                );
            }
        }

        // 2. Fallback na dynamiczne obliczenia
        List<MoonCalendarDayDto> days = new ArrayList<>(ym.lengthOfMonth());
        int bestFishingDaysCount = 0;

        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate dayDate = ym.atDay(day);
            MoonDailyDto daily = getDailyData(location.latitude(), location.longitude(), location.stationId(), dayDate);

            MoonCalendarDayDto calDay = new MoonCalendarDayDto(
                    dayDate,
                    daily.generalMoon().phase(),
                    daily.generalMoon().illuminationPercentage(),
                    daily.generalMoon().moonAgeDays(),
                    daily.sunTimes().sunrise(),
                    daily.sunTimes().sunset(),
                    daily.moonTimes().moonrise(),
                    daily.moonTimes().moonset(),
                    daily.solunar().overallRating(),
                    daily.solunar().stars(),
                    daily.solunar().overallScore(),
                    daily.generalMoon().isPerigee(),
                    daily.generalMoon().isApogee()
            );

            if (daily.solunar().stars() >= 4) {
                bestFishingDaysCount++;
            }
            days.add(calDay);
        }

        LocalDate firstDay = ym.atDay(1);
        GeneralMoonDataDto generalStart = LunarCalculator.calculateGeneralMoonData(firstDay);

        return new MoonMonthlyDto(
                queryYear,
                queryMonth,
                location,
                days,
                generalStart.nextNewMoon(),
                generalStart.nextFullMoon(),
                bestFishingDaysCount
        );
    }

    @Cacheable(value = CacheConfig.CACHE_MOON_YEARLY, key = "'yearly_' + #year + '_' + #lat + '_' + #lon + '_' + #stationId")
    public MoonYearlyAdminDto getYearlyAdminData(int year, Double lat, Double lon, String stationId) {
        LocationInfoDto location = resolveLocation(lat, lon, stationId);
        LocalDate startDate = LocalDate.of(year, 1, 1);
        int totalDays = startDate.lengthOfYear();

        // 1. Sprawdź, czy dane są w bazie danych
        if (location.stationId() != null) {
            List<MoonStationDay> stationDays = moonStationDayRepo.findByStationIdAndYear(location.stationId(), year);
            List<MoonGeneralDay> generalDays = moonGeneralDayRepo.findByYear(year);

            if (stationDays.size() == totalDays && generalDays.size() == totalDays) {
                Map<LocalDate, MoonGeneralDay> generalMap = generalDays.stream()
                        .collect(Collectors.toMap(MoonGeneralDay::getCalculationDate, g -> g));

                List<MoonCalendarDayDto> days = new ArrayList<>(totalDays);
                int excellentDays = 0, veryGoodDays = 0, goodDays = 0, fairDays = 0, poorDays = 0;
                int perigeeDays = 0, apogeeDays = 0;
                List<LocalDate> fullMoonDates = new ArrayList<>();
                List<LocalDate> newMoonDates = new ArrayList<>();
                List<Map<String, Object>> topFishingDays = new ArrayList<>();

                for (MoonStationDay sd : stationDays) {
                    MoonGeneralDay gd = generalMap.get(sd.getCalculationDate());
                    MoonPhaseDto phaseDto = MoonPhaseDto.of(gd.getPhaseName(), gd.getPhaseValue() != null ? gd.getPhaseValue() : 0.0);

                    MoonCalendarDayDto calDay = new MoonCalendarDayDto(
                            sd.getCalculationDate(),
                            phaseDto,
                            gd.getIlluminationPercentage() != null ? gd.getIlluminationPercentage() : 0.0,
                            gd.getMoonAgeDays() != null ? gd.getMoonAgeDays() : 0.0,
                            sd.getSunrise(),
                            sd.getSunset(),
                            sd.getMoonrise(),
                            sd.getMoonset(),
                            sd.getSolunarRating() != null ? sd.getSolunarRating() : SolunarActivityLevel.FAIR,
                            sd.getSolunarStars() != null ? sd.getSolunarStars() : 2,
                            sd.getSolunarScore() != null ? sd.getSolunarScore() : 50,
                            Boolean.TRUE.equals(gd.getIsPerigee()),
                            Boolean.TRUE.equals(gd.getIsApogee())
                    );
                    days.add(calDay);

                    if (sd.getSolunarRating() != null) {
                        switch (sd.getSolunarRating()) {
                            case EXCELLENT -> excellentDays++;
                            case VERY_GOOD -> veryGoodDays++;
                            case GOOD -> goodDays++;
                            case FAIR -> fairDays++;
                            case POOR -> poorDays++;
                        }
                    }

                    if (Boolean.TRUE.equals(gd.getIsPerigee())) perigeeDays++;
                    if (Boolean.TRUE.equals(gd.getIsApogee())) apogeeDays++;

                    if (gd.getPhaseName() == MoonPhase.FULL_MOON && gd.getIlluminationPercentage() != null && gd.getIlluminationPercentage() >= 98.0) {
                        if (fullMoonDates.isEmpty() || fullMoonDates.get(fullMoonDates.size() - 1).isBefore(sd.getCalculationDate().minusDays(5))) {
                            fullMoonDates.add(sd.getCalculationDate());
                        }
                    }
                    if (gd.getPhaseName() == MoonPhase.NEW_MOON && gd.getIlluminationPercentage() != null && gd.getIlluminationPercentage() <= 2.0) {
                        if (newMoonDates.isEmpty() || newMoonDates.get(newMoonDates.size() - 1).isBefore(sd.getCalculationDate().minusDays(5))) {
                            newMoonDates.add(sd.getCalculationDate());
                        }
                    }

                    if (sd.getSolunarScore() != null && sd.getSolunarScore() >= 85) {
                        topFishingDays.add(Map.of(
                                "date", sd.getCalculationDate().toString(),
                                "score", sd.getSolunarScore(),
                                "phase", gd.getPhasePolishName() != null ? gd.getPhasePolishName() : "",
                                "peakWindows", sd.getPeakWindows() != null ? sd.getPeakWindows() : ""
                        ));
                    }
                }

                Map<String, Object> stats = new LinkedHashMap<>();
                stats.put("totalDays", totalDays);
                stats.put("excellentFishingDays", excellentDays);
                stats.put("veryGoodFishingDays", veryGoodDays);
                stats.put("goodFishingDays", goodDays);
                stats.put("fairFishingDays", fairDays);
                stats.put("poorFishingDays", poorDays);
                stats.put("perigeeSupermoonDays", perigeeDays);
                stats.put("apogeeMicromoonDays", apogeeDays);
                stats.put("fullMoonDates", fullMoonDates);
                stats.put("newMoonDates", newMoonDates);
                stats.put("topFishingDaysCount", topFishingDays.size());
                stats.put("topFishingDays", topFishingDays.stream().limit(20).toList());

                return new MoonYearlyAdminDto(year, location, totalDays, days, stats);
            }
        }

        // 2. Dynamiczny fallback
        List<MoonCalendarDayDto> days = new ArrayList<>(totalDays);
        int excellentDays = 0, veryGoodDays = 0, goodDays = 0, fairDays = 0, poorDays = 0;
        int perigeeDays = 0, apogeeDays = 0;

        List<LocalDate> fullMoonDates = new ArrayList<>();
        List<LocalDate> newMoonDates = new ArrayList<>();
        List<Map<String, Object>> topFishingDays = new ArrayList<>();

        for (int dayOfYear = 1; dayOfYear <= totalDays; dayOfYear++) {
            LocalDate date = startDate.plusDays(dayOfYear - 1);
            MoonDailyDto daily = getDailyData(location.latitude(), location.longitude(), location.stationId(), date);

            MoonCalendarDayDto calDay = new MoonCalendarDayDto(
                    date,
                    daily.generalMoon().phase(),
                    daily.generalMoon().illuminationPercentage(),
                    daily.generalMoon().moonAgeDays(),
                    daily.sunTimes().sunrise(),
                    daily.sunTimes().sunset(),
                    daily.moonTimes().moonrise(),
                    daily.moonTimes().moonset(),
                    daily.solunar().overallRating(),
                    daily.solunar().stars(),
                    daily.solunar().overallScore(),
                    daily.generalMoon().isPerigee(),
                    daily.generalMoon().isApogee()
            );
            days.add(calDay);

            switch (daily.solunar().overallRating()) {
                case EXCELLENT -> excellentDays++;
                case VERY_GOOD -> veryGoodDays++;
                case GOOD -> goodDays++;
                case FAIR -> fairDays++;
                case POOR -> poorDays++;
            }

            if (daily.generalMoon().isPerigee()) perigeeDays++;
            if (daily.generalMoon().isApogee()) apogeeDays++;

            if (daily.generalMoon().phase().phase() == MoonPhase.FULL_MOON && (daily.generalMoon().illuminationPercentage() >= 98.0)) {
                if (fullMoonDates.isEmpty() || fullMoonDates.get(fullMoonDates.size() - 1).isBefore(date.minusDays(5))) {
                    fullMoonDates.add(date);
                }
            }
            if (daily.generalMoon().phase().phase() == MoonPhase.NEW_MOON && (daily.generalMoon().illuminationPercentage() <= 2.0)) {
                if (newMoonDates.isEmpty() || newMoonDates.get(newMoonDates.size() - 1).isBefore(date.minusDays(5))) {
                    newMoonDates.add(date);
                }
            }

            if (daily.solunar().overallScore() >= 85) {
                topFishingDays.add(Map.of(
                        "date", date.toString(),
                        "score", daily.solunar().overallScore(),
                        "phase", daily.generalMoon().phase().polishName(),
                        "peakWindows", daily.solunar().peakFishingWindows()
                ));
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalDays", totalDays);
        stats.put("excellentFishingDays", excellentDays);
        stats.put("veryGoodFishingDays", veryGoodDays);
        stats.put("goodFishingDays", goodDays);
        stats.put("fairFishingDays", fairDays);
        stats.put("poorFishingDays", poorDays);
        stats.put("perigeeSupermoonDays", perigeeDays);
        stats.put("apogeeMicromoonDays", apogeeDays);
        stats.put("fullMoonDates", fullMoonDates);
        stats.put("newMoonDates", newMoonDates);
        stats.put("topFishingDaysCount", topFishingDays.size());
        stats.put("topFishingDays", topFishingDays.stream().limit(20).toList());

        return new MoonYearlyAdminDto(year, location, totalDays, days, stats);
    }

    public List<MoonPhaseDto> getAllMoonPhases() {
        return Arrays.stream(MoonPhase.values())
                .map(p -> MoonPhaseDto.of(p, p.getMinPhase()))
                .toList();
    }

    public LocationInfoDto resolveLocation(Double lat, Double lon, String stationId) {
        if (stationId != null && !stationId.trim().isEmpty()) {
            String sid = stationId.trim();

            // Check IMGW Meteo
            Optional<ImgwMeteoStation> meteo = meteoStationRepo.findById(sid);
            if (meteo.isPresent() && meteo.get().getLat() != null && meteo.get().getLon() != null) {
                return new LocationInfoDto(
                        meteo.get().getLat(),
                        meteo.get().getLon(),
                        meteo.get().getId(),
                        meteo.get().getName() + " (Stacja Meteorologiczna IMGW)",
                        0.0,
                        POLAND_ZONE.getId()
                );
            }

            // Check IMGW Synop
            Optional<ImgwSynopStation> synop = synopStationRepo.findById(sid);
            if (synop.isPresent() && synop.get().getLat() != null && synop.get().getLon() != null) {
                return new LocationInfoDto(
                        synop.get().getLat(),
                        synop.get().getLon(),
                        synop.get().getId(),
                        synop.get().getName() + " (Stacja Synoptyczna IMGW)",
                        0.0,
                        POLAND_ZONE.getId()
                );
            }

            // Check GIOS
            try {
                Long giosId = Long.parseLong(sid);
                Optional<GiosStation> gios = giosStationRepo.findById(giosId);
                if (gios.isPresent() && gios.get().getGegrLat() != null && gios.get().getGegrLon() != null) {
                    return new LocationInfoDto(
                            gios.get().getGegrLat(),
                            gios.get().getGegrLon(),
                            gios.get().getId().toString(),
                            gios.get().getStationName() + " (Stacja GIOŚ)",
                            0.0,
                            POLAND_ZONE.getId()
                    );
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (lat != null && lon != null) {
            String nearestStationId = null;
            String nearestStationName = null;
            Double minDistanceKm = null;

            List<ImgwMeteoStation> meteoStations = meteoStationRepo.findAllByIsActiveTrue();
            if (meteoStations.isEmpty()) {
                meteoStations = meteoStationRepo.findAll();
            }

            Optional<ImgwMeteoStation> nearestMeteo = meteoStations.stream()
                    .filter(s -> s.getLat() != null && s.getLon() != null)
                    .min(Comparator.comparingDouble(s -> GeoUtils.calculateDistanceKm(lat, lon, s.getLat(), s.getLon())));

            if (nearestMeteo.isPresent()) {
                double dist = GeoUtils.calculateDistanceKm(lat, lon, nearestMeteo.get().getLat(), nearestMeteo.get().getLon());
                nearestStationId = nearestMeteo.get().getId();
                nearestStationName = nearestMeteo.get().getName() + " (Stacja Meteorologiczna IMGW)";
                minDistanceKm = dist;
            } else {
                Optional<ImgwSynopStation> nearestSynop = synopStationRepo.findAllByIsActiveTrue().stream()
                        .filter(s -> s.getLat() != null && s.getLon() != null)
                        .min(Comparator.comparingDouble(s -> GeoUtils.calculateDistanceKm(lat, lon, s.getLat(), s.getLon())));

                if (nearestSynop.isPresent()) {
                    double dist = GeoUtils.calculateDistanceKm(lat, lon, nearestSynop.get().getLat(), nearestSynop.get().getLon());
                    nearestStationId = nearestSynop.get().getId();
                    nearestStationName = nearestSynop.get().getName() + " (Stacja Synoptyczna IMGW)";
                    minDistanceKm = dist;
                }
            }

            return new LocationInfoDto(
                    lat,
                    lon,
                    nearestStationId,
                    nearestStationName,
                    minDistanceKm,
                    POLAND_ZONE.getId()
            );
        }

        // Default: Warszawa
        return new LocationInfoDto(
                DEFAULT_LATITUDE,
                DEFAULT_LONGITUDE,
                "352200375",
                "WARSZAWA-OKĘCIE (Domyślna)",
                0.0,
                POLAND_ZONE.getId()
        );
    }

    private MoonGeneralDay toMoonGeneralDayEntity(LocalDate date, GeneralMoonDataDto dto) {
        return MoonGeneralDay.builder()
                .calculationDate(date)
                .phaseName(dto.phase().phase())
                .phasePolishName(dto.phase().polishName())
                .phaseEnglishName(dto.phase().englishName())
                .phaseIconCode(dto.phase().iconCode())
                .phaseWeatherIcon(dto.phase().weatherIcon())
                .phaseValue(dto.phase().phaseValue())
                .illuminationPercentage(dto.illuminationPercentage())
                .moonAgeDays(dto.moonAgeDays())
                .distanceKm(dto.distanceKm())
                .isPerigee(dto.isPerigee())
                .isApogee(dto.isApogee())
                .distancePhenomenon(dto.distancePhenomenon())
                .nextNewMoon(dto.nextNewMoon())
                .nextFirstQuarter(dto.nextFirstQuarter())
                .nextFullMoon(dto.nextFullMoon())
                .nextLastQuarter(dto.nextLastQuarter())
                .build();
    }

    private MoonStationDay toMoonStationDayEntity(ImgwMeteoStation station, LocalDate date, GeneralMoonDataDto generalMoon,
                                                 SunTimesDto sunTimes, MoonTimesDto moonTimes, SolunarDayDto solunar) {
        LocalTime major1Start = null, major1End = null, major2Start = null, major2End = null;
        if (solunar.majorPeriods() != null && !solunar.majorPeriods().isEmpty()) {
            major1Start = solunar.majorPeriods().get(0).startTime();
            major1End = solunar.majorPeriods().get(0).endTime();
            if (solunar.majorPeriods().size() > 1) {
                major2Start = solunar.majorPeriods().get(1).startTime();
                major2End = solunar.majorPeriods().get(1).endTime();
            }
        }

        LocalTime minor1Start = null, minor1End = null, minor2Start = null, minor2End = null;
        if (solunar.minorPeriods() != null && !solunar.minorPeriods().isEmpty()) {
            minor1Start = solunar.minorPeriods().get(0).startTime();
            minor1End = solunar.minorPeriods().get(0).endTime();
            if (solunar.minorPeriods().size() > 1) {
                minor2Start = solunar.minorPeriods().get(1).startTime();
                minor2End = solunar.minorPeriods().get(1).endTime();
            }
        }

        String peakWindowsJoined = (solunar.peakFishingWindows() != null)
                ? String.join("; ", solunar.peakFishingWindows())
                : null;

        return MoonStationDay.builder()
                .station(station)
                .calculationDate(date)
                .sunrise(sunTimes.sunrise())
                .sunset(sunTimes.sunset())
                .solarNoon(sunTimes.solarNoon())
                .dayLengthSeconds(sunTimes.dayLengthSeconds())
                .dayLengthFormatted(sunTimes.dayLength())
                .dayLengthDiffSeconds(sunTimes.dayLengthDiffSeconds())
                .morningGoldenHourStart(sunTimes.morningGoldenHour() != null ? sunTimes.morningGoldenHour().start() : null)
                .morningGoldenHourEnd(sunTimes.morningGoldenHour() != null ? sunTimes.morningGoldenHour().end() : null)
                .eveningGoldenHourStart(sunTimes.eveningGoldenHour() != null ? sunTimes.eveningGoldenHour().start() : null)
                .eveningGoldenHourEnd(sunTimes.eveningGoldenHour() != null ? sunTimes.eveningGoldenHour().end() : null)
                .morningBlueHourStart(sunTimes.morningBlueHour() != null ? sunTimes.morningBlueHour().start() : null)
                .morningBlueHourEnd(sunTimes.morningBlueHour() != null ? sunTimes.morningBlueHour().end() : null)
                .eveningBlueHourStart(sunTimes.eveningBlueHour() != null ? sunTimes.eveningBlueHour().start() : null)
                .eveningBlueHourEnd(sunTimes.eveningBlueHour() != null ? sunTimes.eveningBlueHour().end() : null)
                .civilDawn(sunTimes.civilTwilight() != null ? sunTimes.civilTwilight().dawn() : null)
                .civilDusk(sunTimes.civilTwilight() != null ? sunTimes.civilTwilight().dusk() : null)
                .nauticalDawn(sunTimes.nauticalTwilight() != null ? sunTimes.nauticalTwilight().dawn() : null)
                .nauticalDusk(sunTimes.nauticalTwilight() != null ? sunTimes.nauticalTwilight().dusk() : null)
                .astronomicalDawn(sunTimes.astronomicalTwilight() != null ? sunTimes.astronomicalTwilight().dawn() : null)
                .astronomicalDusk(sunTimes.astronomicalTwilight() != null ? sunTimes.astronomicalTwilight().dusk() : null)
                .moonrise(moonTimes.moonrise())
                .moonset(moonTimes.moonset())
                .moonTransit(moonTimes.moonTransit())
                .moonUnderfoot(moonTimes.moonUnderfoot())
                .moonAltitudeDeg(moonTimes.moonAltitudeDegrees())
                .moonAzimuthDeg(moonTimes.moonAzimuthDegrees())
                .solunarRating(solunar.overallRating())
                .solunarScore(solunar.overallScore())
                .solunarStars(solunar.stars())
                .solunarSummary(solunar.summary())
                .moonPhaseInfluence(solunar.moonPhaseInfluence())
                .fishingTip(solunar.fishingTip())
                .majorPeriod1Start(major1Start)
                .majorPeriod1End(major1End)
                .majorPeriod2Start(major2Start)
                .majorPeriod2End(major2End)
                .minorPeriod1Start(minor1Start)
                .minorPeriod1End(minor1End)
                .minorPeriod2Start(minor2Start)
                .minorPeriod2End(minor2End)
                .peakWindows(peakWindowsJoined)
                .build();
    }

    private GeneralMoonDataDto toGeneralMoonDataDto(MoonGeneralDay entity) {
        MoonPhaseDto phaseDto = MoonPhaseDto.of(
                entity.getPhaseName(),
                entity.getPhaseValue() != null ? entity.getPhaseValue() : entity.getPhaseName().getMinPhase()
        );
        return new GeneralMoonDataDto(
                phaseDto,
                entity.getIlluminationPercentage() != null ? entity.getIlluminationPercentage() : 0.0,
                entity.getMoonAgeDays() != null ? entity.getMoonAgeDays() : 0.0,
                entity.getDistanceKm() != null ? entity.getDistanceKm() : 384400.0,
                Boolean.TRUE.equals(entity.getIsPerigee()),
                Boolean.TRUE.equals(entity.getIsApogee()),
                entity.getDistancePhenomenon() != null ? entity.getDistancePhenomenon() : "Odległość standardowa",
                entity.getNextNewMoon(),
                entity.getNextFirstQuarter(),
                entity.getNextFullMoon(),
                entity.getNextLastQuarter()
        );
    }

    private SunTimesDto toSunTimesDto(MoonStationDay entity) {
        GoldenHourDto morningGolden = (entity.getMorningGoldenHourStart() != null && entity.getMorningGoldenHourEnd() != null)
                ? new GoldenHourDto(entity.getMorningGoldenHourStart(), entity.getMorningGoldenHourEnd(), "Poranna złota godzina (fotografia i wędkarstwo)")
                : null;
        GoldenHourDto eveningGolden = (entity.getEveningGoldenHourStart() != null && entity.getEveningGoldenHourEnd() != null)
                ? new GoldenHourDto(entity.getEveningGoldenHourStart(), entity.getEveningGoldenHourEnd(), "Wieczorna złota godzina (fotografia i wędkarstwo)")
                : null;
        BlueHourDto morningBlue = (entity.getMorningBlueHourStart() != null && entity.getMorningBlueHourEnd() != null)
                ? new BlueHourDto(entity.getMorningBlueHourStart(), entity.getMorningBlueHourEnd())
                : null;
        BlueHourDto eveningBlue = (entity.getEveningBlueHourStart() != null && entity.getEveningBlueHourEnd() != null)
                ? new BlueHourDto(entity.getEveningBlueHourStart(), entity.getEveningBlueHourEnd())
                : null;
        TwilightDto civil = (entity.getCivilDawn() != null || entity.getCivilDusk() != null)
                ? new TwilightDto(entity.getCivilDawn(), entity.getCivilDusk())
                : null;
        TwilightDto nautical = (entity.getNauticalDawn() != null || entity.getNauticalDusk() != null)
                ? new TwilightDto(entity.getNauticalDawn(), entity.getNauticalDusk())
                : null;
        TwilightDto astronomical = (entity.getAstronomicalDawn() != null || entity.getAstronomicalDusk() != null)
                ? new TwilightDto(entity.getAstronomicalDawn(), entity.getAstronomicalDusk())
                : null;

        return new SunTimesDto(
                entity.getSunrise(),
                entity.getSunset(),
                entity.getSolarNoon(),
                entity.getDayLengthFormatted() != null ? entity.getDayLengthFormatted() : "00:00:00",
                entity.getDayLengthSeconds() != null ? entity.getDayLengthSeconds() : 0L,
                entity.getDayLengthDiffSeconds(),
                morningGolden,
                eveningGolden,
                morningBlue,
                eveningBlue,
                civil,
                nautical,
                astronomical
        );
    }

    private MoonTimesDto toMoonTimesDto(MoonStationDay entity) {
        return new MoonTimesDto(
                entity.getMoonrise(),
                entity.getMoonset(),
                entity.getMoonTransit(),
                entity.getMoonUnderfoot(),
                entity.getMoonAltitudeDeg(),
                entity.getMoonAzimuthDeg()
        );
    }

    private SolunarDayDto toSolunarDayDto(MoonStationDay entity) {
        List<SolunarPeriodDto> major = new ArrayList<>();
        if (entity.getMajorPeriod1Start() != null && entity.getMajorPeriod1End() != null) {
            major.add(new SolunarPeriodDto(SolunarPeriodType.MAJOR, entity.getMajorPeriod1Start(), entity.getMajorPeriod1End(),
                    SolunarPeriodType.MAJOR.getPolishName(), "Główny okres aktywności ryb (tranzyt Księżyca)",
                    entity.getSolunarScore() != null ? (int) Math.round(entity.getSolunarScore() * 0.95) : 80));
        }
        if (entity.getMajorPeriod2Start() != null && entity.getMajorPeriod2End() != null) {
            major.add(new SolunarPeriodDto(SolunarPeriodType.MAJOR, entity.getMajorPeriod2Start(), entity.getMajorPeriod2End(),
                    SolunarPeriodType.MAJOR.getPolishName(), "Główny okres aktywności ryb (nadir Księżyca)",
                    entity.getSolunarScore() != null ? (int) Math.round(entity.getSolunarScore() * 0.90) : 75));
        }

        List<SolunarPeriodDto> minor = new ArrayList<>();
        if (entity.getMinorPeriod1Start() != null && entity.getMinorPeriod1End() != null) {
            minor.add(new SolunarPeriodDto(SolunarPeriodType.MINOR, entity.getMinorPeriod1Start(), entity.getMinorPeriod1End(),
                    SolunarPeriodType.MINOR.getPolishName(), "Pomniejszy okres żerowania (wschód Księżyca)",
                    entity.getSolunarScore() != null ? (int) Math.round(entity.getSolunarScore() * 0.70) : 60));
        }
        if (entity.getMinorPeriod2Start() != null && entity.getMinorPeriod2End() != null) {
            minor.add(new SolunarPeriodDto(SolunarPeriodType.MINOR, entity.getMinorPeriod2Start(), entity.getMinorPeriod2End(),
                    SolunarPeriodType.MINOR.getPolishName(), "Pomniejszy okres żerowania (zachód Księżyca)",
                    entity.getSolunarScore() != null ? (int) Math.round(entity.getSolunarScore() * 0.65) : 55));
        }

        List<SolunarPeriodDto> all = new ArrayList<>();
        all.addAll(major);
        all.addAll(minor);

        List<String> peakWindows = new ArrayList<>();
        if (entity.getPeakWindows() != null && !entity.getPeakWindows().isBlank()) {
            peakWindows = Arrays.stream(entity.getPeakWindows().split(";\\s*"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        return new SolunarDayDto(
                entity.getSolunarRating() != null ? entity.getSolunarRating() : SolunarActivityLevel.FAIR,
                entity.getSolunarScore() != null ? entity.getSolunarScore() : 50,
                entity.getSolunarStars() != null ? entity.getSolunarStars() : 2,
                entity.getSolunarSummary() != null ? entity.getSolunarSummary() : "",
                major,
                minor,
                all,
                peakWindows,
                entity.getMoonPhaseInfluence() != null ? entity.getMoonPhaseInfluence() : "",
                entity.getFishingTip() != null ? entity.getFishingTip() : ""
        );
    }
}
