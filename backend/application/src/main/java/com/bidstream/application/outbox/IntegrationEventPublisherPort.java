package com.bidstream.application.outbox;

public interface IntegrationEventPublisherPort {

  void publish(OutboxEvent event);
}
