package com.umiot.microclimate.dto;

import lombok.Data;

@Data
public class NowWeatherDTO {
    private String stationId;
    private Double temperature;
    private Double humidity;
    private Double windSpeed10min;
    private String windDirection;
    private Double rain1hour;
    private String updateTime;
}
