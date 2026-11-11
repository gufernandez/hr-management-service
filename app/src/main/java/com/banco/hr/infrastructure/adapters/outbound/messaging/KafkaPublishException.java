package com.banco.hr.infrastructure.adapters.outbound.messaging;

public class KafkaPublishException extends RuntimeException {

    public KafkaPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
