package com.example;

import java.nio.ByteBuffer;

public class DnsMessage {

    private final ByteBuffer bytes;

    public DnsMessage(byte[] bytes) {
        this.bytes = ByteBuffer.wrap(bytes);
    }

    public short getTransactionId() {
        return bytes.getShort(0);
    }

    public void setTransactionId(short transactionId) {
        bytes.putShort(0, transactionId);
    }

    public byte[] bytes() {
        return bytes.array();
    }

}
