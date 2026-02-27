package com.umiot.microclimate.controller;

import com.umiot.microclimate.entity.WeatherRecord;
import com.umiot.microclimate.repository.WeatherRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class WeatherHistoryController {

    @Autowired
    private WeatherRecordRepository repository;

    @GetMapping("/{stationId}")
    public List<WeatherRecord> history(@PathVariable String stationId) {
        return repository.findByStationIdOrderByRecordTimeDesc(stationId);
    }
}