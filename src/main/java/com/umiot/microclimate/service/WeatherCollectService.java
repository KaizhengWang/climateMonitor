package com.umiot.microclimate.service;

import com.umiot.microclimate.dto.WeatherDTO;
import com.umiot.microclimate.entity.WeatherRecord;
import com.umiot.microclimate.repository.WeatherRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class WeatherCollectService {

    @Autowired
    private WeatherRecordRepository repository;

    public void collectOnce(
            Map<String, String> temperature,
            Map<String, String> humidity,
            Map<String, String> windSpeed,
            Map<String, String> windDirection,
            Map<String, String> rainHour
    ) {
        for (String station : temperature.keySet()) {

            WeatherRecord record = new WeatherRecord();
            record.setStationId(station);
            record.setTemperature(parseDouble(temperature.get(station)));
            record.setHumidity(parseDouble(humidity.get(station)));
            record.setWindSpeed(parseDouble(windSpeed.get(station)));
            record.setWindDirection(windDirection.get(station));
            record.setRainHour(parseDouble(rainHour.get(station)));
            record.setRecordTime(LocalDateTime.now());

            repository.save(record);
        }
    }

    private Double parseDouble(String v) {
        try {
            return v == null ? null : Double.parseDouble(v);
        } catch (Exception e) {
            return null;
        }
    }
}