package com.umiot.microclimate.controller;


import com.umiot.microclimate.dto.WeatherStationDTO;
import com.umiot.microclimate.service.WeatherMqttService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mqtt")
public class MqttController {

    @Autowired
    private WeatherMqttService weatherService;

    @GetMapping("/station")
    public WeatherStationDTO getStationWeather() {

        return weatherService.getLatestWeather();

    }
}