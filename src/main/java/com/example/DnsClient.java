package com.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

public class DnsClient {

    private final Configuration configuration;

    private final DatagramChannel channel;

    private final ExecutorService sendingExecutor;
    private final ExecutorService receivingExecutor;

    private Map<Short, CompletableFuture<DnsMessage>> correlations = new ConcurrentHashMap<>();

    public DnsClient(Configuration configuration) {
        this.configuration = configuration;
        try {
            channel = DatagramChannel.open();
            channel.connect(new InetSocketAddress(configuration.getServerHost(), configuration.getServerPort()));
            System.out.println("Ready to communicate with " + configuration.getServerHost() + ":" + configuration.getServerPort());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sendingExecutor = Executors.newSingleThreadExecutor(); // TODO thread names
        receivingExecutor = Executors.newSingleThreadExecutor(); // TODO thread name
    }

    public void start() {
        receivingExecutor.submit(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(configuration.getMaxMessageLength());
            while (!Thread.interrupted()) {
                receive(buffer);
            }
        });
    }

    public CompletableFuture<DnsMessage> send(DnsMessage query) {
        short transactionId = query.getTransactionId();
        return CompletableFuture
                .supplyAsync(() -> {
                    CompletableFuture<DnsMessage> futureResponse = new CompletableFuture<>();
                    correlations.put(transactionId, futureResponse);
                    try {
                        channel.write(ByteBuffer.wrap(query.bytes()));
                        return futureResponse;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, sendingExecutor)
                .thenCompose(futureResponse -> futureResponse.orTimeout(configuration.getServerTimeout(), TimeUnit.MILLISECONDS))
                .whenComplete((response, throwable) -> correlations.remove(transactionId));
    }

    private void receive(ByteBuffer buffer) {
        buffer.clear();
        try {
            channel.read(buffer);
            DnsMessage response = new DnsMessage(Arrays.copyOf(buffer.array(), buffer.position()));
            Optional.ofNullable(correlations.get(response.getTransactionId()))
                    .ifPresent(futureResponse -> futureResponse.complete(response));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

}
