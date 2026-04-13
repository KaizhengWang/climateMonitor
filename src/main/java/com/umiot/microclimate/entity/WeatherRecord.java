package com.umiot.microclimate.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@Entity
@Table(name = "weather_record")
public class WeatherRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 站点 ID（UM / TG / KV）
    private String stationId;

    private Double temperature;
    private Double humidity;
    private Double windSpeed;
    private String windDirection;
    private Double rainHour;

    // 采集时间
    private LocalDateTime recordTime;

}