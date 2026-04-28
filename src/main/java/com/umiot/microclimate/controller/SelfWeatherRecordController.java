package com.umiot.microclimate.controller;

import com.umiot.microclimate.entity.SelfWeatherRecord;
import com.umiot.microclimate.repository.SelfWeatherRecordRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/self-stations")
public class SelfWeatherRecordController {

    private final SelfWeatherRecordRepository repository;

    public SelfWeatherRecordController(SelfWeatherRecordRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<String> devicesCompat() {
        return repository.findDistinctDeviceIds();
    }

    @GetMapping("/devices")
    public List<String> devices() {
        return repository.findDistinctDeviceIds();
    }

    @GetMapping("/history/{deviceId}")
    public List<SelfWeatherRecord> history(@PathVariable String deviceId) {
        return repository.findByDeviceIdOrderByRecordTimeDesc(deviceId);
    }

    @GetMapping("/latest/{deviceId}")
    public SelfWeatherRecord latest(@PathVariable String deviceId) {
        return repository.findTopByDeviceIdOrderByRecordTimeDesc(deviceId);
    }

    @GetMapping("/latest")
    public List<SelfWeatherRecord> latestAll() {
        List<String> deviceIds = repository.findDistinctDeviceIds();
        if (deviceIds.isEmpty()) {
            return Collections.emptyList();
        }

        return deviceIds.stream()
                .map(repository::findTopByDeviceIdOrderByRecordTimeDesc)
                .toList();
    }
}
