package com.umiot.microclimate.repository;

import com.umiot.microclimate.entity.SelfWeatherRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SelfWeatherRecordRepository extends JpaRepository<SelfWeatherRecord, Long> {

    List<SelfWeatherRecord> findByDeviceIdOrderByRecordTimeDesc(String deviceId);

    SelfWeatherRecord findTopByDeviceIdOrderByRecordTimeDesc(String deviceId);

    @Query("select distinct r.deviceId from SelfWeatherRecord r where r.deviceId is not null order by r.deviceId")
    List<String> findDistinctDeviceIds();
}
