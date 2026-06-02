package com.umiot.microclimate.controller;

import com.umiot.microclimate.history.entity.WeatherHistoryRecord;
import com.umiot.microclimate.history.repository.WeatherHistoryDao;
import com.umiot.microclimate.history.service.WeatherHistoryScraperService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherHistoryScraperService scraperService;
    private final WeatherHistoryDao dao;

    public WeatherController(WeatherHistoryScraperService scraperService, WeatherHistoryDao dao) {
        this.scraperService = scraperService;
        this.dao = dao;
    }

    @GetMapping("/import")
    @PostMapping("/import")
    public Map<String, Object> startImport() {
        return scraperService.startImport();
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

    @GetMapping("/latest/{stationId}")
    public WeatherHistoryRecord latest(@PathVariable String stationId) {
        List<WeatherHistoryRecord> all = dao.findByStation(stationId);
        return all.isEmpty() ? null : all.get(all.size() - 1);
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
