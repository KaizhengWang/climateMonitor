package com.umiot.microclimate.service;

import com.umiot.microclimate.dto.WeatherStationDTO;
import org.springframework.stereotype.Service;

@Service
public class WeatherMqttService {

    // 保存最新气象数据
    private WeatherStationDTO latestWeather;

    public void handleWeatherData(WeatherStationDTO data) {

        System.out.println("收到气象数据:");
        System.out.println("温度: " + data.getTemp());
        System.out.println("湿度: " + data.getHum());
        System.out.println("风速: " + data.getWind());

        latestWeather = data;

        // 这里未来可以：
        // 1 保存数据库
        // 2 推 websocket
        // 3 做报警判断
    }

    public WeatherStationDTO getLatestWeather() {
        return latestWeather;
    }
}
