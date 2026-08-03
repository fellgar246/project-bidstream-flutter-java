package com.bidstream.application.outbox;

import java.util.List;

public interface IntegrationEventPublisherPort {

  void publish(OutboxEvent event);
}
