package com.example;

public class Runner {

    public static void main(String[] args) {
        new DnsProxy(Configuration.parse(args)).start();
    }

}
