package com.umiot.microclimate.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
public class WeatherController {

    @GetMapping("/api/weather/macau")
    public Map<String, Map<String, String>> getMacauWeather() {
        Map<String, Map<String, String>> allData = new HashMap<>();

        // -------- 抓取温度 --------
        Map<String, String> temperatureData = new HashMap<>();
        try {
            Document tempDoc = Jsoup.connect(
                            "https://std.puiching.edu.mo/~pcmsams/index.php/nowweather/nowweather/")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/117.0.0.0 Safari/537.36")
                    .timeout(5000)
                    .get();

            Elements liElements = tempDoc.select("div#dataTable ul li");
            for (Element li : liElements) {
                String station = li.selectFirst("span.stnName").text();
                String value = li.selectFirst("span.value").text();
                temperatureData.put(station, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
            temperatureData.put("error", "无法抓取温度");
        }

        // -------- 抓取湿度 --------
        Map<String, String> humidityData = new HashMap<>();
        try {
            String baseUrl = "https://std.puiching.edu.mo/~pcmsams/pages/nowWeather/searchNowWeather/smg.php";
            // 分别编码每个参数
            String url = baseUrl
                    + "?element=" + URLEncoder.encode("Humidity", StandardCharsets.UTF_8)
                    + "&name=" + URLEncoder.encode("相對濕度", StandardCharsets.UTF_8)
                    + "&unit=" + URLEncoder.encode("(%)", StandardCharsets.UTF_8);

            Document humDoc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/117.0.0.0 Safari/537.36")
                    .referrer("https://std.puiching.edu.mo/~pcmsams/index.php/nowweather/nowweather/")
                    .timeout(5000)
                    .get();

            Elements liElements = humDoc.select("div#dataTable ul li");
            for (Element li : liElements) {
                String station = li.selectFirst("span.stnName").text();
                String value = li.selectFirst("span.value").text();
                humidityData.put(station, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
            humidityData.put("error", "无法抓取湿度");
        }
        // -------- 抓取十分钟风速 --------
        Map<String, String> windSpeedData = new HashMap<>();
        try {
            String baseUrl = "https://std.puiching.edu.mo/~pcmsams/pages/nowWeather/searchNowWeather/smg.php";
            String url = baseUrl
                    + "?element=" + URLEncoder.encode("WindSpeed", StandardCharsets.UTF_8)
                    + "&name=" + URLEncoder.encode("十分鐘風速", StandardCharsets.UTF_8)
                    + "&unit=" + URLEncoder.encode("(km/h)", StandardCharsets.UTF_8);

            Document windDoc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/117.0.0.0 Safari/537.36")
                    .referrer("https://std.puiching.edu.mo/~pcmsams/index.php/nowweather/nowweather/")
                    .timeout(5000)
                    .get();

            Elements liElements = windDoc.select("div#dataTable ul li");
            for (Element li : liElements) {
                String station = li.selectFirst("span.stnName").text();
                String value = li.selectFirst("span.value").text();
                windSpeedData.put(station, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
            windSpeedData.put("error", "无法抓取风速");
        }

        // -------- 抓取风向 --------
        Map<String, String> windDirectionData = new HashMap<>();
        try {
            String baseUrl = "https://std.puiching.edu.mo/~pcmsams/pages/nowWeather/searchNowWeather/windDir.php";
            String url = baseUrl
                    + "?element=" + URLEncoder.encode("WindDir", StandardCharsets.UTF_8)
                    + "&name=" + URLEncoder.encode("風向", StandardCharsets.UTF_8)
                    + "&unit=" + URLEncoder.encode("", StandardCharsets.UTF_8); // 风向没有单位

            Document windDirDoc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/117.0.0.0 Safari/537.36")
                    .referrer("https://std.puiching.edu.mo/~pcmsams/index.php/nowweather/nowweather/")
                    .timeout(5000)
                    .get();

            Elements liElements = windDirDoc.select("div#dataTable ul li");
            for (Element li : liElements) {
                String station = li.selectFirst("span.stnName").text();
                String value = li.selectFirst("span.value").text();
                windDirectionData.put(station, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
            windDirectionData.put("error", "无法抓取风向");
        }
        // -------- 抓取一小时雨量 --------
        Map<String, String> rainHourData = new HashMap<>();
        try {
            String baseUrl = "https://std.puiching.edu.mo/~pcmsams/pages/nowWeather/searchNowWeather/smg.php";
            String url = baseUrl
                    + "?element=" + URLEncoder.encode("rainHour", StandardCharsets.UTF_8)
                    + "&name=" + URLEncoder.encode("一小時雨量", StandardCharsets.UTF_8)
                    + "&unit=" + URLEncoder.encode("(mm)", StandardCharsets.UTF_8);

            Document rainDoc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/117.0.0.0 Safari/537.36")
                    .referrer("https://std.puiching.edu.mo/~pcmsams/index.php/nowweather/nowweather/")
                    .timeout(5000)
                    .get();

            Elements liElements = rainDoc.select("div#dataTable ul li");
            for (Element li : liElements) {
                String station = li.selectFirst("span.stnName").text();
                String value = li.selectFirst("span.value").text();
                rainHourData.put(station, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
            rainHourData.put("error", "无法抓取一小时雨量");
        }


        allData.put("temperature", temperatureData);
        allData.put("humidity", humidityData);
        allData.put("windSpeed", windSpeedData);
        allData.put("windDirection", windDirectionData);
        allData.put("rainHour", rainHourData);
        return allData;
    }
}