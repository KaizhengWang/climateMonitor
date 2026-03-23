package com.umiot.microclimate.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.umiot.microclimate.dto.WeatherStationDTO;
import com.umiot.microclimate.service.WeatherMqttService;
import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MqttSubscriber {

    private static final String BROKER = "tcp://mqtt.usr.cn:1883";
    private static final String CLIENT_ID = "usr.cn2";
    private static final String TOPIC = "/PubTopic1";
//收到气象数据
    @Autowired
    private WeatherMqttService weatherService;

    private ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {

        try {

            MqttClient client = new MqttClient(BROKER, CLIENT_ID);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setUserName("usr.cn");
            options.setPassword("usr.cn".toCharArray());
            client.connect(options);

            System.out.println("MQTT 已连接");

            client.subscribe(TOPIC, (topic, message) -> {

                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

                try {

                    if (!isLikelyJsonObject(payload)) {
                        return;
                    }

                    JsonNode root = mapper.readTree(payload);
                    if (!isWeatherPayload(root)) {
                        return;
                    }

                    WeatherStationDTO data = mapper.treeToValue(root, WeatherStationDTO.class);

                    System.out.println("MQTT收到数据:");
                    System.out.println(payload);
                    weatherService.handleWeatherData(data);

                } catch (Exception e) {
                    // Ignore malformed/non-weather payloads to keep terminal clean.
                }

            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isLikelyJsonObject(String payload) {
        if (payload == null) {
            return false;
        }
        String trimmed = payload.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }

    private boolean isWeatherPayload(JsonNode root) {
        return root != null
                && root.isObject()
                && root.has("device_id")
                && root.has("temp")
                && root.has("hum")
                && root.has("wind")
                && root.has("dir")
                && root.has("time");
    }
}
