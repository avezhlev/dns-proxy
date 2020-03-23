package com.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DnsProxy {

    private final Configuration configuration;

    private final DnsTranslator translator;
    private final DatagramChannel channel;

    private final ExecutorService receivingExecutor;
    private final ExecutorService sendingExecutor;

    public DnsProxy(Configuration configuration) {
        this.configuration = configuration;
        this.translator = new DnsTranslator(configuration);
        try {
            channel = DatagramChannel.open();
            channel.bind(new InetSocketAddress(configuration.getListenOnPort()));
            System.out.println("Listening on port " + configuration.getListenOnPort());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        receivingExecutor = Executors.newSingleThreadExecutor(); // TODO thread names
        sendingExecutor = Executors.newSingleThreadExecutor(); // TODO thread names
    }

    public void start() {
        translator.start();
        receivingExecutor.submit(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(configuration.getMaxMessageLength());
            while (!Thread.interrupted()) {
                receiveAndTranslate(buffer);
            }
        });
    }

    private void receiveAndTranslate(ByteBuffer buffer) {
        buffer.clear();
        try {
            SocketAddress client = channel.receive(buffer);
            DnsMessage query = new DnsMessage(Arrays.copyOf(buffer.array(), buffer.position()));
            translator.translate(query)
                    .thenAcceptAsync(response -> send(response, client), sendingExecutor);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void send(DnsMessage response, SocketAddress client) {
        try {
            channel.send(ByteBuffer.wrap(response.bytes()), client);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

}
