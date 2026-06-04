package com.umiot.microclimate.history.service;

import com.umiot.microclimate.history.entity.WeatherHistoryRecord;
import com.umiot.microclimate.history.repository.WeatherHistoryDao;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WeatherHistoryScraperService {

    private final AtomicBoolean importing = new AtomicBoolean(false);
    private final AtomicInteger progress = new AtomicInteger(0);
    private volatile int totalTasks = 0;
    private volatile String currentTask = "";
    private volatile String lastError = null;

    private static final String BASE_URL =
        "https://std.puiching.edu.mo/~pcmsams/pages/weatherData/weatherData/single.php";
    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/117.0.0.0 Safari/537.36";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static final LinkedHashMap<String, String> STATIONS = new LinkedHashMap<>() {{
        put("tg", "大潭山");
        put("dp", "紀念孫中山市政公園");
        put("pe", "外港碼頭");
        put("fm", "大炮台山");
        put("mm", "海事博物館");
        put("em", "澳門污水處理廠");
        put("pn", "友誼大橋北峰");
        put("ps", "友誼大橋南峰");
        put("pg", "嘉樂庇總督大橋");
        put("pv", "西灣大橋");
        put("mn", "澳門大橋北");
        put("ms", "澳門大橋南");
        put("ja", "東亞運站");
        put("kv", "九澳");
        put("dc", "路環分站");
        put("um", "澳門大學");
    }};

    private final WeatherHistoryDao dao;

    public WeatherHistoryScraperService(WeatherHistoryDao dao) {
        this.dao = dao;
        dao.initTable();
    }

    public Map<String, Object> startImport() {
        if (importing.get()) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("status", "already_running");
            resp.put("progress", progress.get() + "/" + totalTasks);
            resp.put("currentTask", currentTask);
            return resp;
        }

        totalTasks = STATIONS.size() * 5; // 16 stations × 5 months (Jan–May)
        progress.set(0);
        lastError = null;
        importing.set(true);

        new Thread(this::doImport, "weather-import").start();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "started");
        resp.put("totalTasks", totalTasks);
        return resp;
    }

    private void doImport() {
        LocalDate[] monthStarts = {
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 5, 1)
        };
        LocalDate[] monthEnds = {
            LocalDate.of(2026, 1, 31),
            LocalDate.of(2026, 2, 28),
            LocalDate.of(2026, 3, 31),
            LocalDate.of(2026, 4, 30),
            LocalDate.of(2026, 5, 31)
        };

        try {
            for (Map.Entry<String, String> entry : STATIONS.entrySet()) {
                String stationId = entry.getKey();
                String stationName = entry.getValue();

                for (int m = 0; m < monthStarts.length; m++) {
                    String startDate = monthStarts[m].toString();
                    String endDate = monthEnds[m].toString();

                    fetchAndSave(stationId, stationName, startDate, endDate);
                    Thread.sleep(2000);
                }
            }
        } catch (Exception e) {
            lastError = "Import failed: " + e.getMessage();
        } finally {
            importing.set(false);
        }
    }

    public Map<String, Object> startImportRange(String startDate, String endDate) {
        if (importing.get()) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("status", "already_running");
            return resp;
        }
        totalTasks = STATIONS.size();
        progress.set(0);
        lastError = null;
        importing.set(true);

        new Thread(() -> doImportRange(LocalDate.parse(startDate), LocalDate.parse(endDate)), "weather-import").start();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "started");
        resp.put("totalTasks", totalTasks);
        resp.put("range", startDate + " ~ " + endDate);
        return resp;
    }

    private void doImportRange(LocalDate start, LocalDate end) {
        try {
            for (Map.Entry<String, String> entry : STATIONS.entrySet()) {
                fetchAndSave(entry.getKey(), entry.getValue(), start.toString(), end.toString());
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            lastError = "Import failed: " + e.getMessage();
        } finally {
            importing.set(false);
        }
    }

    public Map<String, Object> getProgress() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("running", importing.get());
        p.put("progress", progress.get() + "/" + totalTasks);
        p.put("currentTask", currentTask);
        if (lastError != null) p.put("lastError", lastError);
        return p;
    }

    private void fetchAndSave(String stationId, String stationName, String startDate, String endDate)
            throws InterruptedException {
        currentTask = String.format("%s (%s) %s~%s", stationName, stationId, startDate, endDate);

        // try full month up to 3 times
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                List<WeatherHistoryRecord> records = fetchStationData(stationId, startDate, endDate);
                if (!records.isEmpty()) {
                    dao.batchInsert(records);
                }
                System.out.printf("[OK] %s: %d records%n", currentTask, records.size());
                progress.incrementAndGet();
                return;
            } catch (IOException e) {
                System.err.printf("[RETRY %d/3] %s: %s%n", attempt, currentTask, e.getMessage());
                if (attempt < 3) Thread.sleep(3000);
            }
        }

        // full month failed — try weekly
        System.out.printf("[FALLBACK] %s: splitting into weeks%n", currentTask);
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        int monthTotal = 0;
        for (LocalDate weekStart = start; weekStart.isBefore(end); weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(end)) weekEnd = end;
            String ws = weekStart.toString();
            String we = weekEnd.toString();

            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    List<WeatherHistoryRecord> records = fetchStationData(stationId, ws, we);
                    if (!records.isEmpty()) {
                        dao.batchInsert(records);
                        monthTotal += records.size();
                    }
                    Thread.sleep(800);
                    break;
                } catch (IOException e) {
                    if (attempt == 2) {
                        lastError = String.format("%s week %s~%s: %s", currentTask, ws, we, e.getMessage());
                        System.err.println("[ERROR] " + lastError);
                    }
                    Thread.sleep(2000);
                }
            }
        }
        System.out.printf("[OK] %s: %d records (weekly fallback)%n", currentTask, monthTotal);
        progress.incrementAndGet();
    }

    List<WeatherHistoryRecord> fetchStationData(String stationId, String startDate, String endDate)
            throws IOException {
        String url = BASE_URL
            + "?startDate=" + startDate
            + "&endDate=" + endDate
            + "&interval=30"
            + "&stationId=" + stationId
            + "&draw=0";

        Document doc = Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .referrer("https://std.puiching.edu.mo/~pcmsams/index.php/weatherdata/weatherdata/")
            .timeout(120000)
            .get();

        // find the record table
        Element recordTable = doc.selectFirst("div.record table");
        if (recordTable == null) return Collections.emptyList();

        Elements rows = recordTable.select("tr");
        if (rows.size() <= 1) return Collections.emptyList();

        // build column index map from header row (th elements)
        Map<String, Integer> colIdx = new LinkedHashMap<>();
        Elements headers = rows.get(0).select("th");
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i).text().trim();
            colIdx.put(h, i);
        }

        List<WeatherHistoryRecord> records = new ArrayList<>();
        Set<String> seenTimes = new HashSet<>();

        for (int i = 1; i < rows.size(); i++) {
            Elements cells = rows.get(i).select("td");
            if (cells.size() < colIdx.size()) continue;

            String dtStr = getCell(cells, colIdx, "日期時間");
            if (dtStr.isEmpty()) continue;
            if (!seenTimes.add(dtStr)) continue;

            WeatherHistoryRecord r = new WeatherHistoryRecord();
            r.setStationId(stationId);
            r.setRecordTime(LocalDateTime.parse(dtStr, DT_FMT));
            r.setTemperature(parseCell(cells, colIdx, "氣温(℃)"));
            r.setDailyHigh(parseCell(cells, colIdx, "日高温(℃)"));
            r.setDailyLow(parseCell(cells, colIdx, "日低温(℃)"));
            r.setFeelsLike(parseCell(cells, colIdx, "體感温度(℃)"));
            r.setHumidity(parseCell(cells, colIdx, "相對濕度(%)"));
            r.setDewPoint(parseCell(cells, colIdx, "露點(℃)"));
            r.setRain1min(parseCell(cells, colIdx, "一分鐘雨量(mm)"));
            r.setRain1hour(parseCell(cells, colIdx, "一小時雨量(mm)"));
            r.setRain2hour(parseCell(cells, colIdx, "兩小時雨量(mm)"));
            r.setRainDaily(parseCell(cells, colIdx, "當日總雨量(mm)"));
            r.setWindSpeed10min(parseCell(cells, colIdx, "十分鐘風速(km/h)"));
            r.setWindSpeed60min(parseCell(cells, colIdx, "六十分鐘風速(km/h)"));
            r.setWindGust(parseCell(cells, colIdx, "陣風(km/h)"));
            r.setWindDirection(parseCellText(cells, colIdx, "風向"));
            r.setWindDirectionDegrees(parseCell(cells, colIdx, "風向度數(°)"));

            records.add(r);
        }
        return records;
    }

    private String getCell(Elements cells, Map<String, Integer> colIdx, String colName) {
        Integer idx = colIdx.get(colName);
        return idx != null && idx < cells.size() ? cells.get(idx).text().trim() : "";
    }

    private Double parseCell(Elements cells, Map<String, Integer> colIdx, String colName) {
        String text = getCell(cells, colIdx, colName);
        if (text.isEmpty() || "---".equals(text)) return null;
        try { return Double.parseDouble(text); } catch (NumberFormatException e) { return null; }
    }

    private String parseCellText(Elements cells, Map<String, Integer> colIdx, String colName) {
        String text = getCell(cells, colIdx, colName);
        return text.isEmpty() || "---".equals(text) ? null : text;
    }

    public Map<String, Object> getImportStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("totalRecords", dao.countAll());
        Map<String, Long> stationCounts = new LinkedHashMap<>();
        for (String stationId : STATIONS.keySet()) {
            long count = dao.countByStation(stationId);
            if (count > 0) {
                stationCounts.put(stationId, count);
            }
        }
        status.put("stationCounts", stationCounts);
        return status;
    }

    public void clearAllData() {
        dao.deleteAll();
    }
}
