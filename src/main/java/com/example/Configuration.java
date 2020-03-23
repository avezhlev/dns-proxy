package com.example;

import lombok.Getter;

@Getter
public class Configuration {

    private int listenOnPort = 53;

    private String serverHost = "8.8.8.8";
    private int serverPort = 53;
    private int serverTimeout = 200;

    private int maxMessageLength = 512;

    public static Configuration parse(String[] args) {
        // TODO parse args
        return new Configuration();
    }

}
