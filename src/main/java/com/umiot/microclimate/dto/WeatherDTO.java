package com.umiot.microclimate.dto;


public class WeatherDTO {

    private Double temperature;
    private Double humidity;
    private Double windSpeed;
    private String windDirection;
    private Double rainHour;

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    public Double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(Double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(String windDirection) {
        this.windDirection = windDirection;
    }

    public Double getRainHour() {
        return rainHour;
    }

    public void setRainHour(Double rainHour) {
        this.rainHour = rainHour;
    }
}