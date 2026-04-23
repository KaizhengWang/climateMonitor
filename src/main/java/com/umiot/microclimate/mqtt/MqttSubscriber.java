package com.umiot.microclimate.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umiot.microclimate.dto.WeatherStationDTO;
import com.umiot.microclimate.service.WeatherMqttService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class MqttSubscriber {

    private static final String BROKER = "tcp://mqtt.usr.cn:1883";
    private static final String USERNAME = "usr.cn";
    private static final String PASSWORD = "usr.cn";
    private static final String TOPIC = "/PubTopic1";
    private static final int QOS = 1;

    private final WeatherMqttService weatherService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String clientId = buildClientId();

    private volatile MqttClient client;

    public MqttSubscriber(WeatherMqttService weatherService) {
        this.weatherService = weatherService;
    }

    @PostConstruct
    public void init() {
        connectAndSubscribe();
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (client != null) {
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.close();
            }
        } catch (Exception e) {
            System.err.println("关闭 MQTT 客户端失败: " + e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void ensureConnected() {
        if (client == null || !client.isConnected()) {
            System.out.println("检测到 MQTT 未连接，开始重连...");
            connectAndSubscribe();
        }
    }

    private synchronized void connectAndSubscribe() {
        try {
            if (client != null && client.isConnected()) {
                return;
            }

            if (client == null) {
                client = new MqttClient(BROKER, clientId);
                client.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {
                        System.out.printf("MQTT 已连接: server=%s reconnect=%s clientId=%s%n", serverURI, reconnect, clientId);
                        subscribeTopic();
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                        System.err.println("MQTT 连接丢失: " + (cause == null ? "unknown" : cause.getMessage()));
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        handleMessage(message);
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                    }
                });
            }

            if (!client.isConnected()) {
                MqttConnectOptions options = new MqttConnectOptions();
                options.setAutomaticReconnect(true);
                options.setCleanSession(false);
                options.setConnectionTimeout(10);
                options.setKeepAliveInterval(30);
                options.setUserName(USERNAME);
                options.setPassword(PASSWORD.toCharArray());
                client.connect(options);
            }
        } catch (Exception e) {
            System.err.println("MQTT 连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private synchronized void subscribeTopic() {
        try {
            if (client != null && client.isConnected()) {
                client.subscribe(TOPIC, QOS);
                System.out.printf("MQTT 已订阅: topic=%s qos=%d%n", TOPIC, QOS);
            }
        } catch (MqttException e) {
            System.err.println("MQTT 订阅失败: " + e.getMessage());
        }
    }

    private void handleMessage(MqttMessage message) {
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
            System.out.println("MQTT收到数据: " + payload);
            weatherService.handleWeatherData(data);
        } catch (Exception e) {
            System.err.println("MQTT 消息处理失败: " + e.getMessage());
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

    private static String buildClientId() {
        String runtimeId = ManagementFactory.getRuntimeMXBean().getName().replace('@', '-');
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return "microclimate-" + runtimeId + "-" + suffix;
    }
}
