package com.umiot.microclimate.history.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class WeatherHistoryRecord {

    private Long id;
    private String stationId;
    private Double temperature;
    private Double dailyHigh;
    private Double dailyLow;
    private Double feelsLike;
    private Double humidity;
    private Double dewPoint;
    private Double rain1min;
    private Double rain1hour;
    private Double rain2hour;
    private Double rainDaily;
    private Double windSpeed10min;
    private Double windSpeed60min;
    private Double windGust;
    private String windDirection;
    private Double windDirectionDegrees;
    private LocalDateTime recordTime;

    public static String[] COLUMNS = {
        "stationId", "temperature", "dailyHigh", "dailyLow", "feelsLike",
        "humidity", "dewPoint", "rain1min", "rain1hour", "rain2hour",
        "rainDaily", "windSpeed10min", "windSpeed60min", "windGust",
        "windDirection", "windDirectionDegrees", "recordTime"
    };

    public Object[] toRow() {
        return new Object[]{
            stationId, temperature, dailyHigh, dailyLow, feelsLike,
            humidity, dewPoint, rain1min, rain1hour, rain2hour,
            rainDaily, windSpeed10min, windSpeed60min, windGust,
            windDirection, windDirectionDegrees,
            recordTime != null ? recordTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null
        };
    }
}
