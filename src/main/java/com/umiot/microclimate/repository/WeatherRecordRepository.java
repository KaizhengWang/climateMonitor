package com.umiot.microclimate.repository;

import com.umiot.microclimate.entity.WeatherRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WeatherRecordRepository extends JpaRepository<WeatherRecord, Long> {

    List<WeatherRecord> findByStationIdOrderByRecordTimeDesc(String stationId);

    List<WeatherRecord> findByRecordTimeBetween(
            LocalDateTime start, LocalDateTime end
    );
}