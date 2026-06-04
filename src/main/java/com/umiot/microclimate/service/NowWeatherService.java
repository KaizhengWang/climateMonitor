package com.umiot.microclimate.service;

import com.umiot.microclimate.dto.NowWeatherDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class NowWeatherService {

    private static final String BASE = "https://std.puiching.edu.mo/~pcmsams";
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/117.0.0.0 Safari/537.36";
    private static final String REFERER = BASE + "/index.php/nowweather/nowweather/";

    private static final Map<String, ElementRequest> ELEMENTS = new LinkedHashMap<>();

    public NowWeatherService() {
        ELEMENTS.put("Temperature", new ElementRequest("smg", "Temperature", "(℃)"));
        ELEMENTS.put("Humidity", new ElementRequest("smg", "Humidity", "(%)"));
        ELEMENTS.put("WindSpeed", new ElementRequest("smg", "WindSpeed", "(km/h)"));
        ELEMENTS.put("rainHour", new ElementRequest("smg", "rainHour", "(mm)"));
    }

    public NowWeatherDTO fetchNow(String rawStationId) {
        final String stationId = rawStationId.toLowerCase().trim();
        ExecutorService pool = Executors.newFixedThreadPool(5);
        try {
            Future<Double> tempF = pool.submit(() -> fetchValue(stationId, "Temperature"));
            Future<Double> humF = pool.submit(() -> fetchValue(stationId, "Humidity"));
            Future<Double> windF = pool.submit(() -> fetchValue(stationId, "WindSpeed"));
            Future<Double> rainF = pool.submit(() -> fetchValue(stationId, "rainHour"));
            Future<String> dirF = pool.submit(() -> fetchWindDirection(stationId));

            NowWeatherDTO dto = new NowWeatherDTO();
            dto.setStationId(stationId);
            dto.setTemperature(getOrNull(tempF));
            dto.setHumidity(getOrNull(humF));
            dto.setWindSpeed10min(getOrNull(windF));
            dto.setWindDirection(getOrNull(dirF));
            dto.setRain1hour(getOrNull(rainF));
            return dto;
        } finally {
            pool.shutdown();
        }
    }

    public Map<String, NowWeatherDTO> fetchAllNow() {
        Map<String, NowWeatherDTO> result = new LinkedHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(5);
        try {
            Future<Map<String, Double>> tempF = pool.submit(() -> fetchAllValues("Temperature"));
            Future<Map<String, Double>> humF = pool.submit(() -> fetchAllValues("Humidity"));
            Future<Map<String, Double>> windF = pool.submit(() -> fetchAllValues("WindSpeed"));
            Future<Map<String, Double>> rainF = pool.submit(() -> fetchAllValues("rainHour"));
            Future<Map<String, String>> dirF = pool.submit(this::fetchAllWindDirections);

            Map<String, Double> temps = getOrNull(tempF);
            Map<String, Double> hums = getOrNull(humF);
            Map<String, Double> winds = getOrNull(windF);
            Map<String, Double> rains = getOrNull(rainF);
            Map<String, String> dirs = getOrNull(dirF);

            if (temps == null) temps = Collections.emptyMap();
            if (hums == null) hums = Collections.emptyMap();
            if (winds == null) winds = Collections.emptyMap();
            if (rains == null) rains = Collections.emptyMap();
            if (dirs == null) dirs = Collections.emptyMap();

            java.util.Set<String> allIds = new java.util.LinkedHashSet<>();
            allIds.addAll(temps.keySet());
            allIds.addAll(hums.keySet());
            allIds.addAll(winds.keySet());

            for (String id : allIds) {
                NowWeatherDTO dto = new NowWeatherDTO();
                dto.setStationId(id.toLowerCase());
                dto.setTemperature(temps.get(id));
                dto.setHumidity(hums.get(id));
                dto.setWindSpeed10min(winds.get(id));
                dto.setRain1hour(rains.get(id));
                dto.setWindDirection(dirs.get(id));
                result.put(id.toLowerCase(), dto);
            }
            return result;
        } finally {
            pool.shutdown();
        }
    }

    private Map<String, Double> fetchAllValues(String element) {
        ElementRequest req = ELEMENTS.get(element);
        if (req == null) return Collections.emptyMap();
        try {
            String url = String.format("%s/pages/nowWeather/searchNowWeather/%s.php?element=%s&name=&unit=%s",
                    BASE, req.provider, req.element,
                    URLEncoder.encode(req.unit, StandardCharsets.UTF_8));
            Document doc = Jsoup.connect(url)
                    .userAgent(UA)
                    .referrer(REFERER)
                    .timeout(15000)
                    .get();
            return parseAllNumeric(doc);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private Map<String, String> fetchAllWindDirections() {
        try {
            String url = BASE + "/pages/nowWeather/searchNowWeather/windDir.php";
            Document doc = Jsoup.connect(url)
                    .userAgent(UA)
                    .referrer(REFERER)
                    .timeout(15000)
                    .get();
            return parseAllText(doc);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private Map<String, Double> parseAllNumeric(Document doc) {
        Map<String, Double> result = new LinkedHashMap<>();
        Elements lis = doc.select("li span.value");
        for (Element span : lis) {
            Element li = span.parent();
            String id = li.id();
            if (id == null || id.isEmpty()) continue;
            String text = span.text().trim();
            if (text.isEmpty() || "---".equals(text)) continue;
            try {
                result.put(id, Double.parseDouble(text));
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    private Map<String, String> parseAllText(Document doc) {
        Map<String, String> result = new LinkedHashMap<>();
        Elements lis = doc.select("li span.value");
        for (Element span : lis) {
            Element li = span.parent();
            String id = li.id();
            if (id == null || id.isEmpty()) continue;
            String text = span.text().trim();
            if (!text.isEmpty() && !"---".equals(text)) {
                result.put(id, text);
            }
        }
        return result;
    }

    private Double fetchValue(String stationId, String element) {
        ElementRequest req = ELEMENTS.get(element);
        if (req == null) return null;
        try {
            String url = String.format("%s/pages/nowWeather/searchNowWeather/%s.php?element=%s&name=%s&unit=%s",
                    BASE, req.provider, req.element, stationId,
                    URLEncoder.encode(req.unit, StandardCharsets.UTF_8));
            Document doc = Jsoup.connect(url)
                    .userAgent(UA)
                    .referrer(REFERER)
                    .timeout(10000)
                    .get();
            return parseNumeric(doc, stationId);
        } catch (Exception e) {
            return null;
        }
    }

    private String fetchWindDirection(String stationId) {
        try {
            String url = BASE + "/pages/nowWeather/searchNowWeather/windDir.php";
            Document doc = Jsoup.connect(url)
                    .userAgent(UA)
                    .referrer(REFERER)
                    .timeout(10000)
                    .get();
            return parseText(doc, stationId);
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseNumeric(Document doc, String stationId) {
        Elements li = doc.select("li#" + stationId.toUpperCase() + " span.value");
        if (li.isEmpty()) return null;
        String text = li.first().text().trim();
        if (text.isEmpty() || "---".equals(text)) return null;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String parseText(Document doc, String stationId) {
        Elements li = doc.select("li#" + stationId.toUpperCase() + " span.value");
        if (li.isEmpty()) return null;
        String text = li.first().text().trim();
        return text.isEmpty() || "---".equals(text) ? null : text;
    }

    private <T> T getOrNull(Future<T> f) {
        try {
            return f.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            return null;
        }
    }

    private record ElementRequest(String provider, String element, String unit) {}
}
