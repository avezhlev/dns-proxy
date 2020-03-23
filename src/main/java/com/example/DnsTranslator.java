package com.example;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DnsTranslator {

    private final DnsClient client;

    private final AtomicInteger currentTransactionId = new AtomicInteger();
    private final Map<Short, Short> translations = new ConcurrentHashMap<>();

    public DnsTranslator(Configuration configuration) {
        this.client = new DnsClient(configuration);
    }

    public void start() {
        client.start();
    }

    public CompletableFuture<DnsMessage> translate(DnsMessage query) {
        short translatedId = (short) currentTransactionId.getAndIncrement();
        return client.send(translateQuery(query, translatedId))
                .thenApply(this::translateResponse)
                .whenComplete((response, throwable) -> translations.remove(translatedId));
    }

    private DnsMessage translateQuery(DnsMessage query, short translatedId) {
        translations.put(translatedId, query.getTransactionId());
        query.setTransactionId(translatedId);
        return query;
    }

    private DnsMessage translateResponse(DnsMessage response) {
        response.setTransactionId(translations.get(response.getTransactionId())); // if there is no such translation, NPE is OK
        return response;
    }

}
