package com.umiot.microclimate.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "self_weather_record")
public class SelfWeatherRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceId;
    private Double temperature;
    private Double humidity;
    private Double windSpeed;
    private String windDirection;
    private Double pressure;
    private Integer radiation;
    private String gps;
    private Double soc;
    private LocalDateTime recordTime;
    private Boolean batteryAlarm;
    private Boolean networkAlarm;
    private Boolean storageAlarm;
    private Boolean watchdogAlarm;
}
