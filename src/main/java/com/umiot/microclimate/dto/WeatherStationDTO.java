package com.umiot.microclimate.dto;

import lombok.Data;

@Data
public class WeatherStationDTO {

    private double temp;
    private double hum;
    private double wind;
    private String dir;
    private double press;
    private int rad;

    private Power power;
    private Network network;
    private Storage storage;

    @Data
    public static class Power {
        private double soc;
        private double volt;
        private double curr;
        private double pow;

        private String bat;
        private String chg;
        private String dis;
    }

    @Data
    public static class Network {
        private boolean mqtt;
    }

    @Data
    public static class Storage {
        private boolean sd;
    }
}