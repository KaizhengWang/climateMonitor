package com.umiot.microclimate.scheduler;

import com.umiot.microclimate.controller.WeatherController;
import com.umiot.microclimate.service.WeatherCollectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class WeatherScheduleTask {

    @Autowired
    private WeatherController weatherController;

    @Autowired
    private WeatherCollectService collectService;

    // ⏰ 每 15 分钟执行一次
    @Scheduled(cron = "0 0/15 * * * ?")

    public void collectWeather() {

        Map<String, Map<String, String>> data =
                weatherController.getMacauWeather();

        collectService.collectOnce(
                data.get("temperature"),
                data.get("humidity"),
                data.get("windSpeed"),
                data.get("windDirection"),
                data.get("rainHour")
        );

        System.out.println("✅ 定时采集完成：" + LocalDateTime.now());
    }
}
