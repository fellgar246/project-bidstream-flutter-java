package com.bidstream.infrastructure.messaging;

import java.util.Map;

/** Message envelope published to RabbitMQ from the outbox relay. */
public record IntegrationMessage(
    long eventId,
    String eventType,
    String aggregateType,
    long aggregateId,
    String occurredAt,
    Map<String, Object> payload) {}
