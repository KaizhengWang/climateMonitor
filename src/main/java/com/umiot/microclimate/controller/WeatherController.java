package com.umiot.microclimate.controller;

import com.umiot.microclimate.history.entity.WeatherHistoryRecord;
import com.umiot.microclimate.history.repository.WeatherHistoryDao;
import com.umiot.microclimate.history.service.WeatherHistoryScraperService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherHistoryScraperService scraperService;
    private final WeatherHistoryDao dao;
    private final com.umiot.microclimate.service.NowWeatherService nowWeatherService;

    public WeatherController(WeatherHistoryScraperService scraperService, WeatherHistoryDao dao,
                             com.umiot.microclimate.service.NowWeatherService nowWeatherService) {
        this.scraperService = scraperService;
        this.dao = dao;
        this.nowWeatherService = nowWeatherService;
    }

    @GetMapping("/import")
    @PostMapping("/import")
    public Map<String, Object> startImport() {
        return scraperService.startImport();
    }

    @GetMapping("/import/range")
    public Map<String, Object> startImportRange(
            @RequestParam String start, @RequestParam String end) {
        return scraperService.startImportRange(start, end);
    }

    @GetMapping("/import/{year}/{month}")
    public Map<String, Object> startImportMonth(
            @PathVariable int year, @PathVariable int month) {
        return scraperService.startImportMonth(year, month);
    }

    @GetMapping("/import/progress")
    public Map<String, Object> importProgress() {
        return scraperService.getProgress();
    }

    @GetMapping("/status")
    public Map<String, Object> importStatus() {
        return scraperService.getImportStatus();
    }

    @GetMapping("/clear")
    @DeleteMapping("/clear")
    public String clearData() {
        scraperService.clearAllData();
        return "All history data cleared.";
    }

    @GetMapping("/station/{stationId}")
    public List<WeatherHistoryRecord> getStationData(@PathVariable String stationId) {
        return dao.findByStation(stationId);
    }

    @GetMapping("/history/{stationId}")
    public List<WeatherHistoryRecord> history(@PathVariable String stationId) {
        return dao.findByStation(stationId);
    }

    @GetMapping("/query")
    public List<WeatherHistoryRecord> queryByTimeRange(
            @RequestParam String stationId,
            @RequestParam String start,
            @RequestParam String end) {
        LocalDateTime startTime = LocalDateTime.parse(start);
        LocalDateTime endTime = LocalDateTime.parse(end);
        return dao.findByStationAndTimeRange(stationId, startTime, endTime);
    }

    @GetMapping("/recent24h/{stationId}")
    public List<WeatherHistoryRecord> recent24h(@PathVariable String stationId) {
        return scraperService.fetchRecent24h(stationId);
    }

    @GetMapping("/latest/{stationId}")
    public WeatherHistoryRecord latest(@PathVariable String stationId) {
        List<WeatherHistoryRecord> all = dao.findByStation(stationId);
        return all.isEmpty() ? null : all.get(all.size() - 1);
    }

    @GetMapping("/now/{stationId}")
    public com.umiot.microclimate.dto.NowWeatherDTO now(@PathVariable String stationId) {
        return nowWeatherService.fetchNow(stationId);
    }

    @GetMapping("/now")
    public Map<String, com.umiot.microclimate.dto.NowWeatherDTO> nowAll() {
        return nowWeatherService.fetchAllNow();
    }

    @GetMapping("/extremes")
    public Map<String, Object> extremes(
            @RequestParam(defaultValue = "um") String stationId,
            @RequestParam(defaultValue = "2026") int year) {

        List<WeatherHistoryRecord> all = dao.findByStationAndYear(stationId, year);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stationId", stationId);
        result.put("year", year);
        result.put("totalRecords", all.size());

        // group by day
        Map<LocalDate, List<WeatherHistoryRecord>> byDay = new LinkedHashMap<>();
        for (WeatherHistoryRecord r : all) {
            LocalDate day = r.getRecordTime().toLocalDate();
            byDay.computeIfAbsent(day, k -> new ArrayList<>()).add(r);
        }

        // daily aggregates
        List<Map<String, Object>> dailyStats = new ArrayList<>();
        for (Map.Entry<LocalDate, List<WeatherHistoryRecord>> e : byDay.entrySet()) {
            List<WeatherHistoryRecord> recs = e.getValue();
            double minTemp = recs.stream().mapToDouble(r -> r.getTemperature() == null ? Double.NaN : r.getTemperature()).filter(v -> !Double.isNaN(v)).min().orElse(Double.NaN);
            double maxTemp = recs.stream().mapToDouble(r -> r.getTemperature() == null ? Double.NaN : r.getTemperature()).filter(v -> !Double.isNaN(v)).max().orElse(Double.NaN);
            double maxGust = recs.stream().mapToDouble(r -> r.getWindGust() == null ? 0 : r.getWindGust()).max().orElse(0);
            double totalRain = recs.stream().mapToDouble(r -> r.getRain1hour() == null ? 0 : r.getRain1hour()).sum();

            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("date", e.getKey().toString());
            stat.put("minTemp", Double.isNaN(minTemp) ? null : minTemp);
            stat.put("maxTemp", Double.isNaN(maxTemp) ? null : maxTemp);
            stat.put("maxGust", maxGust);
            stat.put("totalRain", Math.round(totalRain * 10.0) / 10.0);
            stat.put("recordCount", recs.size());
            stat.put("records", recs); // full 30-min records for the day
            dailyStats.add(stat);
        }

        // top 5 coldest (by minTemp, exclude null)
        List<Map<String, Object>> coldest = dailyStats.stream()
            .filter(d -> d.get("minTemp") != null)
            .sorted(Comparator.comparing(d -> (Double) d.get("minTemp")))
            .limit(5).toList();

        // top 5 hottest (by maxTemp)
        List<Map<String, Object>> hottest = dailyStats.stream()
            .filter(d -> d.get("maxTemp") != null)
            .sorted(Comparator.<Map<String, Object>, Double>comparing(d -> (Double) d.get("maxTemp")).reversed())
            .limit(5).toList();

        // top 5 windiest (by maxGust)
        List<Map<String, Object>> windiest = dailyStats.stream()
            .sorted(Comparator.<Map<String, Object>, Double>comparing(d -> (Double) d.get("maxGust")).reversed())
            .limit(5).toList();

        // top 5 rainiest (by totalRain)
        List<Map<String, Object>> rainiest = dailyStats.stream()
            .sorted(Comparator.<Map<String, Object>, Double>comparing(d -> (Double) d.get("totalRain")).reversed())
            .limit(5).toList();

        // strip full records from response for list views (keep only for frontend)
        result.put("coldest", stripRecords(coldest));
        result.put("hottest", stripRecords(hottest));
        result.put("windiest", stripRecords(windiest));
        result.put("rainiest", stripRecords(rainiest));

        // detailed records for charts: keep the top days with full 30-min data
        result.put("coldestDetail", coldest);
        result.put("hottestDetail", hottest);
        result.put("windiestDetail", windiest);
        result.put("rainiestDetail", rainiest);

        return result;
    }

    private List<Map<String, Object>> stripRecords(List<Map<String, Object>> days) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> d : days) {
            Map<String, Object> m = new LinkedHashMap<>(d);
            m.remove("records");
            out.add(m);
        }
        return out;
    }

    @GetMapping("/stations")
    public List<Map<String, String>> stationList() {
        List<Map<String, String>> list = new java.util.ArrayList<>();
        for (Map.Entry<String, String> e : WeatherHistoryScraperService.STATIONS.entrySet()) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("id", e.getKey());
            m.put("name", e.getValue());
            list.add(m);
        }
        return list;
    }
}
