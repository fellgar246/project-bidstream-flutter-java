package com.bidstream.infrastructure.messaging;

import com.bidstream.application.outbox.IntegrationEventPublisherPort;
import com.bidstream.application.outbox.OutboxEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitIntegrationEventPublisher implements IntegrationEventPublisherPort {

  private final RabbitTemplate rabbitTemplate;

  public RabbitIntegrationEventPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @Override
  public void publish(OutboxEvent event) {
    IntegrationMessage message =
        new IntegrationMessage(
            event.id(),
            event.eventType(),
            event.aggregateType(),
            event.aggregateId(),
            event.occurredAt().toString(),
            event.payload());
    String routingKey = OutboxEvent.routingKey(event.eventType());
    rabbitTemplate.convertAndSend(RabbitMqConfig.EVENTS_EXCHANGE, routingKey, message);
  }
}
