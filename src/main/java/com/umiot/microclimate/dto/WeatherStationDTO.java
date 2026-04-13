package com.umiot.microclimate.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherStationDTO {

    @JsonProperty("device_id")
    private String deviceId;

    private Double temp;
    private Double hum;
    private Double wind;
    private String dir;
    private Double press;
    private Integer rad;

    private String gps;
    private String time;

    @JsonProperty("SOC")
    private Double soc;//state of charge

    private Alarms alarms;

    @Data
    public static class Alarms {
        private Boolean battery;
        private Boolean network;
        private Boolean storage;
        private Boolean watchdog;
    }
}
