package com.teslamate.query.service;

import com.teslamate.query.dao.AddressDao;
import com.teslamate.query.dao.CarDao;
import com.teslamate.query.dao.ChargeDao;
import com.teslamate.query.dao.ChargingProcessDao;
import com.teslamate.query.dao.DriveDao;
import com.teslamate.query.dao.GeofenceDao;
import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.db.condition.ChargingProcessSearchCondition;
import com.teslamate.query.db.condition.DriveSearchCondition;
import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.dto.DailyOccupancyDto;
import com.teslamate.query.dto.DayGridCellDto;
import com.teslamate.query.dto.MapPointDto;
import com.teslamate.query.dto.MapTracksDto;
import com.teslamate.query.dto.TimelineItemDto;
import com.teslamate.query.dto.TimelineKind;
import com.teslamate.query.entity.AddressEntity;
import com.teslamate.query.entity.CarEntity;
import com.teslamate.query.entity.ChargeEntity;
import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.GeofenceEntity;
import com.teslamate.query.entity.PositionEntity;
import com.teslamate.query.entity.PositionPathPoint;
import com.teslamate.query.exception.NotFoundException;
import com.teslamate.query.service.trip.ActivitySpan;
import com.teslamate.query.service.trip.ActivityTimelineComposer;
import com.teslamate.query.service.trip.DailyOccupancy;
import com.teslamate.query.service.trip.DayGrid;
import com.teslamate.query.service.trip.DaySplit;
import com.teslamate.query.service.trip.PathGeometry;
import com.teslamate.query.service.trip.MapStopMerge;
import com.teslamate.query.service.trip.PathSimplify;
import com.teslamate.query.service.trip.PlaceLabel;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Time-window trip view: chronological DRIVE/CHARGE/PARK log plus map rows / GeoJSON.
 */
@Service
public class TripViewService {

    private static final Logger log = LoggerFactory.getLogger(TripViewService.class);

    static final int DEFAULT_MIN_PARK_MIN = 10;
    /** Safety cap only. Timeline lists every overlapping drive/charge below this. */
    static final int DEFAULT_DRIVE_LIMIT = 2_000;
    static final int DEFAULT_CHARGE_LIMIT = 1_000;
    static final int MAX_CHEVRONS = 80;
    static final int AXIS_MIN_VISUAL_MIN = 12;
    static final String COLOR_DRIVE = "#3b82f6";
    static final String COLOR_CHARGE = "#22c55e";
    static final String COLOR_CHARGE_DC = "#f59e0b";
    static final String COLOR_PARK = "#94a3b8";

    private final CarDao carDao;
    private final DriveDao driveDao;
    private final ChargingProcessDao chargingProcessDao;
    private final ChargeDao chargeDao;
    private final PositionDao positionDao;
    private final AddressDao addressDao;
    private final GeofenceDao geofenceDao;
    private final QuerySupport support;
    private final Clock clock;
    private final Cache<String, Snapshot> windowCache = Caffeine.newBuilder()
            .maximumSize(64)
            .expireAfterWrite(20, TimeUnit.SECONDS)
            .build();

    public TripViewService(
            CarDao carDao,
            DriveDao driveDao,
            ChargingProcessDao chargingProcessDao,
            ChargeDao chargeDao,
            PositionDao positionDao,
            AddressDao addressDao,
            GeofenceDao geofenceDao,
            QuerySupport support,
            Clock clock
    ) {
        this.carDao = carDao;
        this.driveDao = driveDao;
        this.chargingProcessDao = chargingProcessDao;
        this.chargeDao = chargeDao;
        this.positionDao = positionDao;
        this.addressDao = addressDao;
        this.geofenceDao = geofenceDao;
        this.support = support;
        this.clock = clock;
    }

    public List<TimelineItemDto> timeline(long carId, String fromStr, String toStr, Integer minParkMin,
                                          DisplayUnits units, ZoneId zone) {
        return timeline(carId, fromStr, toStr, minParkMin, units, zone, null, null, null, null, null, null, 4);
    }

    public List<TimelineItemDto> timeline(long carId, String fromStr, String toStr, Integer minParkMin,
                                          DisplayUnits units, ZoneId zone,
                                          String hlDay, String hlSlot, String hlKind, String hlFrom, String hlTo,
                                          int dayStartHour) {
        return timeline(carId, fromStr, toStr, minParkMin, units, zone,
                hlDay, hlSlot, hlKind, hlFrom, hlTo, null, dayStartHour);
    }

    public List<TimelineItemDto> timeline(long carId, String fromStr, String toStr, Integer minParkMin,
                                          DisplayUnits units, ZoneId zone,
                                          String hlDay, String hlSlot, String hlKind, String hlFrom, String hlTo,
                                          String hlId, int dayStartHour) {
        List<TimelineItemDto> items = build(carId, fromStr, toStr, minParkMin, units, false, zone).timeline;
        Instant[] win = resolveFocusWindow(items, hlDay, hlSlot, hlKind, hlFrom, hlTo, hlId, zone, dayStartHour);
        Long focusId = parseFlexibleLong(hlId);
        int want = focusKindCode(hlKind);
        if (win == null && focusId == null) {
            return items;
        }
        List<TimelineItemDto> out = new ArrayList<>(items.size());
        for (TimelineItemDto item : items) {
            boolean kindOk = want == 0 || kindCode(item.kind()) == want;
            boolean on = kindOk && (
                    (focusId != null && focusId.equals(item.id()))
                            || (win != null && overlapsWindow(item, win[0], win[1])));
            out.add(on ? item.withHighlight(1) : item);
        }
        return out;
    }

    public List<DailyOccupancyDto> dailyOccupancy(long carId, String fromStr, String toStr, Integer minParkMin,
                                                  DisplayUnits units, ZoneId zone) {
        return DailyOccupancy.from(timeline(carId, fromStr, toStr, minParkMin, units, zone));
    }

    public List<DayGridCellDto> grid(long carId, String fromStr, String toStr, Integer minParkMin,
                                     DisplayUnits units, ZoneId zone, int dayStartHour) {
        return grid(carId, fromStr, toStr, minParkMin, units, zone, dayStartHour,
                null, null, null, null, null, null);
    }

    public List<DayGridCellDto> grid(long carId, String fromStr, String toStr, Integer minParkMin,
                                     DisplayUnits units, ZoneId zone, int dayStartHour,
                                     String hlDay, String hlSlot, String hlKind, String hlFrom, String hlTo,
                                     String hlId) {
        ZoneId z = zone == null ? ZoneId.of("Asia/Shanghai") : zone;
        Instant[] range = support.requireRange(fromStr, toStr);
        List<TimelineItemDto> items = timeline(carId, fromStr, toStr, minParkMin, units, z);
        List<DayGridCellDto> cells = DayGrid.paintFromTimeline(items, z, dayStartHour, range[0], range[1]);
        Instant[] win = resolveFocusWindow(items, hlDay, hlSlot, hlKind, hlFrom, hlTo, hlId, z, dayStartHour);
        return DayGrid.applyHighlight(cells, win == null ? null : win[0],
                win == null ? null : win[1], focusKindCode(hlKind), dayStartHour, z);
    }

    /**
     * Points for a focused trip. Timeline passes {@code id} (and from/to);
     * the day-grid passes {@code day}+{@code slot}. Unset vars return no rows.
     */
    public List<MapPointDto> focus(long carId, String dayStr, String slotStr, String kindStr,
                                   String fromStr, String toStr,
                                   Integer minParkMin, DisplayUnits units, ZoneId zone, int dayStartHour) {
        return focus(carId, dayStr, slotStr, kindStr, fromStr, toStr, null,
                minParkMin, units, zone, dayStartHour);
    }

    public List<MapPointDto> focus(long carId, String dayStr, String slotStr, String kindStr,
                                   String fromStr, String toStr, String idStr,
                                   Integer minParkMin, DisplayUnits units, ZoneId zone, int dayStartHour) {
        ZoneId z = zone == null ? ZoneId.of("Asia/Shanghai") : zone;
        int want = focusKindCode(kindStr);
        Instant[] byId = rangeForSourceId(carId, parseFlexibleLong(idStr), want);
        Instant[] raw = byId != null
                ? byId
                : parseFocusBounds(dayStr, slotStr, fromStr, toStr, z, dayStartHour);
        if (raw == null) {
            return List.of();
        }
        Instant[] win = byId != null ? byId : expandFocus(
                timeline(carId, raw[0].minusSeconds(90).toString(),
                        raw[1].plusSeconds(90).toString(), minParkMin, units, z),
                raw, want);
        String kinds = switch (want) {
            case DayGridCellDto.DRIVE -> "drive";
            case DayGridCellDto.CHARGE -> "charge";
            case DayGridCellDto.PARK -> "park";
            default -> "drive,charge,park";
        };
        return points(carId, win[0].toString(), win[1].toString(), minParkMin, kinds, units);
    }

    private Instant[] rangeForSourceId(long carId, Long id, int want) {
        if (id == null) {
            return null;
        }
        if (want == DayGridCellDto.CHARGE || want == 0) {
            Instant[] charge = chargingProcessDao.findById(id)
                    .filter(c -> Objects.equals(c.carId(), carId))
                    .map(c -> closedRange(c.startDate(), c.endDate()))
                    .orElse(null);
            if (charge != null) {
                return charge;
            }
        }
        if (want == DayGridCellDto.DRIVE || want == 0) {
            return driveDao.findById(id)
                    .filter(d -> Objects.equals(d.carId(), carId))
                    .map(d -> closedRange(d.startDate(), d.endDate()))
                    .orElse(null);
        }
        return null;
    }

    private static Instant[] closedRange(Instant start, Instant end) {
        if (start == null) {
            return null;
        }
        Instant to = end == null || !end.isAfter(start) ? start.plusSeconds(60) : end;
        return new Instant[]{start, to};
    }

    private Instant[] resolveFocusWindow(List<TimelineItemDto> items, String hlDay, String hlSlot,
                                         String hlKind, String hlFrom, String hlTo, String hlId,
                                         ZoneId zone, int dayStartHour) {
        Long focusId = parseFlexibleLong(hlId);
        int want = focusKindCode(hlKind);
        if (focusId != null && items != null) {
            List<TimelineItemDto> byId = items.stream()
                    .filter(i -> focusId.equals(i.id()))
                    .filter(i -> want == 0 || kindCode(i.kind()) == want)
                    .toList();
            if (!byId.isEmpty()) {
                return expandFocus(byId, new Instant[]{byId.getFirst().start(), byId.getFirst().end()}, want);
            }
        }
        Instant[] raw = parseFocusBounds(hlDay, hlSlot, hlFrom, hlTo, zone, dayStartHour);
        if (raw == null) {
            return null;
        }
        return expandFocus(items, raw, want);
    }

    private Long parseFlexibleLong(String raw) {
        if (support.unset(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return DayGrid.parseSourceId(raw);
        }
    }

    private Instant[] parseFocusBounds(String dayStr, String slotStr, String fromStr, String toStr,
                                       ZoneId zone, int dayStartHour) {
        ZoneId z = zone == null ? ZoneId.of("Asia/Shanghai") : zone;
        if (!support.unset(fromStr) && !support.unset(toStr)) {
            Instant from = parseFlexibleInstant(fromStr);
            Instant to = parseFlexibleInstant(toStr);
            if (from == null || to == null) {
                return null;
            }
            if (!to.isAfter(from)) {
                to = from.plusSeconds(60);
            }
            return new Instant[]{from, to};
        }
        if (support.unset(dayStr) || support.unset(slotStr)) {
            return null;
        }
        Integer clockH = DayGrid.parseClockHour(slotStr);
        Instant dayInstant = parseFlexibleInstant(dayStr);
        if (clockH == null || dayInstant == null) {
            return null;
        }
        LocalDate day = dayInstant.atZone(z).toLocalDate();
        int startH = Math.floorMod(dayStartHour, 24);
        ZonedDateTime slotStart = day.atTime(clockH, 0).atZone(z);
        if (clockH < startH) {
            slotStart = slotStart.plusDays(1);
        }
        return new Instant[]{slotStart.toInstant(), slotStart.plusHours(1).toInstant()};
    }

    private Instant[] expandFocus(List<TimelineItemDto> items, Instant[] raw, int want) {
        if (items == null || raw == null) {
            return raw;
        }
        List<TimelineItemDto> hit = items.stream()
                .filter(i -> overlapsWindow(i, raw[0], raw[1]))
                .filter(i -> want == 0 || kindCode(i.kind()) == want)
                .toList();
        if (hit.isEmpty()) {
            return raw;
        }
        Instant expFrom = hit.stream().map(TimelineItemDto::start).filter(Objects::nonNull)
                .min(Instant::compareTo).orElse(raw[0]);
        Instant expTo = hit.stream().map(TimelineItemDto::end).filter(Objects::nonNull)
                .max(Instant::compareTo).orElse(raw[1]);
        return new Instant[]{expFrom, expTo};
    }

    public List<MapPointDto> points(long carId, String fromStr, String toStr, Integer minParkMin,
                                    String kinds, DisplayUnits units) {
        Snapshot snap = build(carId, fromStr, toStr, minParkMin, units, true, null);
        Set<String> want = parseKinds(kinds);
        return snap.points.stream().filter(p -> want.contains(p.kind())).toList();
    }

    public MapTracksDto geoJson(long carId, String fromStr, String toStr, Integer minParkMin,
                                DisplayUnits units) {
        return build(carId, fromStr, toStr, minParkMin, units, true, null).geoJson;
    }

    private Snapshot build(long carId, String fromStr, String toStr, Integer minParkMin, DisplayUnits units,
                           boolean includePath, ZoneId zone) {
        int minPark = minParkMin == null ? DEFAULT_MIN_PARK_MIN : Math.max(minParkMin, 0);
        ZoneId z = zone == null ? ZoneId.of("Asia/Shanghai") : zone;
        String unitKey = units == null ? "km-C" : units.length() + "-" + units.temperature();
        String key = carId + "|" + fromStr + "|" + toStr + "|" + minPark + "|" + unitKey + "|" + includePath + "|" + z.getId();
        return windowCache.get(key, k -> buildUncached(carId, fromStr, toStr, minPark, units, includePath, z));
    }

    private Snapshot buildUncached(long carId, String fromStr, String toStr, int minPark, DisplayUnits units,
                                   boolean includePath, ZoneId zone) {
        long started = System.nanoTime();
        requireCar(carId);
        DisplayUnits u = units == null ? DisplayUnits.METRIC : units;
        Instant[] range = support.requireRange(fromStr, toStr);
        Instant from = range[0];
        Instant to = range[1];
        Instant now = clock.instant();

        List<DriveEntity> windowDrives = loadOverlappingDrives(carId, from, to);
        List<ChargingProcessEntity> charges = loadOverlappingCharges(carId, from, to);
        Instant composeFrom = from;
        if (windowDrives.size() == DEFAULT_DRIVE_LIMIT && !windowDrives.isEmpty()) {
            Instant first = windowDrives.getFirst().startDate();
            if (first != null && first.isAfter(from)) {
                composeFrom = first;
                log.info("trip drives capped at {} ; compose from {}", DEFAULT_DRIVE_LIMIT, first);
            }
        }

        List<DriveEntity> neighborBefore = loadNeighborBefore(carId, composeFrom);
        Long seedPos = null;
        Long seedDrive = null;
        if (!neighborBefore.isEmpty()) {
            DriveEntity n = neighborBefore.getFirst();
            if (n.endDate() != null && n.endDate().compareTo(composeFrom) <= 0) {
                seedPos = n.endPositionId();
                seedDrive = n.id();
            }
        }

        List<ActivitySpan> spans = ActivityTimelineComposer.compose(
                windowDrives, charges, composeFrom, to, now, minPark, seedPos, seedDrive);
        LocalDate dummyDay = now.atZone(zone).toLocalDate().minusDays(1);

        Map<Long, DriveEntity> driveById = windowDrives.stream()
                .collect(Collectors.toMap(DriveEntity::id, Function.identity(), (a, b) -> a));
        Map<Long, ChargingProcessEntity> chargeById = charges.stream()
                .collect(Collectors.toMap(ChargingProcessEntity::id, Function.identity(), (a, b) -> a));
        Map<Long, ChargeEntity> sampleByProcess = chargeDao.findLatestPerProcess(
                        charges.stream().map(ChargingProcessEntity::id).toList())
                .stream()
                .collect(Collectors.toMap(ChargeEntity::chargingProcessId, Function.identity(), (a, b) -> a));

        List<Long> posIds = Stream.concat(
                        Stream.concat(
                                spans.stream().map(ActivitySpan::locationPositionId),
                                windowDrives.stream().flatMap(d -> Stream.of(d.startPositionId(), d.endPositionId()))),
                        charges.stream().map(ChargingProcessEntity::positionId))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, PositionEntity> posById = posIds.isEmpty()
                ? Map.of()
                : positionDao.findByIds(posIds).stream()
                .collect(Collectors.toMap(PositionEntity::id, Function.identity(), (a, b) -> a));

        List<Long> addressIds = Stream.concat(
                        windowDrives.stream().flatMap(d -> Stream.of(d.startAddressId(), d.endAddressId())),
                        charges.stream().map(ChargingProcessEntity::addressId))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, AddressEntity> addrById = addressIds.isEmpty()
                ? Map.of()
                : addressDao.findByIds(addressIds).stream()
                .collect(Collectors.toMap(AddressEntity::id, Function.identity(), (a, b) -> a));

        List<Long> geofenceIds = Stream.concat(
                        windowDrives.stream().flatMap(d -> Stream.of(d.startGeofenceId(), d.endGeofenceId())),
                        charges.stream().map(ChargingProcessEntity::geofenceId))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, GeofenceEntity> geoById = geofenceIds.isEmpty()
                ? Map.of()
                : geofenceDao.findByIds(geofenceIds).stream()
                .collect(Collectors.toMap(GeofenceEntity::id, Function.identity(), (a, b) -> a));

        Map<Long, List<PositionPathPoint>> byDrive = includePath
                ? loadPaths(windowDrives, from, to)
                : Map.of();

        List<TimelineItemDto> timeline = new ArrayList<>();
        for (int i = 0; i < spans.size(); i++) {
            ActivitySpan span = spans.get(i);
            boolean last = i == spans.size() - 1;
            boolean ongoingPark = last && span.kind() == TimelineKind.PARK
                    && liveWindow(to, now) && openEnded(span.end(), now);
            timeline.add(toItem(i + 1, span, driveById, chargeById, sampleByProcess,
                    posById, addrById, geoById, u, zone, ongoingPark, List.of(), dummyDay));
        }

        List<MapPointDto> points = new ArrayList<>();
        List<MapTracksDto.Feature> features = new ArrayList<>();
        int totalPts = 0;
        int chevronIdx = 0;
        int chevronBudget = MAX_CHEVRONS;
        if (includePath) {
            for (int i = 0; i < spans.size(); i++) {
                ActivitySpan span = spans.get(i);
                switch (span.kind()) {
                    case DRIVE -> {
                        DriveEntity d = span.sourceId() == null ? null : driveById.get(span.sourceId());
                        List<PositionPathPoint> path = d == null ? List.of() : byDrive.getOrDefault(d.id(), List.of());
                        if (path.size() < 2 && d != null) {
                            path = endpointsAsPath(d, posById);
                        }
                        TimelineItemDto item = toItem(i + 1, span, driveById, chargeById, sampleByProcess,
                                posById, addrById, geoById, u, zone, false, path, dummyDay);
                        totalPts += addDrivePoints(points, features, item, path, from, chevronIdx, chevronBudget);
                        if (path.size() >= 2) {
                            chevronIdx += Math.max(1, path.size() / 8);
                            chevronBudget = Math.max(0, chevronBudget - Math.max(1, path.size() / 8));
                        }
                    }
                    case CHARGE, PARK -> {
                        boolean ongoing = i == spans.size() - 1 && span.kind() == TimelineKind.PARK
                                && liveWindow(to, now) && openEnded(span.end(), now);
                        TimelineItemDto item = toItem(i + 1, span, driveById, chargeById, sampleByProcess,
                                posById, addrById, geoById, u, zone, ongoing, List.of(), dummyDay);
                        addStopPoint(points, features, item, span.kind() == TimelineKind.CHARGE ? "charge" : "park");
                    }
                }
            }
            points = MapStopMerge.mergeStops(points);
        }

        int parkCount = (int) timeline.stream().filter(t -> t.kind() == TimelineKind.PARK).count();
        int chargeCount = (int) timeline.stream().filter(t -> t.kind() == TimelineKind.CHARGE).count();
        int driveN = (int) timeline.stream().filter(t -> t.kind() == TimelineKind.DRIVE).count();
        MapTracksDto geo = new MapTracksDto("FeatureCollection", features,
                new MapTracksDto.Meta(carId, from, to, driveN, chargeCount, parkCount, totalPts));
        log.info("trip {} car={} drives={} charges={} spans={} pathPts={} {}ms",
                includePath ? "map" : "timeline",
                carId, windowDrives.size(), charges.size(), spans.size(), totalPts,
                (System.nanoTime() - started) / 1_000_000);
        return new Snapshot(timeline, points, geo);
    }

    private static List<PositionPathPoint> endpointsAsPath(DriveEntity d, Map<Long, PositionEntity> posById) {
        List<PositionPathPoint> out = new ArrayList<>(2);
        addEndpoint(out, d.id(), d.startPositionId(), posById);
        addEndpoint(out, d.id(), d.endPositionId(), posById);
        return out;
    }

    private static void addEndpoint(List<PositionPathPoint> out, Long driveId, Long positionId,
                                    Map<Long, PositionEntity> posById) {
        if (positionId == null) {
            return;
        }
        PositionEntity p = posById.get(positionId);
        if (p == null || p.longitude() == null || p.latitude() == null) {
            return;
        }
        out.add(new PositionPathPoint(driveId, p.date(), p.longitude(), p.latitude()));
    }

    private Map<Long, List<PositionPathPoint>> loadPaths(List<DriveEntity> drives, Instant from, Instant to) {
        if (drives.isEmpty()) {
            return Map.of();
        }
        List<Long> driveIds = drives.stream().map(DriveEntity::id).toList();
        Map<Long, List<PositionPathPoint>> raw = positionDao.findPathPointsByDriveIds(driveIds).stream()
                .filter(p -> p.driveId() != null)
                .collect(Collectors.groupingBy(PositionPathPoint::driveId, LinkedHashMap::new, Collectors.toList()));
        long spanSec = Math.max(1, to.getEpochSecond() - from.getEpochSecond());
        double eps = PathSimplify.epsilonMeters(spanSec);
        Map<Long, List<PositionPathPoint>> slim = new LinkedHashMap<>();
        int rawN = 0;
        int slimN = 0;
        for (Map.Entry<Long, List<PositionPathPoint>> e : raw.entrySet()) {
            rawN += e.getValue().size();
            List<PositionPathPoint> keep = PathSimplify.douglasPeucker(e.getValue(), eps);
            slimN += keep.size();
            slim.put(e.getKey(), keep);
        }
        log.info("path simplify epsilon={}m {} -> {} pts", eps, rawN, slimN);
        return slim;
    }

    private TimelineItemDto toItem(
            int seq,
            ActivitySpan span,
            Map<Long, DriveEntity> driveById,
            Map<Long, ChargingProcessEntity> chargeById,
            Map<Long, ChargeEntity> sampleByProcess,
            Map<Long, PositionEntity> posById,
            Map<Long, AddressEntity> addrById,
            Map<Long, GeofenceEntity> geoById,
            DisplayUnits units,
            ZoneId zone,
            boolean ongoingPark,
            List<PositionPathPoint> path,
            LocalDate dummyDay
    ) {
        Instant[] clock = DaySplit.clockRange(span.start(), span.end(), zone, dummyDay, AXIS_MIN_VISUAL_MIN);
        return switch (span.kind()) {
            case DRIVE -> {
                DriveEntity d = span.sourceId() == null ? null : driveById.get(span.sourceId());
                yield toDriveItem(seq, span, d, path, posById, addrById, geoById, units, zone, clock);
            }
            case CHARGE -> {
                ChargingProcessEntity c = span.sourceId() == null ? null : chargeById.get(span.sourceId());
                ChargeEntity sample = c == null ? null : sampleByProcess.get(c.id());
                yield toChargeItem(seq, span, c, sample, posById, addrById, geoById, zone, clock);
            }
            case PARK -> toParkItem(seq, span, posById, ongoingPark, zone, clock);
        };
    }

    private TimelineItemDto toDriveItem(
            int seq, ActivitySpan span, DriveEntity d, List<PositionPathPoint> path,
            Map<Long, PositionEntity> posById,
            Map<Long, AddressEntity> addrById,
            Map<Long, GeofenceEntity> geoById,
            DisplayUnits units,
            ZoneId zone,
            Instant[] clock
    ) {
        String fromPlace = d == null ? null : PlaceLabel.of(
                d.startGeofenceId() == null ? null : geoById.get(d.startGeofenceId()),
                d.startAddressId() == null ? null : addrById.get(d.startAddressId()));
        String toPlace = d == null ? null : PlaceLabel.of(
                d.endGeofenceId() == null ? null : geoById.get(d.endGeofenceId()),
                d.endAddressId() == null ? null : addrById.get(d.endAddressId()));
        String title = joinPlaces(fromPlace, toPlace);
        if (title == null) {
            title = "Drive";
        }
        Double distance = d == null ? null : UnitConverter.length(d.distance(), units);
        Integer startSoc = soc(d == null ? null : d.startPositionId(), posById);
        Integer endSoc = soc(d == null ? null : d.endPositionId(), posById);
        String detail = join(
                distance == null ? null : round1(distance) + " km",
                formatDuration(span.durationMin()),
                socRange(startSoc, endSoc));
        Double[] latLon = latLon(span.locationPositionId(), posById);
        if (latLon[0] == null && !path.isEmpty()) {
            PositionPathPoint last = path.getLast();
            latLon = new Double[]{last.latitude().doubleValue(), last.longitude().doubleValue()};
        }
        return new TimelineItemDto(
                seq, TimelineKind.DRIVE, span.sourceId(), span.start(), span.end(),
                round1(span.durationMin()), title, detail, COLOR_DRIVE,
                latLon[0], latLon[1], distance, startSoc, endSoc, null, null,
                DaySplit.dayLabel(span.start(), zone), DaySplit.dayBand(span.start(), zone),
                clock[0], clock[1], 0);
    }

    private TimelineItemDto toChargeItem(
            int seq, ActivitySpan span, ChargingProcessEntity c, ChargeEntity sample,
            Map<Long, PositionEntity> posById,
            Map<Long, AddressEntity> addrById,
            Map<Long, GeofenceEntity> geoById,
            ZoneId zone,
            Instant[] clock
    ) {
        String chargeType = TripMapService.chargeType(sample);
        boolean dc = "dc".equals(chargeType);
        String place = c == null ? null : PlaceLabel.of(
                c.geofenceId() == null ? null : geoById.get(c.geofenceId()),
                c.addressId() == null ? null : addrById.get(c.addressId()));
        String title = (dc ? "DC charge" : "AC charge") + (place == null ? "" : " · " + place);
        Integer startSoc = c == null ? null : c.startBatteryLevel();
        Integer endSoc = c == null ? null : c.endBatteryLevel();
        Double energy = c == null || c.chargeEnergyAdded() == null ? null : c.chargeEnergyAdded().doubleValue();
        String detail = join(
                energy == null ? null : round1(energy) + " kWh",
                socRange(startSoc, endSoc),
                formatDuration(span.durationMin()));
        Long posId = c != null && c.positionId() != null ? c.positionId() : span.locationPositionId();
        Double[] latLon = latLon(posId, posById);
        return new TimelineItemDto(
                seq, TimelineKind.CHARGE, span.sourceId(), span.start(), span.end(),
                round1(span.durationMin()), title, detail, dc ? COLOR_CHARGE_DC : COLOR_CHARGE,
                latLon[0], latLon[1], null, startSoc, endSoc, energy,
                chargeType == null ? null : chargeType.toUpperCase(Locale.ROOT),
                DaySplit.dayLabel(span.start(), zone), DaySplit.dayBand(span.start(), zone),
                clock[0], clock[1], 0);
    }

    private TimelineItemDto toParkItem(int seq, ActivitySpan span, Map<Long, PositionEntity> posById,
                                       boolean ongoing, ZoneId zone, Instant[] clock) {
        Double[] latLon = latLon(span.locationPositionId(), posById);
        String durationLabel = parkDurationLabel(span.durationMin(), ongoing);
        return new TimelineItemDto(
                seq, TimelineKind.PARK, null, span.start(), span.end(),
                ongoing ? null : round1(span.durationMin()), "Parked", durationLabel, COLOR_PARK,
                latLon[0], latLon[1], null, null, null, null, null,
                DaySplit.dayLabel(span.start(), zone), DaySplit.dayBand(span.start(), zone),
                clock[0], clock[1], 0);
    }

    private int addDrivePoints(
            List<MapPointDto> points,
            List<MapTracksDto.Feature> features,
            TimelineItemDto item,
            List<PositionPathPoint> path,
            Instant windowFrom,
            int chevronStart,
            int chevronBudget
    ) {
        List<List<BigDecimal>> coords = new ArrayList<>();
        Double heading = null;
        int arrowEvery = Math.max(4, path.size() / 8);
        int arrowsLeft = chevronBudget;
        int localArrow = 0;
        for (int i = 0; i < path.size(); i++) {
            PositionPathPoint p = path.get(i);
            PositionPathPoint next = i + 1 < path.size() ? path.get(i + 1) : null;
            Double h = next == null
                    ? heading
                    : PathGeometry.bearingDeg(p.longitude(), p.latitude(), next.longitude(), next.latitude());
            if (h != null) {
                heading = h;
            }
            double elapsed = p.date() == null || windowFrom == null
                    ? 0
                    : Duration.between(windowFrom, p.date()).toMillis() / 60_000.0;
            points.add(new MapPointDto(
                    p.date(),
                    p.latitude().doubleValue(),
                    p.longitude().doubleValue(),
                    "drive",
                    item.id(),
                    item.seq(),
                    heading,
                    round1(elapsed),
                    COLOR_DRIVE,
                    item.title(),
                    null,
                    null,
                    null));
            coords.add(List.of(p.longitude(), p.latitude()));
            if (h != null && arrowsLeft > 0 && i > 0 && i < path.size() - 1 && i % arrowEvery == 0) {
                List<List<BigDecimal>> chevron = PathGeometry.chevron(p.longitude(), p.latitude(), h, 18.0);
                if (chevron.size() == 3) {
                    Map<String, Object> props = new HashMap<>();
                    props.put("seq", item.seq());
                    props.put("driveId", item.id());
                    props.put("color", COLOR_DRIVE);
                    features.add(MapTracksDto.arrowLine(chevronStart + localArrow, chevron, props));
                    localArrow++;
                    arrowsLeft--;
                }
            }
        }
        if (coords.size() >= 2) {
            Map<String, Object> props = new HashMap<>();
            props.put("seq", item.seq());
            props.put("startDate", iso(item.start()));
            props.put("endDate", iso(item.end()));
            props.put("distance", item.distanceKm());
            props.put("durationMin", item.durationMin());
            props.put("title", item.title());
            props.put("detail", item.detail());
            props.put("color", COLOR_DRIVE);
            features.add(MapTracksDto.driveLine(item.id() == null ? item.seq() : item.id(), coords, props));
        }
        return coords.size();
    }

    private void addStopPoint(
            List<MapPointDto> points,
            List<MapTracksDto.Feature> features,
            TimelineItemDto item,
            String kind
    ) {
        if (item.latitude() == null || item.longitude() == null) {
            return;
        }
        boolean ongoingPark = "park".equals(kind) && item.durationMin() == null
                && "-".equals(item.detail());
        String mapLabel = "charge".equals(kind)
                ? MapStopMerge.chargeLabel(1, item.chargeType(), item.energyKwh(), item.durationMin())
                : MapStopMerge.parkLabel(1, item.durationMin(), ongoingPark);
        points.add(new MapPointDto(
                item.start(), item.latitude(), item.longitude(),
                kind, item.id(), item.seq(), null, null, item.color(), item.title(),
                mapLabel, item.durationMin(), item.energyKwh()));
        Map<String, Object> props = new HashMap<>();
        props.put("seq", item.seq());
        props.put("startDate", iso(item.start()));
        props.put("endDate", iso(item.end()));
        props.put("durationMin", item.durationMin());
        props.put("durationLabel", item.detail());
        props.put("title", item.title());
        props.put("detail", item.detail());
        props.put("color", item.color());
        BigDecimal lon = BigDecimal.valueOf(item.longitude());
        BigDecimal lat = BigDecimal.valueOf(item.latitude());
        if ("charge".equals(kind)) {
            props.put("chargeEnergyAdded", item.energyKwh());
            props.put("chargeType", item.chargeType());
            features.add(MapTracksDto.chargePoint(item.id() == null ? item.seq() : item.id(), lon, lat, props));
        } else {
            features.add(MapTracksDto.parkPoint(item.seq(), lon, lat, props));
        }
    }

    private List<DriveEntity> loadOverlappingDrives(long carId, Instant from, Instant to) {
        DriveSearchCondition cond = DriveSearchCondition.builder()
                .carId(carId)
                .overlapping(from, to)
                .build();
        List<DriveEntity> rows = driveDao.findByIdsOrdered(driveDao.findIds(cond, DEFAULT_DRIVE_LIMIT, 0));
        return rows.stream()
                .filter(d -> d.startDate() != null)
                .sorted(Comparator.comparing(DriveEntity::startDate))
                .toList();
    }

    private List<DriveEntity> loadNeighborBefore(long carId, Instant from) {
        DriveSearchCondition cond = DriveSearchCondition.builder()
                .carId(carId)
                .startDateTo(from)
                .build();
        return driveDao.findByIdsOrdered(driveDao.findIds(cond, 1, 0));
    }

    private List<ChargingProcessEntity> loadOverlappingCharges(long carId, Instant from, Instant to) {
        ChargingProcessSearchCondition cond = ChargingProcessSearchCondition.builder()
                .carId(carId)
                .overlapping(from, to)
                .build();
        List<ChargingProcessEntity> rows =
                chargingProcessDao.findByIdsOrdered(chargingProcessDao.findIds(cond, DEFAULT_CHARGE_LIMIT, 0));
        return rows.stream()
                .filter(c -> c.startDate() != null)
                .sorted(Comparator.comparing(ChargingProcessEntity::startDate))
                .toList();
    }

    private CarEntity requireCar(long carId) {
        return carDao.findById(carId).orElseThrow(() -> new NotFoundException("Car not found: " + carId));
    }

    private Instant parseFlexibleInstant(String raw) {
        return coerceInstant(raw);
    }

    /**
     * Grafana may send ISO-8601, unix seconds/millis, or an offset whose {@code +}
     * was decoded as a space in the query string.
     */
    static Instant coerceInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if (s.matches(".*T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)? \\d{2}:\\d{2}(:\\d{2})?$")) {
            s = s.replaceFirst(" (\\d{2}:\\d{2}(?::\\d{2})?)$", "+$1");
        }
        if (s.chars().skip(s.startsWith("-") ? 1 : 0).allMatch(Character::isDigit)) {
            try {
                long n = Long.parseLong(s);
                return Math.abs(n) < 1_000_000_000_000L
                        ? Instant.ofEpochSecond(n)
                        : Instant.ofEpochMilli(n);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        try {
            return Instant.parse(s);
        } catch (RuntimeException e) {
            try {
                return java.time.OffsetDateTime.parse(s).toInstant();
            } catch (RuntimeException e2) {
                return null;
            }
        }
    }

    static int focusKindCode(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("${")) {
            return 0;
        }
        String s = raw.trim();
        String u = s.toUpperCase(Locale.ROOT);
        if (u.startsWith("PARK") || u.equals("1")) {
            return DayGridCellDto.PARK;
        }
        if (u.startsWith("DRIVE") || u.equals("2")) {
            return DayGridCellDto.DRIVE;
        }
        if (u.startsWith("CHARGE") || u.equals("3")) {
            return DayGridCellDto.CHARGE;
        }
        try {
            long n = Long.parseLong(s);
            if (n >= DayGridCellDto.HOVER_TAIL) {
                int base = (int) (n / DayGridCellDto.HOVER_TAIL);
                if (base >= 11 && base <= 13) {
                    return base - 10;
                }
                return base >= 1 && base <= 3 ? base : 0;
            }
            if (n >= 11 && n <= 13) {
                return (int) n - 10;
            }
            return n >= 1 && n <= 3 ? (int) n : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int kindCode(TimelineKind kind) {
        if (kind == TimelineKind.DRIVE) {
            return DayGridCellDto.DRIVE;
        }
        if (kind == TimelineKind.CHARGE) {
            return DayGridCellDto.CHARGE;
        }
        return DayGridCellDto.PARK;
    }

    private static boolean overlapsWindow(TimelineItemDto item, Instant from, Instant to) {
        if (item == null || item.start() == null || item.end() == null) {
            return false;
        }
        return item.start().isBefore(to) && item.end().isAfter(from);
    }

    static Set<String> parseKinds(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("${")) {
            return Set.of("drive", "charge", "park");
        }
        Set<String> out = new java.util.HashSet<>();
        for (String part : raw.split(",")) {
            String k = part.trim().toLowerCase(Locale.ROOT);
            if (k.equals("drive") || k.equals("charge") || k.equals("park")) {
                out.add(k);
            }
        }
        return out.isEmpty() ? Set.of("drive", "charge", "park") : Set.copyOf(out);
    }

    private static Integer soc(Long positionId, Map<Long, PositionEntity> posById) {
        if (positionId == null) {
            return null;
        }
        PositionEntity p = posById.get(positionId);
        return p == null ? null : p.batteryLevel();
    }

    private static Double[] latLon(Long positionId, Map<Long, PositionEntity> posById) {
        if (positionId == null) {
            return new Double[]{null, null};
        }
        PositionEntity p = posById.get(positionId);
        if (p == null || p.latitude() == null || p.longitude() == null) {
            return new Double[]{null, null};
        }
        return new Double[]{p.latitude().doubleValue(), p.longitude().doubleValue()};
    }

    private static String joinPlaces(String from, String to) {
        if (from == null && to == null) {
            return null;
        }
        if (from == null) {
            return "→ " + to;
        }
        if (to == null || to.equals(from)) {
            return from;
        }
        return from + " → " + to;
    }

    private static String socRange(Integer start, Integer end) {
        if (start == null && end == null) {
            return null;
        }
        if (start == null) {
            return end + "%";
        }
        if (end == null) {
            return start + "%";
        }
        return start + "% → " + end + "%";
    }

    static boolean liveWindow(Instant windowTo, Instant now) {
        return windowTo != null && now != null && !windowTo.isBefore(now.minusSeconds(120));
    }

    static boolean openEnded(Instant end, Instant now) {
        return end != null && now != null && !end.isBefore(now.minusSeconds(90));
    }

    static String parkDurationLabel(double minutes, boolean ongoing) {
        return ongoing ? "-" : formatDuration(minutes);
    }

    private static String formatDuration(double minutes) {
        long m = Math.round(minutes);
        if (m < 60) {
            return m + " min";
        }
        long h = m / 60;
        long rem = m % 60;
        return rem == 0 ? h + "h" : h + "h " + rem + "m";
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append(p);
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private static Double round1(Double v) {
        if (v == null) {
            return null;
        }
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static String iso(Instant t) {
        return t == null ? null : t.toString();
    }

    private record Snapshot(
            List<TimelineItemDto> timeline,
            List<MapPointDto> points,
            MapTracksDto geoJson
    ) {}
}
