package com.umiot.microclimate.service;

import com.umiot.microclimate.dto.NowWeatherDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
