package com.bidstream.infrastructure.messaging;

import com.bidstream.application.outbox.IntegrationEventPublisherPort;
import com.bidstream.application.outbox.OutboxEvent;
import com.bidstream.application.tracing.TraceContext;
import org.slf4j.MDC;
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
    restoreTraceContext(event.payload());
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

  private void restoreTraceContext(java.util.Map<String, Object> payload) {
    Object traceId = payload.get("_traceId");
    if (traceId != null) {
      MDC.put(TraceContext.TRACE_ID_MDC, traceId.toString());
    }
    Object spanId = payload.get("_spanId");
    if (spanId != null) {
      MDC.put(TraceContext.SPAN_ID_MDC, spanId.toString());
    }
  }
}
