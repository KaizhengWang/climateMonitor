package com.umiot.microclimate.service;

import com.umiot.microclimate.entity.SelfWeatherRecord;
import com.umiot.microclimate.repository.SelfWeatherRecordRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class StationCompareService {

    private static final String BASE = "https://std.puiching.edu.mo/~pcmsams";
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/117.0.0.0 Safari/537.36";
    private static final String REFERER = BASE + "/index.php/nowweather/nowweather/";

    private static final Map<String, String> STATION_NAMES = new LinkedHashMap<>();
    static {
        STATION_NAMES.put("dp", "紀念孫中山市政公園");
        STATION_NAMES.put("pe", "外港");
        STATION_NAMES.put("ja", "東亞運大馬路");
        STATION_NAMES.put("em", "黑沙環");
        STATION_NAMES.put("fm", "大炮台");
        STATION_NAMES.put("mm", "媽閣");
        STATION_NAMES.put("dc", "路環市區");
        STATION_NAMES.put("um", "澳門大學");
        STATION_NAMES.put("tg", "大潭山");
        STATION_NAMES.put("kv", "九澳");
    }

    private static final String SELF_STATION_ID = "self_a";
    private static final String SELF_STATION_NAME = "自研站A";
    private static final String SELF_DEVICE_ID = "34:85:18:8F:5D:F4";

    private final SelfWeatherRecordRepository selfRepo;

    public StationCompareService(SelfWeatherRecordRepository selfRepo) {
        this.selfRepo = selfRepo;
    }

    public Map<String, Object> getStationComparison() {
        Map<String, Double> temps = fetchAllValues("Temperature");
        Map<String, Double> hums = fetchAllValues("Humidity");

        // 计算外部站平均值
        double avgTemp = temps.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgHum = hums.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);

        List<Map<String, Object>> stations = new ArrayList<>();

        // 外部站
        for (String id : STATION_NAMES.keySet()) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("stationId", id);
            s.put("stationName", STATION_NAMES.get(id));
            s.put("source", "smg");
            Double temp = temps.get(id.toUpperCase());
            Double hum = hums.get(id.toUpperCase());
            s.put("temperature", temp);
            s.put("humidity", hum);
            s.put("tempDiff", temp != null ? round(temp - avgTemp) : null);
            s.put("humDiff", hum != null ? round(hum - avgHum) : null);
            stations.add(s);
        }

        // 自研站
        SelfWeatherRecord r = selfRepo.findTopByDeviceIdOrderByRecordTimeDesc(SELF_DEVICE_ID);
        Map<String, Object> self = new LinkedHashMap<>();
        self.put("stationId", SELF_STATION_ID);
        self.put("stationName", SELF_STATION_NAME);
        self.put("source", "mqtt");
        if (r != null && r.getTemperature() != null) {
            self.put("temperature", r.getTemperature());
            self.put("tempDiff", round(r.getTemperature() - avgTemp));
        } else {
            self.put("temperature", null);
            self.put("tempDiff", null);
        }
        if (r != null && r.getHumidity() != null) {
            self.put("humidity", r.getHumidity());
            self.put("humDiff", round(r.getHumidity() - avgHum));
        } else {
            self.put("humidity", null);
            self.put("humDiff", null);
        }
        stations.add(self);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("avgTemperature", round(avgTemp));
        result.put("avgHumidity", round(avgHum));
        result.put("stations", stations);
        return result;
    }

    private Map<String, Double> fetchAllValues(String element) {
        try {
            String url = String.format("%s/pages/nowWeather/searchNowWeather/smg.php?element=%s&name=&unit=%s",
                    BASE, element, URLEncoder.encode(element.equals("Temperature") ? "(℃)" : "(%)", StandardCharsets.UTF_8));
            Document doc = Jsoup.connect(url)
                    .userAgent(UA)
                    .referrer(REFERER)
                    .timeout(15000)
                    .get();
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
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
