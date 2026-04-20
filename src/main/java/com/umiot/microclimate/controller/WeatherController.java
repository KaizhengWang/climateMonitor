package com.umiot.microclimate.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umiot.microclimate.service.WeatherCollectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
public class WeatherController {

    private static final String SMG_ACTUAL_WEATHER_URL =
            "https://new-api.smg.gov.mo/weather_v2?selection=actualweather&lang=c";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WeatherCollectService collectService;

    @GetMapping("/api/weather/macau")
    public Map<String, Map<String, String>> getMacauWeather() {
        Map<String, String> temperatureData = new HashMap<>();
        Map<String, String> humidityData = new HashMap<>();
        Map<String, String> windSpeedData = new HashMap<>();
        Map<String, String> windDirectionData = new HashMap<>();
        Map<String, String> rainHourData = new HashMap<>();

        try {
            JsonNode weatherReports = fetchWeatherReports();
            for (JsonNode report : weatherReports) {
                JsonNode stationNode = report.path("station").path(0);
                if (stationNode.isMissingNode()) {
                    continue;
                }

                String station = text(stationNode.path("stationname"), "");
                if (station.isBlank()) {
                    station = stationNode.path("$").path("code").asText("");
                }
                if (station.isBlank()) {
                    continue;
                }

                putIfPresent(temperatureData, station, firstMetricValue(stationNode, "Temperature"));
                putIfPresent(humidityData, station, firstMetricValue(stationNode, "Humidity"));
                putIfPresent(windSpeedData, station, firstMetricValue(stationNode, "WindSpeed"));
                putIfPresent(windDirectionData, station, firstMetricValue(stationNode, "WindDirection"));
                putIfPresent(rainHourData, station, rainfallByType(stationNode, "4"));
            }
        } catch (Exception e) {
            temperatureData.put("error", "无法抓取温度");
            humidityData.put("error", "无法抓取湿度");
            windSpeedData.put("error", "无法抓取风速");
            windDirectionData.put("error", "无法抓取风向");
            rainHourData.put("error", "无法抓取1小时雨量");
        }

        Map<String, Map<String, String>> allData = new HashMap<>();
        allData.put("temperature", temperatureData);
        allData.put("humidity", humidityData);
        allData.put("windSpeed", windSpeedData);
        allData.put("windDirection", windDirectionData);
        allData.put("rainHour", rainHourData);
        return allData;
    }

    private JsonNode fetchWeatherReports() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SMG_ACTUAL_WEATHER_URL))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Content-Type", "application/json;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString("", StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("SMG API returned status " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.path("ActualWeather").path("Custom").path(0).path("WeatherReport");
    }

    private String firstMetricValue(JsonNode stationNode, String fieldName) {
        JsonNode metricNode = stationNode.path(fieldName).path(0);
        return text(metricNode.path("dValue"), null);
    }

    private String rainfallByType(JsonNode stationNode, String targetType) {
        for (JsonNode rainfallNode : stationNode.path("Rainfall")) {
            String type = text(rainfallNode.path("Type"), "");
            if (targetType.equals(type)) {
                return text(rainfallNode.path("dValue"), null);
            }
        }
        return null;
    }

    private String text(JsonNode node, String defaultValue) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        if (node.isArray()) {
            if (node.isEmpty()) {
                return defaultValue;
            }
            JsonNode first = node.get(0);
            return first == null || first.isNull() ? defaultValue : first.asText(defaultValue);
        }
        return node.asText(defaultValue);
    }

    private void putIfPresent(Map<String, String> target, String station, String value) {
        if (value != null && !value.isBlank()) {
            target.put(station, value);
        }
    }
}
//WindDirection