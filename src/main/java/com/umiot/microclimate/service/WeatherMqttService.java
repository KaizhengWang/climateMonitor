package com.umiot.microclimate.service;

import com.umiot.microclimate.dto.WeatherStationDTO;
import com.umiot.microclimate.entity.SelfWeatherRecord;
import com.umiot.microclimate.repository.SelfWeatherRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class WeatherMqttService {

    private final SelfWeatherRecordRepository selfWeatherRecordRepository;

    private WeatherStationDTO latestWeather;

    public WeatherMqttService(SelfWeatherRecordRepository selfWeatherRecordRepository) {
        this.selfWeatherRecordRepository = selfWeatherRecordRepository;
    }

    public void handleWeatherData(WeatherStationDTO data) {
        latestWeather = data;
        selfWeatherRecordRepository.save(toEntity(data));
    }

    public WeatherStationDTO getLatestWeather() {
        return latestWeather;
    }

    private SelfWeatherRecord toEntity(WeatherStationDTO data) {
        SelfWeatherRecord record = new SelfWeatherRecord();
        record.setDeviceId(data.getDeviceId());
        record.setTemperature(data.getTemp());
        record.setHumidity(data.getHum());
        record.setWindSpeed(data.getWind());
        record.setWindDirection(data.getDir());
        record.setPressure(data.getPress());
        record.setRadiation(data.getRad());
        record.setGps(data.getGps());
        record.setSoc(data.getSoc());
        record.setRecordTime(parseRecordTime(data.getTime()));

        WeatherStationDTO.Alarms alarms = data.getAlarms();
        if (alarms != null) {
            record.setBatteryAlarm(alarms.getBattery());
            record.setNetworkAlarm(alarms.getNetwork());
            record.setStorageAlarm(alarms.getStorage());
            record.setWatchdogAlarm(alarms.getWatchdog());
        }
        return record;
    }

    private LocalDateTime parseRecordTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }

        return LocalDateTime.now();
    }
}
